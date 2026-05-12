package com.example.project;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioFocusRequest;
import android.media.AudioTrack;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.app.Notification;

public class ReminderCountdownService extends Service {
    private static final String TAG = "CountdownService";
    private static final String CHANNEL_ID = "countdown_channel";
    private static final int NOTIFICATION_ID = 1001;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int secondsRemaining = 10;
    private PowerManager.WakeLock wakeLock;
    private AudioTrack silentAudioTrack;
    private boolean isPlayingSilence = false;
    private MediaSession mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");

        // Create a MediaSession to signal to the OS that this is an active media app
        mediaSession = new MediaSession(this, "PhindeeCountdown");
        mediaSession.setActive(true);

        // Add metadata to make it look like a real player to the OS
        MediaMetadata metadata = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, "Reminder Active")
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Phindee")
                .build();
        mediaSession.setMetadata(metadata);

        PlaybackState state = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_STOP)
                .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build();
        mediaSession.setPlaybackState(state);

        // CREATE CHANNEL AND START FOREGROUND IMMEDIATELY IN ONCREATE
        // This is the most reliable way to beat the "Deep Clean" race condition.
        createNotificationChannel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, updateCountdownNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, updateCountdownNotification());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.d(TAG, "onStartCommand: null intent (system restart)");
            return START_STICKY;
        }
        Log.d(TAG, "onStartCommand: Manufacturer: " + Build.MANUFACTURER);

        // Start silent audio playback to masquerade as a media player
        startSilentAudio();

        // Request audio focus to appear as a legitimate media app
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(audioAttributes)
                        .build();
                am.requestAudioFocus(focusRequest);
            } else {
                am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            }
        }

        // Acquire a partial wake lock to keep CPU running during countdown
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Phindee:CountdownWakeLock");
            wakeLock.acquire(15000); // 15 seconds max
            Log.d(TAG, "WakeLock acquired");
        }

        // Ensure any previous countdown is stopped
        handler.removeCallbacksAndMessages(null);

        secondsRemaining = 10;
        // startForeground was already called in onCreate, but we can call it again to update if needed
        // though it's already updated in startCountdown

        startCountdown();

        return START_STICKY;
    }

    private void startCountdown() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Tick: " + secondsRemaining + "s remaining");
                secondsRemaining--;
                if (secondsRemaining > 0) {
                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (manager != null) {
                        manager.notify(NOTIFICATION_ID, updateCountdownNotification());
                    }
                    handler.postDelayed(this, 1000);
                } else {
                    Log.d(TAG, "Countdown finished");
                    // The actual notification will be sent by NotificationReceiver
                    // via the AlarmManager set in the Activity for better reliability.
                    stopSilentAudio();
                    if (wakeLock != null && wakeLock.isHeld()) {
                        wakeLock.release();
                    }
                    stopForeground(true);
                    stopSelf();
                }
            }
        }, 1000);
    }

    private void startSilentAudio() {
        if (isPlayingSilence) return;

        try {
            int bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();

                AudioFormat audioFormat = new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(44100)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build();

                silentAudioTrack = new AudioTrack.Builder()
                        .setAudioAttributes(audioAttributes)
                        .setAudioFormat(audioFormat)
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build();
            } else {
                // Fallback for API < 26 (though minSdk is 28, keeping it for robustness)
                silentAudioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        44100,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM);
            }

            isPlayingSilence = true;
            silentAudioTrack.play();

            final short[] silence = new short[bufferSize];
            new Thread(() -> {
                while (isPlayingSilence && silentAudioTrack != null) {
                    try {
                        silentAudioTrack.write(silence, 0, silence.length);
                    } catch (Exception e) {
                        Log.e(TAG, "Error writing to AudioTrack: " + e.getMessage());
                        break;
                    }
                }
            }).start();
            Log.d(TAG, "Silent audio playback started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start silent audio: " + e.getMessage());
        }
    }

    private void stopSilentAudio() {
        isPlayingSilence = false;
        if (silentAudioTrack != null) {
            try {
                if (silentAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    silentAudioTrack.stop();
                }
                silentAudioTrack.release();
                silentAudioTrack = null;
                Log.d(TAG, "Silent audio playback stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping AudioTrack: " + e.getMessage());
            }
        }
    }

    private Notification updateCountdownNotification() {
        Intent notificationIntent = new Intent(this, A_HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Using framework Notification.Builder to access MediaStyle directly
        // and ensure the strongest signal to OEM battery managers.
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle("Test in Progress")
                .setContentText("Notification will arrive in " + secondsRemaining + " seconds...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setStyle(new Notification.MediaStyle()
                    .setMediaSession(mediaSession.getSessionToken())
                    .setShowActionsInCompactView(0));
            builder.setCategory(Notification.CATEGORY_SERVICE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Countdown Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "App swiped away, but service continues...");
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopSilentAudio();
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "WakeLock released in onDestroy");
        }
        super.onDestroy();
    }
}
