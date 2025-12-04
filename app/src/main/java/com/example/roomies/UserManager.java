package com.example.roomies;

import android.content.Context;
import android.content.SharedPreferences;

public class UserManager {

    private static final String PREFS_NAME = "roomies_prefs";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_AUTO_REMINDERS = "auto_reminders";
    private static final String KEY_ROOM_URI = "room_uri";
    private static final String KEY_ROOM_FILE_URI = "room_file_uri";

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

    // --- shared room file URI (for cross-device sync) ---
    public static void setRoomURI(Context context, String uriString) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ROOM_URI, uriString == null ? "" : uriString).apply();
    }

    public static String getRoomURI(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ROOM_URI, "");
    }

    public static boolean hasRoom(Context context) {
        return !getRoomURI(context).isEmpty();
    }

    public static void setRoomFileUri(Context context, String uriString) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ROOM_FILE_URI, uriString).apply();
    }

    public static String getRoomFileUri(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ROOM_FILE_URI, null);
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