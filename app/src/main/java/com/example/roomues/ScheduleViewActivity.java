package com.example.roomues;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.*;
import android.view.View;
import android.widget.AdapterView;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ScheduleViewActivity extends AppCompatActivity {

    private Button prevWeekBtn, nextWeekBtn;
    private TableLayout scheduleTable;
    private Spinner userSelector;
    private TextView remindersText, weekLabel;
    private int currentWeek = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_view);

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

        // footer navigation
        Button choresListBtn = findViewById(R.id.choresListBtn);
        Button scheduleBtn = findViewById(R.id.scheduleBtn);
        choresListBtn.setOnClickListener(v ->
                startActivity(new Intent(this, ChoresListActivity.class))
        );
        scheduleBtn.setOnClickListener(v -> {}); // already here

        loadSchedule();
    }

    private void loadSchedule() {
        updateWeekLabel();

        RoomiesDatabase db = RoomiesDatabase.getDatabase(this);
        List<RoommateEntity> roommates = db.roommateDao().getAll();
        List<ChoreEntity> chores = db.choreDao().getAll();
        List<ChoreSwapEntity> swaps = db.choreSwapDao().getSwapsForWeek(currentWeek);

        scheduleTable.removeAllViews();

        // Add header row: chores + roommate names
        TableRow header = new TableRow(this);
        TextView choreHeader = new TextView(this);
        choreHeader.setText("Chore");
        choreHeader.setPadding(16, 8, 16, 8);
        header.addView(choreHeader);

        for (RoommateEntity r : roommates) {
            TextView name = new TextView(this);
            name.setText(r.name);
            name.setPadding(16, 8, 16, 8);
            header.addView(name);
        }
        scheduleTable.addView(header);

        // Add one row per chore
        for (int i = 0; i < chores.size(); i++) {
            ChoreEntity c = chores.get(i);
            TableRow row = new TableRow(this);

            TextView choreName = new TextView(this);
            choreName.setText(c.name);
            choreName.setPadding(16, 8, 16, 8);
            row.addView(choreName);

            for (int j = 0; j < roommates.size(); j++) {
                TextView cell = new TextView(this);
                cell.setPadding(16, 8, 16, 8);
                int assignedIndex = (i + currentWeek) % roommates.size();

                for (ChoreSwapEntity s : swaps) {
                    if (s.chore1Id == c.id) {
                        assignedIndex = findIndexByChoreId(chores, s.chore2Id, roommates.size());
                    } else if (s.chore2Id == c.id) {
                        assignedIndex = findIndexByChoreId(chores, s.chore1Id, roommates.size());
                    }
                }

                // show checkmark if this roommate has this chore this week
                cell.setText(assignedIndex == j ? "•" : "");
                row.addView(cell);
            }

            scheduleTable.addView(row);
            updatePrevButtonState();
        }

        // Dropdown for reminders
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                roommates.stream().map(r -> r.name).toArray(String[]::new));
        userSelector.setAdapter(adapter);

        userSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selected = roommates.get(pos).name;
                showRemindersFor(selected, chores, roommates);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // helper
    private int findIndexByChoreId(List<ChoreEntity> chores, int targetId, int roommateCount) {
        for (int k = 0; k < chores.size(); k++) {
            if (chores.get(k).id == targetId)
                return k % roommateCount;
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
        for (int i = 0; i < chores.size(); i++) {
            int assignedIndex = (i + currentWeek + 2) % roommates.size();
            String assigned = roommates.get(assignedIndex).name;
            if (assigned.equals(name)) {
                sb.append("• ").append(chores.get(i).name).append("\n");
            }
        }
        remindersText.setText("Your Chores:\n" + sb);
    }
}
