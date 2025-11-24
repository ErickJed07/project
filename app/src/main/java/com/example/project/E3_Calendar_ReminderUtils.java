package com.example.project;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class E3_Calendar_ReminderUtils {

    public static final String NONE = "None";

    // 🔹 Add this reference (you should set it from your DB or E1_CalendarActivity)
    private static List<E2_Calendar_Event> eventsList;

    public static void setEventsList(List<E2_Calendar_Event> events) {
        eventsList = events;
    }

    /**
     * ✅ Overload to schedule reminder from E2_Calendar_Event
     */
    public static void scheduleReminder(Context context, E2_Calendar_Event event) {
        if (event == null) return;

        try {
            Calendar cal = Calendar.getInstance();
            String dateTime = event.getDate() + " " + event.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            cal.setTime(sdf.parse(dateTime));

            scheduleReminder(context, event.getId(), event.getTitle(), event.getDate(),
                    cal, event.getReminder());
        } catch (ParseException e) {
            Log.e("E3_Calendar_ReminderUtils", "Failed to parse date/time for reminder", e);
        }
    }

    /**
     * ✅ Expanded reminder scheduling
     */
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
            Log.d("E3_Calendar_ReminderUtils", "⏰ Skipping past reminder for " + title);
            return;
        }

        Intent intent = new Intent(context, E5_Calendar_ReminderBroadcastReceiver.class);

        // Pass event details so the BroadcastReceiver can show time/date
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeStr = timeFormat.format(cal.getTime());

        intent.putExtra("eventId", eventId);
        intent.putExtra("title", title != null ? title : "Event");
        intent.putExtra("time", timeStr);
        intent.putExtra("date", date);

        Log.d("E3_Calendar_ReminderUtils", "➡ Scheduling reminder extras: time=" + timeStr + " date=" + date + " id=" + eventId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                eventId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            Log.d("E3_Calendar_ReminderUtils", "⏰ Reminder set for " + title + " at " + triggerAt);
        }
    }

    /**
     * ✅ Overload to cancel reminder from E2_Calendar_Event
     */
    public static void cancelReminder(Context context, E2_Calendar_Event event) {
        if (event != null) {
            cancelReminder(context, event.getId());
        }
    }

    /**
     * ✅ Expanded cancel reminder
     */
    public static void cancelReminder(Context context, String eventId) {
        Intent intent = new Intent(context, E5_Calendar_ReminderBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                eventId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d("E3_Calendar_ReminderUtils", "❌ Reminder cancelled for event " + eventId);
        }
    }

    /**
     * ✅ Utility to check if a day has an event
     */
    public static boolean hasEventOnDay(int day, int month, int year) {
        if (eventsList == null) return false;

        for (E2_Calendar_Event event : eventsList) {
            try {
                // Example date format in event: "yyyy-MM-dd"
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                cal.setTime(sdf.parse(event.getDate()));

                int eventDay = cal.get(Calendar.DAY_OF_MONTH);
                int eventMonth = cal.get(Calendar.MONTH) + 1; // months are 0-based
                int eventYear = cal.get(Calendar.YEAR);

                if (eventDay == day && eventMonth == month && eventYear == year) {
                    return true;
                }
            } catch (Exception e) {
                Log.e("E3_Calendar_ReminderUtils", "❌ Failed to parse event date: " + event.getDate(), e);
            }
        }
        return false;
    }

}
