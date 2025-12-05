package com.example.roomies;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Helper for scheduling and cancelling alarms for reminders.
 */
public class ReminderScheduler {

    public static void scheduleReminder(Context context, ReminderEntity rem) {
        if (rem.triggerAtMillis <= 0) {
            return;
        }

        Intent intent = new Intent(context, ReminderAlarmReceiver.class);
        intent.putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, rem.id);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                rem.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        long triggerAt = rem.triggerAtMillis;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Android 12+ → avoid exact APIs to skip the permission requirement
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // Android 6–11 → exact + idle is fine
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            // Android 4.4–5.x → exact
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            // Very old devices
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    public static void cancelReminder(Context context, int reminderId) {
        Intent intent = new Intent(context, ReminderAlarmReceiver.class);
        intent.putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(pi);
        }
    }
}