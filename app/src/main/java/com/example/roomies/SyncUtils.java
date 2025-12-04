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
import java.util.List;

public class SyncUtils {

    private static final String TAG = "SyncUtils";

    private SyncUtils() {}

    // --- shared file URI helpers ---

    public static void saveRoomFileUri(Context context, Uri uri) {
        UserManager.setRoomURI(context, uri == null ? "" : uri.toString());
    }

    public static String getRoomFileUri(Context context) {
        return UserManager.getRoomURI(context);
    }

    public static boolean hasLinkedRoom(Context context) {
        return UserManager.hasRoom(context);
    }

    /* ---------------------------------------------------
     * Public entry points you’ll call from Activities
     * --------------------------------------------------- */

    /** Call this after any “important change” (add / delete / swap / reminder). */
    public static void pushIfRoomLinked(Context context) {
        if (!hasLinkedRoom(context)) {
            return;
        }
        CloudSyncManager.getInstance(context).pushNow();
    }

    public static void pullLatestFromRoomFile(Activity activity) {
        if (!hasLinkedRoom(activity)) {
            return;
        }
        CloudSyncManager.getInstance(activity).pullNow();
    }

    /** Call once on app startup (MainActivity.onCreate). */
    public static void pullIfRoomLinkedOnStartup(Activity activity) {
        if (!hasLinkedRoom(activity)) {
            return;
        }
        CloudSyncManager.getInstance(activity).pullNow();
    }

    /** When user CREATES a new shared file (ACTION_CREATE_DOCUMENT). */
    public static void linkAndPushNewRoomFile(Activity activity, Uri uri) {
        if (uri == null) return;

        takePersistablePermission(activity, uri);
        saveRoomFileUri(activity, uri);

        CloudSyncManager.getInstance(activity).pushNow();
    }

    /** When user JOINS an existing room (ACTION_OPEN_DOCUMENT). */
    public static void linkAndPullExistingRoomFile(Activity activity, Uri uri) {
        if (uri == null) return;

        takePersistablePermission(activity, uri);
        saveRoomFileUri(activity, uri);

        CloudSyncManager.getInstance(activity).pullNow();
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
}