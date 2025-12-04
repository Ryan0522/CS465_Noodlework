package com.example.roomies;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.List;

public class RemindersListActivity extends AppCompatActivity {

    private LinearLayout remindersContainer;
    private RoomiesDatabase db;
    private int currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders_list);
        NavBarHelper.setupBottomNav(this, "reminder");

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
                    String autoText = days[0].trim() + " " + times[0].trim();
                    ReminderEntity autoR = new ReminderEntity(chore.id, autoText, true);
                    db.reminderDao().insert(autoR);
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
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(24, 8, 24, 8);

                    TextView timeTv = new TextView(this);
                    timeTv.setText(r.timeText + (r.isAuto ? " (Auto)" : ""));
                    timeTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                    row.addView(timeTv);

                    if (!r.isAuto) {
                        ImageButton editBtn = new ImageButton(this);
                        editBtn.setImageResource(android.R.drawable.ic_menu_edit);
                        editBtn.setBackgroundColor(0x00000000);
                        editBtn.setOnClickListener(v -> editReminder(r));
                        row.addView(editBtn);

                        ImageButton deleteBtn = new ImageButton(this);
                        deleteBtn.setImageResource(android.R.drawable.ic_menu_delete);
                        deleteBtn.setBackgroundColor(0x00000000);
                        deleteBtn.setOnClickListener(v -> deleteReminder(r));
                        row.addView(deleteBtn);
                    }

                    remindersContainer.addView(row);
                }
            }

            // Add Reminder button
            Button addBtn = new Button(this);
            addBtn.setText("+ Add Reminder");
            addBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.nav_selected));
            addBtn.setTextColor(ContextCompat.getColor(this, R.color.white));
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
                .show();    }

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
}