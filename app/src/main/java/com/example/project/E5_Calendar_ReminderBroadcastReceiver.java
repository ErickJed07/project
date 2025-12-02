package com.example.project;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class E5_Calendar_ReminderBroadcastReceiver extends BroadcastReceiver {

    @SuppressLint("MissingPermission") // Permission checked in Activities
    @Override
    public void onReceive(Context context, Intent intent) {
        String eventId = intent.getStringExtra("eventId");
        String title = intent.getStringExtra("title");
        String time = intent.getStringExtra("time");
        String date = intent.getStringExtra("date");

        createNotificationChannel(context);

        // Intent to open App when notification is clicked
        Intent appIntent = new Intent(context, E1_CalendarActivity.class);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "CALENDAR_CHANNEL_ID")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Make sure this icon exists
                .setContentTitle("Outfit Reminder: " + title)
                .setContentText("Scheduled for " + time + " on " + date)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // Use eventId hashcode as notification ID to allow multiple notifications
        notificationManager.notify(eventId != null ? eventId.hashCode() : 0, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Outfit Reminders";
            String description = "Channel for Calendar Outfit Reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("CALENDAR_CHANNEL_ID", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
