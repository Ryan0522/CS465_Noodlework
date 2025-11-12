package com.example.roomues;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.annotation.SuppressLint;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class ChoresListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChoreAdapter adapter;
    private TextView weekLabel;

    private int currentWeek = 0;  // Week offset from today (0 = current week)

    private List<RoommateEntity> roommates = new ArrayList<>();
    private List<ChoreEntity> chores = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chores_list);

        // --- find views ---
        recyclerView = findViewById(R.id.choresRecyclerView);
        weekLabel = findViewById(R.id.weekDisplayText);
        Button prevWeekBtn = findViewById(R.id.prevWeekBtn);
        Button nextWeekBtn = findViewById(R.id.nextWeekBtn);
        Button addRoommateBtn = findViewById(R.id.addRoommateBtn);
        Button addChoreBtn = findViewById(R.id.addChoreBtn);
        Button swapBtn = findViewById(R.id.swapChoreBtn);

        // --- setup list ---
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChoreAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // --- setup buttons ---
        prevWeekBtn.setOnClickListener(v -> {
            currentWeek--;
            refreshList();
        });
        nextWeekBtn.setOnClickListener(v -> {
            currentWeek++;
            refreshList();
        });

        addRoommateBtn.setOnClickListener(v ->
                startActivity(new Intent(this, AddRoommateActivity.class))
        );

        addChoreBtn.setOnClickListener(v ->
                startActivity(new Intent(this, AddChoreActivity.class))
        );

        swapBtn.setOnClickListener(v ->
                Toast.makeText(this, "Swap feature coming soon!", Toast.LENGTH_SHORT).show()
        );

        // --- footer navigation ---
        Button choresListBtn = findViewById(R.id.choresListBtn);
        Button scheduleBtn = findViewById(R.id.scheduleBtn);
        choresListBtn.setOnClickListener(v -> {
        }); // Already here
        scheduleBtn.setOnClickListener(v ->
                startActivity(new Intent(this, ScheduleViewActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    // --- helper: refresh list and label ---
    private void refreshList() {
        updateWeekLabel();

        RoomiesDatabase db = RoomiesDatabase.getDatabase(this);
        roommates = db.roommateDao().getAll();
        chores = db.choreDao().getAll();

        List<ChoreItem> groupedList = new ArrayList<>();

        if (roommates.isEmpty()) {
            adapter.updateList(groupedList);
            return;
        }

        // For each roommate, find their chores
        for (int i = 0; i < roommates.size(); i++) {
            RoommateEntity r = roommates.get(i);
            List<String> theirChores = new ArrayList<>();

            for (int j = 0; j < chores.size(); j++) {
                ChoreEntity c = chores.get(j);

                // rotation logic
                int assignIndex = (j + currentWeek) % roommates.size();
                if (roommates.get(assignIndex).id == r.id) {
                    theirChores.add(c.name);
                }
            }

            String choreText = theirChores.isEmpty()
                    ? "No chores this week"
                    : TextUtils.join(", ", theirChores);

            groupedList.add(new ChoreItem(r.name, choreText));
        }

        adapter.updateList(groupedList);
    }

    @SuppressLint("NewApi")
    private void updateWeekLabel() {
        LocalDate start = LocalDate.now().plusWeeks(currentWeek).with(DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        weekLabel.setText(fmt.format(start) + " - " + fmt.format(end));
    }
}
