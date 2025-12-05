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

import android.graphics.Typeface;
import android.view.Gravity;
import android.graphics.drawable.GradientDrawable;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.view.View;

public class ScheduleViewActivity extends AppCompatActivity {

    private ImageButton prevWeekBtn, nextWeekBtn;
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

        scheduleTable.removeAllViews();

        if (roommates == null || roommates.isEmpty() || chores == null || chores.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setPadding(24, 24, 24, 24);
            empty.setText("Add roommates and chores to see the rotation.");
            empty.setTextSize(14);
            empty.setTextColor(ContextCompat.getColor(this, R.color.black));
            scheduleTable.addView(empty);

            setupUserDropdown(roommates == null ? java.util.Collections.emptyList() : roommates, chores);
            return;
        }

        // ----- Header row: "Chore" + roommate names -----
        TableRow header = new TableRow(this);
        header.addView(makeHeaderCell("Chore", Gravity.START));

        for (RoommateEntity r : roommates) {
            boolean isYou = (r.id == userId);
            TextView nameCell = makeHeaderCell(isYou ? r.name + " (You)" : r.name, Gravity.CENTER);
            if (isYou) {
                nameCell.setTextColor(ContextCompat.getColor(this, R.color.user_highlight));
            }
            header.addView(nameCell);
        }

        scheduleTable.addView(header);

// ----- One row per chore -----
        for (ChoreEntity c : chores) {
            TableRow row = new TableRow(this);

            // Chore name, left
            TextView choreCell = makeBodyCell(c.name, Gravity.START);
            row.addView(choreCell);

            // One dot cell per roommate
            int baseIndex = findRoommateIndexById(roommates, c.roommateId);
            for (int i = 0; i < roommates.size(); i++) {
                RoommateEntity r = roommates.get(i);
                int assignedIndex = (baseIndex + currentWeek) % roommates.size();
                boolean isAssigned = (roommates.get(assignedIndex).id == r.id);
                boolean isYou = (r.id == userId);

                LinearLayout dotCell = makeDotCell(isAssigned, isYou);
                row.addView(dotCell);
            }

            scheduleTable.addView(row);
        }

//        setupUserDropdown(roommates, chores);

        updatePrevButtonState();
        setupUserDropdown(roommates, chores);
        if (userId != -1) {
            userSelector.setEnabled(false);
        }
    }

    private TextView makeHeaderCell(String text, int gravity) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(8, 8, 8, 8);
        tv.setTextSize(14);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tv.setGravity(gravity);
        return tv;
    }

    private TextView makeBodyCell(String text, int gravity) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(8, 12, 8, 12);
        tv.setTextSize(15);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        tv.setGravity(gravity);
        return tv;
    }

    private LinearLayout makeDotCell(boolean isAssigned, boolean isYou) {
        LinearLayout container = new LinearLayout(this);
        container.setGravity(Gravity.CENTER);
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        container.setLayoutParams(params);
        container.setPadding(0, 8, 0, 8);

        if (!isAssigned) {
            return container; // empty cell
        }

        View dot = new View(this);
        int size = (int) (10 * getResources().getDisplayMetrics().density);

        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(size, size);
        dot.setLayoutParams(dotParams);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        int color = ContextCompat.getColor(
                this,
                isYou ? R.color.user_highlight : R.color.text_primary
        );
        shape.setColor(color);
        dot.setBackground(shape);

        container.addView(dot);
        return container;
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
    private ChoreEntity findChoreById(List<ChoreEntity> list, int id) {
        for (ChoreEntity c : list) if (c.id == id) return c;
        return null;
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

        List<String> yourChores = new ArrayList<>();
        for (ChoreEntity c : chores) {
            int baseIndex = findRoommateIndexById(roommates, c.roommateId);
            int assignedIndex = (baseIndex + currentWeek) % roommates.size();

            if (roommates.get(assignedIndex).name.equals(name)) {
                yourChores.add(c.name);
            }
        }

        if (yourChores.isEmpty()) {
            sb.append("No chores this week 🎉");
        } else {
            sb.append("Your Chores:\n");
            for (String cName : yourChores) {
                sb.append("• ").append(cName).append("\n");
            }
        }
        remindersText.setText(sb.toString().trim());
        remindersText.setTextSize(15);
        remindersText.setTextColor(
                ContextCompat.getColor(this, R.color.text_primary)
        );
    }

    private int findRoommateIndexById(List<RoommateEntity> list, int id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == id) return i;
        }
        return 0;
    }
}
