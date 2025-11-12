package com.example.roomies;

import android.content.Context;
import android.content.SharedPreferences;

public class UserManager {

    private static final String PREFS_NAME = "roomies_prefs";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_AUTO_REMINDERS = "auto_reminders";

    // --- user id ---
    public static void setCurrentUser(Context context, int userId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_CURRENT_USER_ID, userId).apply();
    }

    public static int getCurrentUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_CURRENT_USER_ID, -1);
    }

    // --- auto reminders ---
    public static void setAutoRemindersEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_AUTO_REMINDERS, enabled).apply();
    }

    public static boolean getAutoRemindersEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_AUTO_REMINDERS, false);
    }

    private static final String KEY_REMINDER_DAYS = "reminder_days";
    private static final String KEY_REMINDER_TIMES = "reminder_times";

    public static void setReminderDays(Context ctx, String daysCsv) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        p.edit().putString(KEY_REMINDER_DAYS, daysCsv).apply();
    }
    public static String getReminderDays(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return p.getString(KEY_REMINDER_DAYS, "");
    }

    public static void setReminderTimes(Context ctx, String timesCsv) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        p.edit().putString(KEY_REMINDER_TIMES, timesCsv).apply();
    }
    public static String getReminderTimes(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return p.getString(KEY_REMINDER_TIMES, "");
    }

}