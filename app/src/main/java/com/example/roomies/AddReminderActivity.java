package com.example.roomies;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Build;
import android.content.pm.PackageManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import android.annotation.SuppressLint;

public class AddReminderActivity extends AppCompatActivity {
    private RoomiesDatabase db;
    private ChoreEntity chore;
    private ReminderEntity editingReminder;

    private TextView roommateText, choreText, dueText;
    private EditText commentInput;
    private TimePicker timePicker;
    private Button saveBtn, cancelBtn;
    private Button todayBtn, tomorrowBtn, b8am, b3pm, b6pm, b9pm;

    private int choreId;
    private int reminderId = -1;
    private String selectedDay = "Today";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);

        db = RoomiesDatabase.getDatabase(this);

        // Get intent data
        choreId = getIntent().getIntExtra("choreId", -1);
        reminderId = getIntent().getIntExtra("reminderId", -1);

        roommateText = findViewById(R.id.roommateText);
        choreText = findViewById(R.id.choreText);
        dueText = findViewById(R.id.dueText);
        commentInput = findViewById(R.id.commentInput);
        timePicker = findViewById(R.id.timePicker);
        timePicker.setIs24HourView(false);
        saveBtn = findViewById(R.id.saveButton);
        cancelBtn = findViewById(R.id.cancelButton);

        todayBtn = findViewById(R.id.btnToday);
        tomorrowBtn = findViewById(R.id.btnTomorrow);
        b8am = findViewById(R.id.btn8am);
        b3pm = findViewById(R.id.btn3pm);
        b6pm = findViewById(R.id.btn6pm);
        b9pm = findViewById(R.id.btn9pm);

        // Find chore
        chore = db.choreDao().getAll().stream()
                .filter(c -> c.id == choreId)
                .findFirst().orElse(null);

        if (chore == null) {
            Toast.makeText(this, "Chore not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        RoommateEntity rm = db.roommateDao().getById(chore.roommateId);
        roommateText.setText(rm != null ? rm.name : "Unknown");
        choreText.setText(chore.name);
        dueText.setText(chore.dueDays);

        if (reminderId != -1) {
            for (ReminderEntity r : db.reminderDao().getAll()) {
                if (r.id == reminderId) {
                    editingReminder = r;
                    break;
                }
            }
            if (editingReminder != null) loadExistingReminder();
        }

        setupQuickButtons();

        saveBtn.setOnClickListener(v -> saveReminder());
        cancelBtn.setOnClickListener(v -> finish());
        NotificationHelper.sendNotification(this, 999, "Test Reminder", "This is a test!");

    }

    private void loadExistingReminder() {
        commentInput.setText("");
        String t = editingReminder.timeText;
        if (t == null) return;

        String[] parts = t.split("—");
        if (parts.length > 1) {
            commentInput.setText(parts[1].trim());
        }
    }

    private void setupQuickButtons() {
        todayBtn.setOnClickListener(v -> selectedDay = "Today");
        tomorrowBtn.setOnClickListener(v -> selectedDay = "Tomorrow");

        b8am.setOnClickListener(v -> setTime(8, 0));
        b3pm.setOnClickListener(v -> setTime(15, 0));
        b6pm.setOnClickListener(v -> setTime(18, 0));
        b9pm.setOnClickListener(v -> setTime(21, 0));
    }

    private void setTime(int hour, int minute) {
        timePicker.setHour(hour);
        timePicker.setMinute(minute);
    }

    @SuppressLint("NewApi")
    private void saveReminder() {
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();

        String timeText = String.format("%02d:%02d", hour, minute);
        String fullText = selectedDay + " " + timeText;
        if (!commentInput.getText().toString().trim().isEmpty()) {
            fullText += " — " + commentInput.getText().toString().trim();
        }

        if (editingReminder != null) {
            ReminderScheduler.cancelReminder(this, editingReminder.id);

            editingReminder.timeText = fullText;
            db.reminderDao().update(editingReminder);

            long triggerTime = calculateTriggerTime(fullText);
            Log.d("ReminderTest", "Updating reminder id=" + editingReminder.id + " for triggerTime=" + triggerTime);
            ReminderScheduler.scheduleReminder(this, editingReminder.id, chore.name, triggerTime);

            Toast.makeText(this, "Reminder updated!", Toast.LENGTH_SHORT).show();

        } else {
            ReminderEntity reminder = new ReminderEntity(choreId, fullText, false);
            long id = db.reminderDao().insert(reminder);
            reminder.id = (int) id;
            Log.d("ReminderDebug", "New reminder ID: " + reminder.id);

            long triggerTime = calculateTriggerTime(fullText);
            Log.d("ReminderDebug", "Scheduling reminder for triggerTime: " + triggerTime);

            ReminderScheduler.scheduleReminder(this, reminder.id, chore.name, triggerTime);


            Toast.makeText(this, "Reminder saved!", Toast.LENGTH_SHORT).show();
        }

        // finish(); // optional, keep for testing
    }

    private long calculateTriggerTime(String timeText) {
        // For testing, trigger 5 seconds from now
        return System.currentTimeMillis() + 5000;
    }



}