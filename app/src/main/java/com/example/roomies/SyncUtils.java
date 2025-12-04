package com.example.roomies;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class SyncUtils {

    private static final String TAG = "SyncUtils";

    // Must match UserManager prefs name
    private static final String PREFS_NAME = "roomies_prefs";
    private static final String KEY_ROOM_FILE_URI = "room_file_uri";

    // Must match the database name in RoomiesDatabase ("roomies_db")
    private static final String DB_NAME = "roomies_db";

    private SyncUtils() {}

    // --- shared file URI helpers ---

    public static void saveRoomFileUri(Context context, Uri uri) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ROOM_FILE_URI, uri.toString()).apply();
    }

    public static String getRoomFileUri(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ROOM_FILE_URI, null);
    }

    public static boolean hasLinkedRoom(Context context) {
        String s = getRoomFileUri(context);
        return s != null && !s.trim().isEmpty();
    }

    /* ---------------------------------------------------
     * Public entry points you’ll call from Activities
     * --------------------------------------------------- */

    /** Call this after any “important change” (add / delete / swap / reminder). */
    public static void pushIfRoomLinked(Context context) {
        String uriStr = getRoomFileUri(context);
        if (uriStr == null || uriStr.trim().isEmpty()) {
            return; // no backend linked, nothing to do
        }
        Uri uri = Uri.parse(uriStr);
        copyDbToUriAsync(context.getApplicationContext(), uri, false);
    }

    /** Call once on app startup (MainActivity.onCreate). */
    public static void pullIfRoomLinkedOnStartup(Activity activity) {
        String uriStr = getRoomFileUri(activity);
        if (uriStr == null || uriStr.trim().isEmpty()) {
            return;
        }
        Uri uri = Uri.parse(uriStr);
        // We do this synchronously but on a background thread to avoid blocking UI.
        copyUriToDbAsync(activity, uri, true);
    }

    /** When user CREATES a new shared file (ACTION_CREATE_DOCUMENT). */
    public static void linkAndPushNewRoomFile(Activity activity, Uri uri) {
        if (uri == null) return;

        // Persist permission so future launches can read/write without re-picking
        takePersistablePermission(activity, uri);

        saveRoomFileUri(activity, uri);
        copyDbToUriAsync(activity.getApplicationContext(), uri, true);
    }

    /** When user JOINS an existing room (ACTION_OPEN_DOCUMENT). */
    public static void linkAndPullExistingRoomFile(Activity activity, Uri uri) {
        if (uri == null) return;

        takePersistablePermission(activity, uri);
        saveRoomFileUri(activity, uri);
        copyUriToDbAsync(activity, uri, true);
    }

    // --- helpers ---
    private static void takePersistablePermission(Activity activity, Uri uri) {
        try {
            final int takeFlags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

            activity
                    .getContentResolver()
                    .takePersistableUriPermission(uri, takeFlags);

        } catch (Exception e) {
            Log.w(TAG, "Persistable permission not supported here", e);
        }
    }

    private static void copyDbToUriAsync(Context appContext, Uri uri, boolean showToast) {
        new Thread(() -> {
            boolean ok = copyDbToUri(appContext, uri);
            if (showToast && appContext instanceof Activity) {
                Activity a = (Activity) appContext;
                a.runOnUiThread(() -> {
                    Toast.makeText(a,
                            ok ? "Shared room file updated." : "Failed to update shared room file.",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private static void copyUriToDbAsync(Activity activity, Uri uri, boolean showToast) {
        Context appContext = activity.getApplicationContext();
        new Thread(() -> {
            boolean ok = copyUriToDb(appContext, uri);
            if (showToast) {
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity,
                            ok ? "Pulled latest data from shared room." : "Failed to pull shared room data.",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private static boolean copyDbToUri(Context appContext, Uri uri) {
        try {
            File dbFile = appContext.getDatabasePath(DB_NAME);
            if (!dbFile.exists()) {
                Log.w(TAG, "DB file does not exist yet: " + dbFile.getAbsolutePath());
                return false;
            }

            ContentResolver cr = appContext.getContentResolver();
            try (InputStream in = new FileInputStream(dbFile);
                 OutputStream out = cr.openOutputStream(uri, "wt")) {

                if (out == null) return false;
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
                out.flush();
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error copying DB -> Uri", e);
            return false;
        }
    }

    private static boolean copyUriToDb(Context appContext, Uri uri) {
        try {
            File dbFile = appContext.getDatabasePath(DB_NAME);
            File parent = dbFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            ContentResolver cr = appContext.getContentResolver();
            try (InputStream in = cr.openInputStream(uri);
                 OutputStream out = new FileOutputStream(dbFile, false)) {

                if (in == null) return false;
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
                out.flush();
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error copying Uri -> DB", e);
            return false;
        }
    }
}