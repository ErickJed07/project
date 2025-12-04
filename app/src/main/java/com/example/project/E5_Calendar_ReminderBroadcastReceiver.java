package com.example.project;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class E5_Calendar_ReminderBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // 1. Extract Data from Intent
        String eventId = intent.getStringExtra("eventId");
        String title = intent.getStringExtra("title");
        String time = intent.getStringExtra("time");
        String date = intent.getStringExtra("date");

        if (title == null) title = "Event Reminder";

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "calendar_alarm_channel"; // Distinct ID for high priority

        // 2. Create an Aggressive Notification Channel (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "High Priority Event Alarms",
                    NotificationManager.IMPORTANCE_HIGH // Max importance
            );

            channel.setDescription("Loud and vibrating notifications for events");

            // Enable Vibration
            channel.enableVibration(true);
            // "SOS" style pattern: 3 short, 3 long, 3 short (repeated later via builder)
            long[] pattern = {0, 500, 200, 500, 200, 500, 1000, 1000, 1000, 1000, 500, 200, 500, 200, 500};
            channel.setVibrationPattern(pattern);

            // Enable Sound (Alarm Sound)
            // Enable Sound (Alarm Sound)
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            // SUPER VIBRATE PATTERN: 2 seconds vibrate, 0.2s pause, repeat
            long[] superVibratePattern = {0, 2000, 200, 2000, 200, 2000, 200, 2000};
            channel.setVibrationPattern(superVibratePattern);


            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();
            channel.setSound(alarmSound, audioAttributes);

            // Allow bypassing Do Not Disturb
            channel.setBypassDnd(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            notificationManager.createNotificationChannel(channel);
        }

        // 3. Setup Intent to open App when clicked
        Intent appIntent = new Intent(context, E1_CalendarActivity.class);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                eventId != null ? eventId.hashCode() : 0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 4. Build the Notification
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_popup_reminder) // Safe system icon
                .setContentTitle("ALARM: " + title)
                .setContentText("Event happening at " + time)
                .setPriority(NotificationCompat.PRIORITY_MAX) // Maximum Priority for pre-Oreo
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setSound(alarmSound)
                // Heavy Vibration Pattern matching the channel
                .setVibrate(new long[]{0, 2000, 200, 2000, 200, 2000, 200, 2000})
                .setFullScreenIntent(contentIntent, true); // Tries to wake up the screen

        // 5. Show It
        int notificationId = eventId != null ? eventId.hashCode() : (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }
}
