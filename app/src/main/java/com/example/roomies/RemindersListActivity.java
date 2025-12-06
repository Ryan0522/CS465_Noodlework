package com.example.roomies;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class RemindersListActivity extends AppCompatActivity {

    private LinearLayout remindersContainer;
    private RoomiesDatabase db;
    private TextView weekLabel;
    private int currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders_list);
        NavBarHelper.setupBottomNav(this, "reminder");
        weekLabel = findViewById(R.id.weekLabel);
        updateWeekLabel();

        db = RoomiesDatabase.getDatabase(this);
        remindersContainer = findViewById(R.id.remindersContainer);

        currentUserId = UserManager.getCurrentUser(this);
        if (currentUserId == -1) {
            Toast.makeText(this, "Please set up your profile first.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ReminderSetupActivity.class));
            finish();
            return;
        }

        loadReminders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReminders();
    }

    private void loadReminders() {
        updateWeekLabel();
        remindersContainer.removeAllViews();

        RoommateEntity me = db.roommateDao().getById(currentUserId);
        if (me == null) return;

        List<ChoreEntity> allChores = db.choreDao().getAll();
        for (ChoreEntity chore : allChores) {
            if (chore.roommateId != currentUserId) continue;

            // Section header
            TextView choreTitle = new TextView(this);
            choreTitle.setText(chore.name + " (" + chore.dueDays + ")");
            choreTitle.setTextSize(20);
            choreTitle.setTextColor(ContextCompat.getColor(this, R.color.nav_selected));
            choreTitle.setPadding(8, 24, 8, 8);
            remindersContainer.addView(choreTitle);

            // --- Load reminders and add auto if needed ---
            List<ReminderEntity> reminders = db.reminderDao().getByChore(chore.id);
            if (reminders.isEmpty()) {
                String autoDays = UserManager.getReminderDays(this);
                String autoTimes = UserManager.getReminderTimes(this);
                if (!autoDays.isEmpty() && !autoTimes.isEmpty()) {
                    String[] days = autoDays.split(",");
                    String[] times = autoTimes.split(",");

                    String dayLabel = days[0].trim();
                    String timeLabel = times[0].trim();
                    String autoText = dayLabel + " " + timeLabel;
                    ReminderEntity autoR = new ReminderEntity(chore.id, autoText, true);
                    autoR.triggerAtMillis = ReminderAutoGenerator.computeTriggerMillis(dayLabel, timeLabel);
                    long newId = db.reminderDao().insert(autoR);
                    autoR.id = (int) newId;

                    ReminderScheduler.scheduleReminder(this, autoR);
                    reminders = db.reminderDao().getByChore(chore.id);
                }
            }

            // --- Show reminders ---
            if (reminders.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText("No reminders yet.");
                empty.setPadding(16, 8, 16, 8);
                remindersContainer.addView(empty);
            } else {
                for (ReminderEntity r : reminders) {
                    // Card container
                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setPadding(32, 24, 32, 24);

                    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    cardParams.setMargins(0, 12, 0, 12);
                    card.setLayoutParams(cardParams);

                    GradientDrawable cardBg = new GradientDrawable();
                    cardBg.setShape(GradientDrawable.RECTANGLE);
                    cardBg.setColor(Color.WHITE);
                    cardBg.setCornerRadius(32);
                    cardBg.setStroke(2, ContextCompat.getColor(this, R.color.gray));
                    card.setBackground(cardBg);

                    // Top row: reminder text + edit/delete
                    LinearLayout topRow = new LinearLayout(this);
                    topRow.setOrientation(LinearLayout.HORIZONTAL);
                    topRow.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));

                    TextView timeTv = new TextView(this);
                    timeTv.setText(r.timeText + (r.isAuto ? " (Auto)" : ""));
                    timeTv.setTextSize(16);
                    timeTv.setTypeface(null, Typeface.BOLD);
                    timeTv.setLayoutParams(new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1
                    ));
                    topRow.addView(timeTv);

                    if (!r.isAuto) {
                        ImageButton editBtn = new ImageButton(this);
                        editBtn.setImageResource(android.R.drawable.ic_menu_edit);
                        editBtn.setBackgroundColor(Color.TRANSPARENT);
                        editBtn.setOnClickListener(v -> editReminder(r));
                        topRow.addView(editBtn);

                        ImageButton deleteBtn = new ImageButton(this);
                        deleteBtn.setImageResource(android.R.drawable.ic_menu_delete);
                        deleteBtn.setBackgroundColor(Color.TRANSPARENT);
                        deleteBtn.setOnClickListener(v -> deleteReminder(r));
                        topRow.addView(deleteBtn);
                    }

                    card.addView(topRow);

                    // Progress based on chore's due date (not reminder time)
                    long dueMillis = computeNextDueMillisForChore(chore);
                    long now = System.currentTimeMillis();
                    long dayMs = 24L * 60 * 60 * 1000;

                    int max = 100;
                    int progress = 0;
                    boolean overdue = false;
                    String statusText = "";

                    if (dueMillis > 0) {
                        if (now >= dueMillis) {
                            overdue = true;
                            progress = max;
                            statusText = "Overdue!";
                        } else {
                            long daysUntilDue = (dueMillis - now) / dayMs;
                            if (daysUntilDue == 0) {
                                statusText = "Due today";
                            } else if (daysUntilDue == 1) {
                                statusText = "Due tomorrow";
                            } else {
                                statusText = "Due in " + daysUntilDue + " days";
                            }

                            long windowStart = dueMillis - 7L * dayMs;
                            if (now <= windowStart) {
                                progress = 0;
                            } else {
                                float fraction = (float) (now - windowStart)
                                        / (float) (dueMillis - windowStart);
                                progress = (int) (fraction * max);
                            }
                        }
                    }

                    ProgressBar progressBar = new ProgressBar(this, null,
                            android.R.attr.progressBarStyleHorizontal);
                    progressBar.setMax(max);
                    progressBar.setProgress(progress);
                    tintProgressBar(progressBar, overdue);

                    LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    pbParams.setMargins(0, 16, 0, 4);
                    progressBar.setLayoutParams(pbParams);
                    card.addView(progressBar);

                    TextView statusTv = new TextView(this);
                    statusTv.setText(statusText);
                    statusTv.setTextSize(12);
                    statusTv.setTextColor(ContextCompat.getColor(
                            this,
                            overdue ? android.R.color.holo_red_dark : android.R.color.darker_gray
                    ));
                    card.addView(statusTv);

                    remindersContainer.addView(card);
                }
            }

            Button addBtn = new Button(this);
            addBtn.setText("+ Add Reminder");
            addBtn.setAllCaps(false);
            addBtn.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            addBtn.setTextSize(16);
            addBtn.setPadding(32, 24, 32, 24);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setColor(ContextCompat.getColor(this, R.color.primary));
            shape.setCornerRadius(999);
            addBtn.setBackground(shape);

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            btnParams.setMargins(0, 12, 0, 16);
            addBtn.setLayoutParams(btnParams);

            addBtn.setOnClickListener(v -> openAddReminder(chore.id));
            remindersContainer.addView(addBtn);

            // Divider
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 2));
            divider.setBackgroundColor(ContextCompat.getColor(this, R.color.gray));
            remindersContainer.addView(divider);
        }
    }

    private void tintProgressBar(ProgressBar pb, boolean overdue) {
        int colorRes = overdue ? R.color.progress_overdue : R.color.progress_normal;
        int color = ContextCompat.getColor(this, colorRes);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            pb.setProgressTintList(ColorStateList.valueOf(color));
        } else {
            Drawable d = pb.getProgressDrawable();
            if (d != null) {
                d = d.mutate();
                d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                pb.setProgressDrawable(d);
            }
        }
    }

    @SuppressLint("NewApi")
    private void updateWeekLabel() {
        LocalDate start = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        weekLabel.setText(fmt.format(start) + " - " + fmt.format(end));
    }

    private void deleteReminder(ReminderEntity r) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Reminder?")
                .setMessage("Are you sure you want to delete this reminder?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.reminderDao().delete(r);
                    Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show();
                    loadReminders();
                    SyncUtils.pushIfRoomLinked(this);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void editReminder(ReminderEntity r) {
        Intent i = new Intent(this, AddReminderActivity.class);
        i.putExtra("choreId", r.choreId);
        i.putExtra("reminderId", r.id);
        startActivity(i);
    }

    private void openAddReminder(int choreId) {
        Intent i = new Intent(this, AddReminderActivity.class);
        i.putExtra("choreId", choreId);
        startActivity(i);
    }

    @SuppressLint("NewApi")
    public static DayOfWeek mapShortDayToDow(String label) {
        if (label == null) return null;
        String l = label.trim().toLowerCase();
        switch (l) {
            case "mon":
                return DayOfWeek.MONDAY;
            case "tue":
                return DayOfWeek.TUESDAY;
            case "wed":
                return DayOfWeek.WEDNESDAY;
            case "thu":
                return DayOfWeek.THURSDAY;
            case "fri":
                return DayOfWeek.FRIDAY;
            case "sat":
                return DayOfWeek.SATURDAY;
            case "sun":
                return DayOfWeek.SUNDAY;
            default:
                return null;
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