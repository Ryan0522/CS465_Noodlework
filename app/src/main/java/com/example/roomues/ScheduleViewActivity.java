package com.example.roomues;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ScheduleViewActivity extends AppCompatActivity {

    private int currentWeek = 0;
    private TableLayout scheduleTable;
    private Spinner userSelector;
    private TextView remindersText, weekLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_view);

        scheduleTable = findViewById(R.id.scheduleTable);
        userSelector = findViewById(R.id.userSelector);
        remindersText = findViewById(R.id.remindersText);
        weekLabel = findViewById(R.id.weekLabel);

        Button prevWeekBtn = findViewById(R.id.prevWeekBtn);
        Button nextWeekBtn = findViewById(R.id.nextWeekBtn);

        updateWeekLabel();
        loadSchedule();

        prevWeekBtn.setOnClickListener(v -> {
            currentWeek--;
            updateWeekLabel();
            loadSchedule();
        });
        nextWeekBtn.setOnClickListener(v -> {
            currentWeek++;
            updateWeekLabel();
            loadSchedule();
        });

        setupFooter();
    }

    private void setupFooter() {
        Button choresListBtn = findViewById(R.id.choresListBtn);
        Button scheduleBtn = findViewById(R.id.scheduleBtn);

        choresListBtn.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, ChoresListActivity.class))
        );
        scheduleBtn.setOnClickListener(v -> {}); // Already here
    }

    private void updateWeekLabel() {
        LocalDate start = LocalDate.now().plusWeeks(currentWeek).with(DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        weekLabel.setText(fmt.format(start) + " - " + fmt.format(end));
    }

    private void loadSchedule() {
        RoomiesDatabase db = RoomiesDatabase.getDatabase(this);
        List<RoommateEntity> roommates = db.roommateDao().getAllRoommates();
        List<ChoreEntity> chores = db.choreDao().getAllChores();

        scheduleTable.removeAllViews();

        // Add header row
        TableRow header = new TableRow(this);
        TextView choreHeader = new TextView(this);
        choreHeader.setText("Chore");
//        choreHeader.setTextStyle(android.graphics.Typeface.BOLD);
        header.addView(choreHeader);

        for (RoommateEntity r : roommates) {
            TextView t = new TextView(this);
            t.setText(r.getName());
//            t.setTextStyle(android.graphics.Typeface.BOLD);
            t.setPadding(16, 8, 16, 8);
            header.addView(t);
        }
        scheduleTable.addView(header);

        // Add rows
        for (ChoreEntity chore : chores) {
            TableRow row = new TableRow(this);
            TextView choreName = new TextView(this);
            choreName.setText(chore.getName());
            choreName.setPadding(8, 8, 8, 8);
            row.addView(choreName);

            for (int i = 0; i < roommates.size(); i++) {
                TextView cell = new TextView(this);
                int assignedIndex = (chores.indexOf(chore) + currentWeek + 2) % roommates.size();
                String assigned = roommates.get(assignedIndex).getName();
                cell.setText(assignedIndex == i ? "✓" : "");
                cell.setPadding(16, 8, 16, 8);
                row.addView(cell);
            }
            scheduleTable.addView(row);
        }

        // Populate user selector
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                roommates.stream().map(RoommateEntity::getName).toArray(String[]::new)
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        userSelector.setAdapter(adapter);

        userSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int pos, long id) {
                String name = roommates.get(pos).getName();
                showReminders(name, chores, roommates);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showReminders(String selectedName, List<ChoreEntity> chores, List<RoommateEntity> roommates) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chores.size(); i++) {
            int assignedIndex = (i + currentWeek + 2) % roommates.size();
            String assigned = roommates.get(assignedIndex).getName();
            if (assigned.equals(selectedName)) {
                sb.append("• ").append(chores.get(i).getName()).append("\n");
            }
        }
        remindersText.setText("Your Chores:\n" + sb);
    }
}
