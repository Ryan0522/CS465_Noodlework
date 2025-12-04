package com.example.roomies;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CloudSyncManager {
    private static final String TAG = "CloudSync";

    private static CloudSyncManager INSTANCE;

    private final Context appContext;
    private final RoomiesDatabase db;

    private CloudSyncManager(Context ctx) {
        this.appContext = ctx.getApplicationContext();
        this.db = RoomiesDatabase.getDatabase(appContext);
    }

    public static synchronized CloudSyncManager getInstance(Context ctx) {
        if (INSTANCE == null) {
            INSTANCE = new CloudSyncManager(ctx);
        }
        return INSTANCE;
    }

    // ---helpers ---

    private Uri getRoomUri() {
        String uriString = UserManager.getRoomURI(appContext);
        if (uriString == null || uriString.isEmpty()) return null;
        return Uri.parse(uriString);
    }

    public boolean hasRoom() {
        return getRoomUri() != null;
    }

    // --- public API ---

    public void pushNow() {
        Uri uri = getRoomUri();
        if (uri == null) {
            Log.d(TAG, "pushNow: no room URI set, skipping.");
            return;
        }

        try {
            JSONObject json = exportDatabse();

            OutputStream os = appContext.getContentResolver()
                    .openOutputStream(uri, "wt"); // "wt" = write & truncate
            if (os == null) {
                Log.w(TAG, "pushNow: openOutputStream returned null");
                return;
            }

            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            os.write(bytes);
            os.flush();
            os.close();

            Log.d(TAG, "pushNow: wrote " + bytes.length + " ytes to room file.");
        } catch (Exception e) {
            Log.e(TAG, "pushNow failed", e);
        }
    }

    public void pullNow() {
        Uri uri = getRoomUri();
        if (uri == null) {
            Log.d(TAG, "pullNow: no room URI set, skipping.");
            return;
        }

        InputStream is = null;

        try {
            is = appContext.getContentResolver().openInputStream(uri);
            if (is == null) {
                Log.w(TAG, "pullNow: openInputStream returned null");
                return;
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }

            String jsonText = bos.toString(StandardCharsets.UTF_8.name());
            JSONObject json = new JSONObject(jsonText);

            importDatabase(json);

            Log.d(TAG, "pullNow: imported DB from JSON, length=" + jsonText.length());
        } catch (Exception e) {
            Log.e(TAG, "pullNow failed", e);
        } finally {
            try {
                if (is != null) is.close();
            } catch (Exception ignored) {}
        }
    }

    // DB to JSON
    private JSONObject exportDatabse() throws JSONException {
        JSONObject root = new JSONObject();

        // roommates
        List<RoommateEntity> roommates = db.roommateDao().getAll();
        JSONArray roommatesArr = new JSONArray();
        for (RoommateEntity r : roommates) {
            JSONObject o = new JSONObject();
            o.put("id", r.id);
            o.put("name", r.name);
            roommatesArr.put(o);
        }
        root.put("roommates", roommatesArr);

        // chores
        List<ChoreEntity> chores = db.choreDao().getAll();
        JSONArray choresArr = new JSONArray();
        for (ChoreEntity c : chores) {
            JSONObject o = new JSONObject();
            o.put("id", c.id);
            o.put("name", c.name);
            o.put("frequency", c.frequency);
            o.put("roommateId", c.roommateId);
            o.put("dueDays", c.dueDays);
            choresArr.put(o);
        }
        root.put("chores", choresArr);

        // reminders
        List<ReminderEntity> reminders = db.reminderDao().getAll();
        JSONArray remsArr = new JSONArray();
        for (ReminderEntity r : reminders) {
            JSONObject o = new JSONObject();
            o.put("id", r.id);
            o.put("choreId", r.choreId);
            o.put("timeText", r.timeText);
            o.put("isAuto", r.isAuto);
            remsArr.put(o);
        }
        root.put("reminders", remsArr);

        // swaps (for "current week only" and future weekly logic)
        List<ChoreSwapEntity> swaps = db.choreSwapDao().getAll();
        JSONArray swapsArr = new JSONArray();
        for (ChoreSwapEntity s : swaps) {
            JSONObject o = new JSONObject();
            o.put("id", s.id);
            o.put("weekOffset", s.weekOffset);
            o.put("chore1Id", s.chore1Id);
            o.put("chore2Id", s.chore2Id);
            swapsArr.put(o);
        }
        root.put("swaps", swapsArr);

        return root;
    }

    // JSON to DB
    private void importDatabase(JSONObject root) throws  JSONException {
        // clear existing data
        List<ReminderEntity> oldRems = db.reminderDao().getAll();
        for (ReminderEntity r : oldRems) {
            db.reminderDao().delete(r);
        }
        db.choreSwapDao().deleteAll();
        List<ChoreEntity> oldChores = db.choreDao().getAll();
        for (ChoreEntity c : oldChores) {
            db.choreDao().delete(c);
        }
        List<RoommateEntity> oldRoommates = db.roommateDao().getAll();
        for (RoommateEntity r : oldRoommates) {
            db.roommateDao().delete(r);
        }

        // re-insert from JSON in order: roommates, chores, reminders

        // roommates
        JSONArray roommatesArr = root.optJSONArray("roommates");
        if (roommatesArr != null) {
            for (int i = 0; i < roommatesArr.length(); i++) {
                JSONObject o = roommatesArr.getJSONObject(i);
                RoommateEntity r = new RoommateEntity(o.optString("name", ""));
                r.id = o.optInt("id", 0);
                db.roommateDao().insert(r);
            }
        }

        // chores
        JSONArray choresArr = root.optJSONArray("chores");
        if (choresArr != null) {
            for (int i = 0; i < choresArr.length(); i++) {
                JSONObject o = choresArr.getJSONObject(i);
                ChoreEntity c = new ChoreEntity(
                        o.optString("name", ""),
                        o.optString("frequency", ""),
                        o.optInt("roommateId", 0),
                        o.optString("dueDays", "")
                );
                c.id = o.optInt("id", 0);
                db.choreDao().insert(c);
            }
        }

        // swaps
        JSONArray swapsArr = root.optJSONArray("swaps");
        if (swapsArr != null) {
            for (int i = 0; i < swapsArr.length(); i++) {
                JSONObject o = swapsArr.getJSONObject(i);
                ChoreSwapEntity s = new ChoreSwapEntity();
                s.id = o.optInt("id", 0);
                s.weekOffset = o.optInt("weekOffset", 0);
                s.chore1Id = o.optInt("chore1Id", 0);
                s.chore2Id = o.optInt("chore2Id", 0);
                db.choreSwapDao().insert(s);
            }
        }

        // reminders
        JSONArray remsArr = root.optJSONArray("reminders");
        if (remsArr != null) {
            for (int i = 0; i < remsArr.length(); i++) {
                JSONObject o = remsArr.getJSONObject(i);
                ReminderEntity r = new ReminderEntity(
                        o.optInt("choreId", 0),
                        o.optString("timeText", ""),
                        o.optBoolean("isAuto", false)
                );
                r.id = o.optInt("id", 0);
                db.reminderDao().insert(r);
            }
        }
    }
}