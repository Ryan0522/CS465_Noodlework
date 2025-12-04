package com.example.roomies;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
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

        LocalTime time = LocalTime.of(hour, minute);
        String timeText = time.format(DateTimeFormatter.ofPattern("h:mm a"));

        String fullText = selectedDay + " " + timeText;
        if (!commentInput.getText().toString().trim().isEmpty()) {
            fullText += " — " + commentInput.getText().toString().trim();
        }

        long triggerAt = computeTriggerMillisFromSelection(selectedDay, time);

        if (editingReminder != null) {
            editingReminder.timeText = fullText;
            editingReminder.triggerAtMillis = triggerAt;
            db.reminderDao().update(editingReminder);
            ReminderScheduler.scheduleReminder(this, editingReminder);
            Toast.makeText(this, "Reminder updated!", Toast.LENGTH_SHORT).show();
        } else {
            ReminderEntity reminder = new ReminderEntity(chore.id, fullText, false);
            reminder.triggerAtMillis = triggerAt;
            long newId = db.reminderDao().insert(reminder);
            reminder.id = (int) newId;
            ReminderScheduler.scheduleReminder(this, reminder);
            Toast.makeText(this, "Reminder saved!", Toast.LENGTH_SHORT).show();
        }
        SyncUtils.pushIfRoomLinked(this);
        finish();
    }

    @SuppressLint("NewApi")
    public long computeTriggerMillisFromSelection(String dayLabel, LocalTime time) {
        java.time.LocalDate date = java.time.LocalDate.now();
        if ("Tomorrow".equalsIgnoreCase(dayLabel)) {
            date = date.plusDays(1);
        }
        if ("Today".equalsIgnoreCase(dayLabel)) {
            if (time.isBefore(java.time.LocalTime.now())) {
                date = date.plusDays(1);
            }
        }

        java.time.ZonedDateTime zdt = date.atTime(time)
                .atZone(java.time.ZoneId.systemDefault());

        return zdt.toInstant().toEpochMilli();
    }
}