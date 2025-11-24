package com.example.project;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class E5_Calendar_ReminderBroadcastReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "calendar_reminders_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Extract event info
        String eventId = intent.getStringExtra("eventId");
        String title = intent.getStringExtra("title");
        String time = intent.getStringExtra("time");
        String date = intent.getStringExtra("date");

        // Intent for full-screen display (acts like alarm)
        Intent fullScreenIntent = new Intent(context, E1_CalendarActivity.class);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                eventId != null ? eventId.hashCode() : 0,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Notification Manager
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // For Android 8+ we need a channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Calendar Reminders";
            String description = "Notifications for your scheduled events";
            int importance = NotificationManager.IMPORTANCE_HIGH; // must be HIGH for heads-up/fullscreen
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }

        Log.d("E5_Calendar_ReminderBroadcastReceiver", "🔔 Received reminder: id=" + eventId + " title=" + title + " time=" + time + " date=" + date);

        String contentText;
        if (time != null && !time.isEmpty()) {
            contentText = (title != null ? title : "Event") + " — Scheduled at " + time;
        } else if (date != null && !date.isEmpty()) {
            contentText = (title != null ? title : "Event") + " — Scheduled on " + date;
        } else {
            contentText = (title != null ? title : "Event") + " — Reminder";
        }

        // Build full-screen notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("Event Reminder")
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM) // Alarm-style
                .setFullScreenIntent(fullScreenPendingIntent, true) // Force heads-up/fullscreen
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 1000, 1000, 1000}); // fallback vibration pattern

        // Vibrate for 30 seconds
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect effect = VibrationEffect.createOneShot(30000, VibrationEffect.DEFAULT_AMPLITUDE);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(30000); // legacy
            }
        }

        // Show the notification
        if (nm != null) {
            nm.notify(eventId != null ? eventId.hashCode() : 0, builder.build());
        }
    }
}
