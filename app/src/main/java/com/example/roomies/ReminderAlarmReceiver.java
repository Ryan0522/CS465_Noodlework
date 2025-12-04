package com.example.roomies;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Receives reminder alarms and either:
 * - Shows the notification, or
 * - Handles Snooze / Done actions from the notification buttons.
 */
public class ReminderAlarmReceiver extends BroadcastReceiver {

    public static final String EXTRA_REMINDER_ID = "reminderId";
    public static final String EXTRA_ACTION = "action";

    public static final String ACTION_SNOOZE = "SNOOZE";
    public static final String ACTION_DONE = "DONE";

    public static final String CHANNEL_REMINDERS = "roomies_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        int reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1);
        String action = intent.getStringExtra(EXTRA_ACTION);

        if (reminderId == -1) return;

        RoomiesDatabase db = RoomiesDatabase.getDatabase(context);
        ReminderDao remDao = db.reminderDao();
        ReminderEntity rem = getReminderById(remDao, reminderId);
        if (rem == null) return;

        if (ACTION_SNOOZE.equals(action)) {
            handleSnooze(context, remDao, rem);
        } else if (ACTION_DONE.equals(action)) {
            handleDone(context, remDao, rem);
        } else {
            showNotification(context, db, rem);
        }
    }

    // --- helpers ---------------------------------------------------------

    // Your ReminderDao currently only has getAll(), so we search manually.
    private ReminderEntity getReminderById(ReminderDao dao, int id) {
        for (ReminderEntity r : dao.getAll()) {
            if (r.id == id) return r;
        }
        return null;
    }

    private ChoreEntity getChoreById(ChoreDao dao, int id) {
        for (ChoreEntity c : dao.getAll()) {
            if (c.id == id) return c;
        }
        return null;
    }

    private void showNotification(Context context, RoomiesDatabase db, ReminderEntity rem) {
        createReminderChannelIfNeeded(context);

        ChoreDao choreDao = db.choreDao();
        ChoreEntity chore = getChoreById(choreDao, rem.choreId);
        String choreName = (chore != null && chore.name != null && !chore.name.isEmpty())
                ? chore.name
                : "Chore reminder";

        // Snooze action
        Intent snoozeIntent = new Intent(context, ReminderAlarmReceiver.class);
        snoozeIntent.putExtra(EXTRA_REMINDER_ID, rem.id);
        snoozeIntent.putExtra(EXTRA_ACTION, ACTION_SNOOZE);
        PendingIntent snoozePi = PendingIntent.getBroadcast(
                context,
                rem.id * 10 + 1,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Done action
        Intent doneIntent = new Intent(context, ReminderAlarmReceiver.class);
        doneIntent.putExtra(EXTRA_REMINDER_ID, rem.id);
        doneIntent.putExtra(EXTRA_ACTION, ACTION_DONE);
        PendingIntent donePi = PendingIntent.getBroadcast(
                context,
                rem.id * 10 + 2,
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String body = (rem.timeText != null && !rem.timeText.isEmpty())
                ? rem.timeText
                : "Scheduled chore";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                // FIX: use an icon that really exists in your project
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(choreName)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(0, "Snooze", snoozePi)
                .addAction(0, "Done", donePi);

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        // Android 13+ POST_NOTIFICATIONS permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                // No permission -> just skip showing the notification
                return;
            }
        }

        nm.notify(rem.id, builder.build());
    }

    private void handleSnooze(Context context, ReminderDao remDao, ReminderEntity rem) {
        long now = System.currentTimeMillis();
        long snoozeMillis = 5L * 60L * 60L * 1000L; // 5 hours

        rem.triggerAtMillis = now + snoozeMillis;
        remDao.update(rem);

        ReminderScheduler.scheduleReminder(context, rem);
    }

    private void handleDone(Context context, ReminderDao remDao, ReminderEntity rem) {
        // Delete this reminder from DB
        remDao.delete(rem);
        // Cancel any pending alarm
        ReminderScheduler.cancelReminder(context, rem.id);
    }

    private void createReminderChannelIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm == null) return;

            NotificationChannel existing = nm.getNotificationChannel(CHANNEL_REMINDERS);
            if (existing != null) return;

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Chore Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for upcoming chores.");
            nm.createNotificationChannel(channel);
        }
    }
}