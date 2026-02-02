package com.example.project;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class E_Calendar_ReminderUtils {

    public static final String NONE = "None";

    private static List<E_Calendar_Event> eventsList;

    public static void setEventsList(List<E_Calendar_Event> events) {
        eventsList = events;
    }

    public static void scheduleReminder(Context context, E_Calendar_Event event) {
        if (event == null) return;

        try {
            Calendar cal = Calendar.getInstance();
            String dateTime = event.getDate() + " " + event.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            cal.setTime(Objects.requireNonNull(sdf.parse(dateTime)));

            scheduleReminder(context, event.getId(), event.getTitle(), event.getDate(),
                    cal, event.getReminder());
        } catch (ParseException e) {
            Log.e("E_ReminderUtils", "Failed to parse date/time for reminder", e);
        }
    }

    public static void scheduleReminder(Context context, String eventId, String title, String date,
                                        Calendar cal, String reminder) {
        if (reminder == null || reminder.equals(NONE)) return;

        long triggerAt = cal.getTimeInMillis();

        switch (reminder) {
            case "1 hour before":
                triggerAt -= 60 * 60 * 1000;
                break;
            case "45 min before":
                triggerAt -= 45 * 60 * 1000;
                break;
            case "30 min before":
                triggerAt -= 30 * 60 * 1000;
                break;
            case "15 min before":
                triggerAt -= 15 * 60 * 1000;
                break;
        }

        if (triggerAt < System.currentTimeMillis()) {
            return;
        }

        Intent intent = new Intent(context, E_Calendar_ReminderBroadcastReceiver.class);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeStr = timeFormat.format(cal.getTime());

        intent.putExtra("eventId", eventId);
        intent.putExtra("title", title != null ? title : "Event");
        intent.putExtra("time", timeStr);
        intent.putExtra("date", date);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                eventId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    return;
                }
            }

            try {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } catch (SecurityException e) {
                Log.e("E_ReminderUtils", "SecurityException: Permission not granted", e);
            }
        }
    }

    public static void cancelReminder(Context context, E_Calendar_Event event) {
        if (event != null) {
            cancelReminder(context, event.getId());
        }
    }

    public static void cancelReminder(Context context, String eventId) {
        Intent intent = new Intent(context, E_Calendar_ReminderBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                eventId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    public static boolean hasEventOnDay(int day, int month, int year) {
        if (eventsList == null) return false;

        for (E_Calendar_Event event : eventsList) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                if (event.getDate() != null) {
                    cal.setTime(sdf.parse(event.getDate()));

                    int eventDay = cal.get(Calendar.DAY_OF_MONTH);
                    int eventMonth = cal.get(Calendar.MONTH) + 1;
                    int eventYear = cal.get(Calendar.YEAR);

                    if (eventDay == day && eventMonth == month && eventYear == year) {
                        return true;
                    }
                }
            } catch (Exception e) {
                Log.e("E_ReminderUtils", "❌ Failed to parse event date", e);
            }
        }
        return false;
    }
}
