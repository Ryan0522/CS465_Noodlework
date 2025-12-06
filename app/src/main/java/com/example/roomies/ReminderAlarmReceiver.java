package com.example.roomies;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

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

        String baseBody = (rem.timeText != null && !rem.timeText.isEmpty())
                ? rem.timeText
                : "Scheduled chore";

        // --- Compute progress & overdue ---
        long now = System.currentTimeMillis();
        long dueMillis = computeNextDueMillisForChore(chore);

        int max = 100;
        int progress = 0;
        boolean overdue = false;
        String statusSuffix = "";

        if (dueMillis > 0) {
            long dayMs = 24L * 60 * 60 * 1000;
            // window = 7 days before due
            long windowStart = dueMillis - 7L * dayMs;

            if (now >= dueMillis) {
                overdue = true;
                progress = max;
                statusSuffix = " • OVERDUE!";
            } else if (now <= windowStart) {
                progress = 0;
                // not yet in the “last week” window; we can show how many days until due
                long daysUntilDue = (dueMillis - now) / dayMs;
                if (daysUntilDue == 0) {
                    statusSuffix = " • Due today";
                } else if (daysUntilDue == 1) {
                    statusSuffix = " • Due tomorrow";
                } else {
                    statusSuffix = " • Due in " + daysUntilDue + " days";
                }
            } else {
                // within last week before due → progress from 0..100
                float fraction = (float)(now - windowStart) / (float)(dueMillis - windowStart);
                progress = (int)(fraction * max);

                long daysUntilDue = (dueMillis - now) / dayMs;
                if (daysUntilDue == 0) {
                    statusSuffix = " • Due today";
                } else if (daysUntilDue == 1) {
                    statusSuffix = " • Due tomorrow";
                } else {
                    statusSuffix = " • Due in " + daysUntilDue + " days";
                }
            }
        }

        String finalBody = baseBody + statusSuffix;

        int colorRes = overdue ? R.color.progress_overdue : R.color.progress_normal;
        int accentColor = ContextCompat.getColor(context, colorRes);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(choreName)
                .setContentText(finalBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(finalBody))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setColor(accentColor)
                .addAction(0, "Snooze", snoozePi)
                .addAction(0, "Done", donePi);

        // Only set progress if we have a due time
        if (dueMillis > 0) {
            builder.setProgress(max, progress, false);
        }

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        // Android 13+ POST_NOTIFICATIONS permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        nm.notify(rem.id, builder.build());
    }

    private void handleSnooze(Context context, ReminderDao remDao, ReminderEntity rem) {
        long now = System.currentTimeMillis();
        long snoozeMillis = 5L * 60L * 60L * 1000L; // 5 hours

        rem.timeText += " (Snoozed)";
        rem.triggerAtMillis = now + snoozeMillis;
        remDao.update(rem);

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.cancel(rem.id);
        Toast.makeText(context, "Reminder snoozed for 5 hours", Toast.LENGTH_SHORT).show();

        ReminderScheduler.scheduleReminder(context, rem);
    }

    private void handleDone(Context context, ReminderDao remDao, ReminderEntity rem) {
        // Delete this reminder from DB
        remDao.delete(rem);
        // Cancel any pending alarm
        ReminderScheduler.cancelReminder(context, rem.id);

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.cancel(rem.id);
        Toast.makeText(context, "Reminder marked as done", Toast.LENGTH_SHORT).show();
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

    @SuppressLint("NewApi")
    private static DayOfWeek mapShortDayToDow(String label) {
        if (label == null) return null;
        String l = label.trim().toLowerCase();
        switch (l) {
            case "mon": return DayOfWeek.MONDAY;
            case "tue": return DayOfWeek.TUESDAY;
            case "wed": return DayOfWeek.WEDNESDAY;
            case "thu": return DayOfWeek.THURSDAY;
            case "fri": return DayOfWeek.FRIDAY;
            case "sat": return DayOfWeek.SATURDAY;
            case "sun": return DayOfWeek.SUNDAY;
            default: return null;
        }
    }
    @SuppressLint("NewApi")
    private static long computeNextDueMillisForChore(ChoreEntity chore) {
        if (chore == null) return 0L;
        if (chore.dueDays == null || chore.dueDays.trim().isEmpty()) return 0L;

        LocalDate today = LocalDate.now();
        LocalTime dueTime = LocalTime.of(23, 59); // 11:59pm end-of-day
        DayOfWeek todayDow = today.getDayOfWeek();
        int todayVal = todayDow.getValue();
        LocalTime nowTime = LocalTime.now();

        String[] parts = chore.dueDays.split(",");
        List<DayOfWeek> dueDays = new ArrayList<>();

        for (String raw : parts) {
            String label = raw.trim();
            if (label.isEmpty()) continue;
            DayOfWeek target = mapShortDayToDow(label);
            if (target != null) {
                dueDays.add(target);
            }
        }

        if (dueDays.isEmpty()) return 0L;

        // ---------- 1) Look for the next due day later THIS week ----------
        // Includes "today" if it's not past 23:59 yet.
        Integer bestFutureDiff = null; // days from today, 0..6
        for (DayOfWeek target : dueDays) {
            int targetVal = target.getValue();
            int diff = targetVal - todayVal; // negative = earlier this week

            boolean isFuture = false;

            if (diff > 0) {
                // Later this week
                isFuture = true;
            } else if (diff == 0 && nowTime.isBefore(dueTime)) {
                // Today, and we haven't reached dueTime yet
                diff = 0;
                isFuture = true;
            }

            if (isFuture) {
                if (bestFutureDiff == null || diff < bestFutureDiff) {
                    bestFutureDiff = diff;
                }
            }
        }

        if (bestFutureDiff != null) {
            // There is still a due date later this week (or later today)
            LocalDate dueDate = today.plusDays(bestFutureDiff); // 0..6
            ZonedDateTime zdt = dueDate.atTime(dueTime).atZone(ZoneId.systemDefault());
            return zdt.toInstant().toEpochMilli();
        }

        // ---------- 2) No more due dates later this week => chore is OVERDUE ----------
        // We return the last due day that already passed in THIS week
        Integer bestPastTargetVal = null; // 1..7 (DayOfWeek values)
        for (DayOfWeek target : dueDays) {
            int targetVal = target.getValue();
            boolean isPast = false;

            if (targetVal < todayVal) {
                // Earlier day in this week
                isPast = true;
            } else if (targetVal == todayVal && !nowTime.isBefore(dueTime)) {
                // Today, but we're already past dueTime
                isPast = true;
            }

            if (isPast) {
                if (bestPastTargetVal == null || targetVal > bestPastTargetVal) {
                    bestPastTargetVal = targetVal;
                }
            }
        }

        if (bestPastTargetVal == null) {
            // This should be rare, but fallback: treat as today at dueTime.
            bestPastTargetVal = todayVal;
        }

        int diffDays = bestPastTargetVal - todayVal; // <= 0 (same or earlier this week)
        LocalDate dueDate = today.plusDays(diffDays);
        ZonedDateTime zdt = dueDate.atTime(dueTime).atZone(ZoneId.systemDefault());
        return zdt.toInstant().toEpochMilli();
    }
}