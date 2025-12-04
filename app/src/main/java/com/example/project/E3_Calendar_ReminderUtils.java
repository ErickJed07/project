package com.example.project;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast; // Added for user feedback

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class E3_Calendar_ReminderUtils {

    public static final String NONE = "None";

    // Reference set from E1_CalendarActivity
    private static List<E2_Calendar_Event> eventsList;

    public static void setEventsList(List<E2_Calendar_Event> events) {
        eventsList = events;
    }

    /**
     * Overload to schedule reminder from E2_Calendar_Event
     */
    public static void scheduleReminder(Context context, E2_Calendar_Event event) {
        if (event == null) return;

        try {
            Calendar cal = Calendar.getInstance();
            // Combine date and time strings
            String dateTime = event.getDate() + " " + event.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            cal.setTime(Objects.requireNonNull(sdf.parse(dateTime)));

            scheduleReminder(context, event.getId(), event.getTitle(), event.getDate(),
                    cal, event.getReminder());
        } catch (ParseException e) {
            Log.e("E3_Calendar_ReminderUtils", "Failed to parse date/time for reminder", e);
        }
    }

    /**
     * Expanded reminder scheduling with Android 12+ Safety Check
     */
    public static void scheduleReminder(Context context, String eventId, String title, String date,
                                        Calendar cal, String reminder) {
        if (reminder == null || reminder.equals(NONE)) return;

        long triggerAt = cal.getTimeInMillis();

        // Calculate trigger time
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

        // Don't schedule if the time has already passed
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

        Log.d("E3_Calendar_ReminderUtils", "➡ Scheduling reminder for: " + title + " at " + timeStr);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                eventId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            // --- SAFETY CHECK FOR ANDROID 12+ (API 31+) ---
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w("E3_Reminder", "Cannot schedule exact alarms. Permission missing.");
                    // Optionally show a Toast or handle fallback (e.g., setInexactRepeating)
                    return;
                }
            }

            try {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
                Log.d("E3_Calendar_ReminderUtils", "⏰ Reminder set successfully for " + triggerAt);
            } catch (SecurityException e) {
                // This catches crashes if permission is revoked at runtime
                Log.e("E3_Reminder", "SecurityException: Permission not granted", e);
                Toast.makeText(context, "Permission needed for exact reminders", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Overload to cancel reminder from E2_Calendar_Event
     */
    public static void cancelReminder(Context context, E2_Calendar_Event event) {
        if (event != null) {
            cancelReminder(context, event.getId());
        }
    }

    /**
     * Expanded cancel reminder
     */
    public static void cancelReminder(Context context, String eventId) {
        Intent intent = new Intent(context, E5_Calendar_ReminderBroadcastReceiver.class);
        // requestCode must match the one used in scheduleReminder (eventId.hashCode())
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
     * Utility to check if a day has an event (Used for drawing dots on Calendar)
     */
    public static boolean hasEventOnDay(int day, int month, int year) {
        if (eventsList == null) return false;

        for (E2_Calendar_Event event : eventsList) {
            try {
                // Event date format is "yyyy-MM-dd"
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                // Ensure event.getDate() is not null before parsing
                if (event.getDate() != null) {
                    cal.setTime(sdf.parse(event.getDate()));

                    int eventDay = cal.get(Calendar.DAY_OF_MONTH);
                    int eventMonth = cal.get(Calendar.MONTH) + 1; // Java Calendar months are 0-11
                    int eventYear = cal.get(Calendar.YEAR);

                    if (eventDay == day && eventMonth == month && eventYear == year) {
                        return true;
                    }
                }
            } catch (Exception e) {
                Log.e("E3_Calendar_ReminderUtils", "❌ Failed to parse event date: " + event.getDate(), e);
            }
        }
        return false;
    }
}
