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

    private Button prevWeekBtn, nextWeekBtn;
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
        prevWeekBtn = findViewById(R.id.prevWeekBtn);
        nextWeekBtn = findViewById(R.id.nextWeekBtn);
        Button addRoommateBtn = findViewById(R.id.addRoommateBtn);
        Button addChoreBtn = findViewById(R.id.addChoreBtn);
        Button swapBtn = findViewById(R.id.swapChoreBtn);

        // --- setup list ---
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChoreAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // --- setup buttons ---
        prevWeekBtn.setOnClickListener(v -> {
            if (currentWeek > 0) {
                currentWeek--;
                refreshList();
            }
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
                startActivity(new Intent(this, SwapChoreActivity.class))
        );

        NavBarHelper.setupBottomNav(this, "home");
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

        List<ChoreSwapEntity> swaps = db.choreSwapDao().getSwapsForWeek(currentWeek);

        List<ChoreItem> groupedList = new ArrayList<>();
        if (roommates.isEmpty()) {
            adapter.updateList(groupedList);

            adapter.setHighlightName(null);
            return;
        }

        // For each roommate, find their chores
        for (RoommateEntity r : roommates) {
            List<String> theirChores = new ArrayList<>();

            for (int j = 0; j < chores.size(); j++) {
                ChoreEntity c = chores.get(j);

                // Base rotation
                int assignIndex = (j + currentWeek) % roommates.size();
                int assignedRoommateId = roommates.get(assignIndex).id;

                // Apply weekly swap if exists
                for (ChoreSwapEntity s : swaps) {
                    if (s.chore1Id == c.id) {
                        // chore1 swapped -> now assigned to chore2's roommate
                        ChoreEntity swappedWith = findChoreById(chores, s.chore2Id);
                        if (swappedWith != null) {
                            int swpIndex = (findChoreIndex(chores, swappedWith) + currentWeek) % roommates.size();
                            assignedRoommateId = roommates.get(swpIndex).id;
                        }
                    } else if (s.chore2Id == c.id) {
                        // chore2 swapped -> now assigned to chore1's roommate
                        ChoreEntity swappedWith = findChoreById(chores, s.chore1Id);
                        if (swappedWith != null) {
                            int swpIndex = (findChoreIndex(chores, swappedWith) + currentWeek) % roommates.size();
                            assignedRoommateId = roommates.get(swpIndex).id;
                        }
                    }
                }

                if (assignedRoommateId == r.id) {
                    theirChores.add(c.name);
                }
            }

            String choreText = theirChores.isEmpty()
                    ? "No chores this week"
                    : TextUtils.join(", ", theirChores);
            groupedList.add(new ChoreItem(r.name, choreText));
        }

        adapter.updateList(groupedList);

        int uid = UserManager.getCurrentUser(this);
        String highlight = null;
        if (uid != -1) {
            for (RoommateEntity r : roommates) {
                if (r.id == uid) { highlight = r.name; break; }
            }
        }
        adapter.setHighlightName(highlight);

        updatePrevButtonState();
    }

    // helper
    private ChoreEntity findChoreById(List<ChoreEntity> list, int id) {
        for (ChoreEntity c : list) if (c.id == id) return c;
        return null;
    }

    private int findChoreIndex(List<ChoreEntity> list, ChoreEntity target) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).id == target.id) return i;
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
}
