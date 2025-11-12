package com.example.roomies;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ScheduleViewActivity extends AppCompatActivity {

    private Button prevWeekBtn, nextWeekBtn;
    private TableLayout scheduleTable;
    private Spinner userSelector;
    private TextView remindersText, weekLabel;

    private int currentWeek = 0;
    private int userId = -1;
    private String currentUserName = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_view);

        TextView header = findViewById(R.id.scheduleHeader);
        if (userId != -1) {
            RoommateEntity me = RoomiesDatabase.getDatabase(this).roommateDao().getById(userId);
            if (me != null) {
                header.setText("Weekly Chore Schedule — " + me.name);
                header.setTextColor(getResources().getColor(R.color.nav_selected));
            }
        }
        RoomiesDatabase db = RoomiesDatabase.getDatabase(this);

        if (userId != -1) {
            RoommateEntity me = db.roommateDao().getById(userId);
            header.setText("Weekly Chore Schedule — " + me.name);
            header.setTextColor(getResources().getColor(R.color.nav_selected));
        }

        scheduleTable = findViewById(R.id.scheduleTable);
        userSelector = findViewById(R.id.userSelector);
        remindersText = findViewById(R.id.remindersText);
        weekLabel = findViewById(R.id.weekLabel);

        scheduleTable.setStretchAllColumns(true);
        scheduleTable.setShrinkAllColumns(true);

        prevWeekBtn = findViewById(R.id.prevWeekBtn);
        nextWeekBtn = findViewById(R.id.nextWeekBtn);

        prevWeekBtn.setOnClickListener(v -> { currentWeek--; loadSchedule();});
        nextWeekBtn.setOnClickListener(v -> { currentWeek++; loadSchedule();});

        NavBarHelper.setupBottomNav(this, "schedule");
        loadSchedule();
    }

    @Override
    protected void onResume() {
        super.onResume();
        userId = UserManager.getCurrentUser(this);

        if (userId != -1) {
            RoomiesDatabase db = RoomiesDatabase.getDatabase(this);
            RoommateEntity me = db.roommateDao().getById(userId);
            if (me != null) currentUserName = me.name;
        }
        loadSchedule();
    }

    private void loadSchedule() {
        updateWeekLabel();

        RoomiesDatabase db = RoomiesDatabase.getDatabase(this);
        List<RoommateEntity> roommates = db.roommateDao().getAll();
        List<ChoreEntity> chores = db.choreDao().getAll();
        List<ChoreSwapEntity> swaps = db.choreSwapDao().getSwapsForWeek(currentWeek);

        scheduleTable.removeAllViews();

        if (roommates == null || roommates.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setPadding(24, 24, 24, 24);
            empty.setText("Add yourself on the Reminders screen to see assignments.");
            scheduleTable.addView(empty);

            setupUserDropdown(Collections.emptyList(), chores); // keep spinner empty
            updatePrevButtonState();
            return;
        }

        // Add header row: chores + roommate names
        TableRow header = new TableRow(this);
        TextView choreHeader = new TextView(this);
        choreHeader.setText("Chore");
        choreHeader.setPadding(16, 8, 16, 8);
        choreHeader.setTextSize(18);
        header.addView(choreHeader);

        for (RoommateEntity r : roommates) {
            TextView nameTv = new TextView(this);
            nameTv.setText(r.name);
            nameTv.setPadding(16, 8, 16, 8);
            nameTv.setTextSize(18);
            if (r.id == userId) {
                nameTv.setTextColor(ContextCompat.getColor(this, R.color.user_highlight));
            }
            header.addView(nameTv);
        }
        scheduleTable.addView(header);

        // Add one row per chore
        for (int i = 0; i < chores.size(); i++) {
            ChoreEntity c = chores.get(i);
            TableRow row = new TableRow(this);

            TextView choreName = new TextView(this);
            choreName.setText(c.name);
            choreName.setPadding(16, 8, 16, 8);
            choreName.setTextSize(18);
            row.addView(choreName);

            // Base rotation
            int assignedIndex = findRoommateIndexById(roommates, c.roommateId);
            if (currentWeek > 0) {
                assignedIndex = (assignedIndex + currentWeek) % roommates.size();
            }

            // With Swap
            for (ChoreSwapEntity s : swaps) {
                if (s.chore1Id == c.id) {
                    int k = findChoreIndex(chores, s.chore2Id);
                    if (k != -1) assignedIndex = (k + currentWeek) % roommates.size();
                } else if (s.chore2Id == c.id) {
                    int k = findChoreIndex(chores, s.chore1Id);
                    if (k != -1) assignedIndex = (k + currentWeek) % roommates.size();
                }
            }

            for (int j = 0; j < roommates.size(); j++) {
                TextView cell = new TextView(this);
                cell.setPadding(16, 8, 16, 8);
                cell.setTextSize(18);
                cell.setText(j == assignedIndex ? "●" : ""); // mark the assignee

                // Highlight the mark if the assignee is the current user
                if (j == assignedIndex && roommates.get(j).id == userId) {
                    cell.setTextColor(ContextCompat.getColor(this, R.color.user_highlight));
                    cell.setTypeface(Typeface.DEFAULT_BOLD);
                }

                row.addView(cell);
            }

            scheduleTable.addView(row);
        }

        updatePrevButtonState();
        setupUserDropdown(roommates, chores);
        if (userId != -1) {
            userSelector.setEnabled(false);
        }
    }

    private void setupUserDropdown(List<RoommateEntity> roommates, List<ChoreEntity> chores) {
        List<String> names = new ArrayList<>();
        for (RoommateEntity r : roommates) names.add(r.name);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        userSelector.setAdapter(adapter);

        // Preselect current user if available
        int preselectIndex = -1;
        if (currentUserName != null) {
            preselectIndex = names.indexOf(currentUserName);
        }
        if (preselectIndex >= 0) {
            userSelector.setSelection(preselectIndex);
            showRemindersFor(currentUserName, chores, roommates);
        } else if (!names.isEmpty()) {
            userSelector.setSelection(0);
            showRemindersFor(names.get(0), chores, roommates);
        }

        userSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos >= 0 && pos < roommates.size()) {
                    String selected = roommates.get(pos).name;
                    showRemindersFor(selected, chores, roommates);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // helper
    private int findChoreIndex(List<ChoreEntity> list, int targetId) {
        for (int k = 0; k < list.size(); k++) {
            if (list.get(k).id == targetId) return k;
        }
        return -1;
    }

    @SuppressLint("NewApi")
    private void updateWeekLabel() {
        LocalDate start = LocalDate.now().plusWeeks(currentWeek).with(DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        weekLabel.setText(fmt.format(start) + " - " + fmt.format(end));
    }

    private void updatePrevButtonState() {
        prevWeekBtn.setEnabled(currentWeek > 0);
        prevWeekBtn.setAlpha(currentWeek > 0 ? 1.0f : 0.5f); // dim when disabled
    }

    private void showRemindersFor(String name, List<ChoreEntity> chores, List<RoommateEntity> roommates) {
        StringBuilder sb = new StringBuilder();
        for (ChoreEntity c : chores) {
            int assignedIndex = findRoommateIndexById(roommates, c.roommateId);
            if (currentWeek > 0) {
                assignedIndex = (assignedIndex + currentWeek) % roommates.size();
            }
            String assigned = roommates.get(assignedIndex).name;
            if (assigned.equals(name)) {
                sb.append("• ").append(c.name).append("\n");
            }
        }
        remindersText.setText("Your Chores:\n" + sb);
    }

    private int findRoommateIndexById(List<RoommateEntity> list, int id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == id) return i;
        }
        return 0;
    }
}
