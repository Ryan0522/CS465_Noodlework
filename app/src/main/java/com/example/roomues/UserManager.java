package com.example.roomues;

import android.content.Context;
import android.content.SharedPreferences;

public class UserManager {
    private static final String PREFS = "roomies_prefs";
    private static final String KEY_USER_ID = "current_user_id";
    private static final String KEY_FIRST_LAUNCH = "is_first_launch";

    public static void setCurrentUser(Context ctx, int userId) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_USER_ID, userId).apply();
    }

    public static int getCurrentUser(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_USER_ID, -1);
    }

    private static final String KEY_SHARED_URL = "shared_url";

    public static void setSharedUrl(Context context, String url) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SHARED_URL, url)
                .apply();
    }

    public static String getSharedUrl(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_SHARED_URL, null);
    }

    public static boolean isFirstLaunch(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public static void setNotFirstLaunch(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
    }
}