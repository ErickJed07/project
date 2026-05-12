package com.example.project;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class E_Calendar_ReminderUtils {

    public static void checkAndRequestBatteryOptimizations(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        boolean isIgnoring = false;
        if (pm != null) {
            try {
                isIgnoring = pm.isIgnoringBatteryOptimizations(context.getPackageName());
            } catch (SecurityException e) {
                android.util.Log.e("ReminderUtils", "SecurityException checking battery optimizations", e);
                isIgnoring = false;
            }
        }

        if (!isIgnoring) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                try {
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    context.startActivity(intent);
                } catch (Exception ex) {
                    android.util.Log.e("ReminderUtils", "Could not open battery settings");
                }
            }
        }
    }

    public static void scheduleReminder(Context context, E_Calendar_Event event) {
        if (event.getReminder() == null || event.getReminder().equals("None")) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("title", "Outfit Reminder: " + event.getTitle());
        intent.putExtra("message", "It's time for your scheduled outfit!");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                event.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = calculateTriggerTime(event.getDate(), event.getTime(), event.getReminder());
        if (triggerTime <= System.currentTimeMillis()) return;

        if (alarmManager != null) {
            boolean canSchedule = false;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    canSchedule = alarmManager.canScheduleExactAlarms();
                } else {
                    canSchedule = true;
                }
            } catch (SecurityException e) {
                android.util.Log.e("ReminderUtils", "SecurityException checking exact alarm permission", e);
                canSchedule = false;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (canSchedule) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        }
    }

    public static void cancelReminder(Context context, E_Calendar_Event event) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                event.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private static long calculateTriggerTime(String date, String time, String reminderType) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(date + " " + time));

            if (reminderType.contains("10 minutes before")) {
                calendar.add(Calendar.MINUTE, -10);
            } else if (reminderType.contains("30 minutes before")) {
                calendar.add(Calendar.MINUTE, -30);
            } else if (reminderType.contains("1 hour before")) {
                calendar.add(Calendar.HOUR_OF_DAY, -1);
            } else if (reminderType.contains("1 day before")) {
                calendar.add(Calendar.DAY_OF_YEAR, -1);
            }
            return calendar.getTimeInMillis();
        } catch (Exception e) {
            return 0;
        }
    }
}
