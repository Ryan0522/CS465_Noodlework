package com.example.roomues;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
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
    private RoomiesViewModel viewModel;

    private int currentWeek = 0;  // Week offset from today (0 = current week)
    private TextView weekDisplayText;
    private static final int SWAP_CHORE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chores_list);

        recyclerView = findViewById(R.id.choresRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ChoreAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
        weekDisplayText = findViewById(R.id.weekDisplayText);
        Button prevWeekBtn = findViewById(R.id.prevWeekBtn);
        Button nextWeekBtn = findViewById(R.id.nextWeekBtn);

        updateDisplayedWeek();

        prevWeekBtn.setOnClickListener(v -> {
            currentWeek--;
            updateDisplayedWeek();
            refreshChoreList();
        });

        nextWeekBtn.setOnClickListener(v -> {
            currentWeek++;
            updateDisplayedWeek();
            refreshChoreList();
        });

        viewModel = new ViewModelProvider(this).get(RoomiesViewModel.class);

        viewModel.getAllChores().observe(this, chores -> {
            RoomiesDatabase db = RoomiesDatabase.getDatabase(this);
            List<RoommateEntity> roommates = db.roommateDao().getAllRoommates();
            List<ChoreItem> displayList = new ArrayList<>();

            for (ChoreEntity c : chores) {
                String roommateName = "Unassigned";
                for (RoommateEntity r : roommates) {
                    if (r.getId() == c.getRoommateId()) {
                        roommateName = r.getName();
                        break;
                    }
                }
                displayList.add(new ChoreItem(roommateName, c.getName()));
            }
            adapter.updateList(displayList);
        });
        setupFooter();

        Button addRoommateBtn = findViewById(R.id.addRoommateBtn);
        Button addChoreBtn = findViewById(R.id.addChoreBtn);
        Button swapChoreBtn = findViewById(R.id.swapChoreBtn);

        addChoreBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ChoresListActivity.this, AddChoreActivity.class);
            startActivity(intent);
        });

        addRoommateBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ChoresListActivity.this, AddRoommateActivity.class);
            startActivity(intent);
        });

        swapChoreBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ChoresListActivity.this, SwapChoreActivity.class);
            startActivityForResult(intent, SWAP_CHORE_REQUEST);
        });

    }
//    private void updateDisplayedWeek() {
//        weekDisplayText.setText("Week " + currentWeek);
//    }
    private void updateDisplayedWeek() {
        LocalDate start = LocalDate.now().plusWeeks(currentWeek).with(DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        weekDisplayText.setText(fmt.format(start) + " - " + fmt.format(end));
    }
    private void refreshChoreList() {
        RoomiesDatabase db = RoomiesDatabase.getDatabase(this);
        List<RoommateEntity> roommates = db.roommateDao().getAllRoommates();
        List<ChoreEntity> chores = db.choreDao().getAllChores();

        List<ChoreItem> displayList = new ArrayList<>();

        if (roommates.isEmpty()) return;

        for (int i = 0; i < chores.size(); i++) {
            // Determine which roommate gets this chore for the selected week
            int rotatedIndex = (i + currentWeek) % roommates.size();
            String roommateName = roommates.get(rotatedIndex).getName();
            displayList.add(new ChoreItem(roommateName, chores.get(i).getName()));
        }

        adapter.updateList(displayList);
    }

    private void setupFooter() {
        Button choresListBtn = findViewById(R.id.choresListBtn);
        Button scheduleBtn = findViewById(R.id.scheduleBtn);

        scheduleBtn.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, ScheduleViewActivity.class))
        );
        choresListBtn.setOnClickListener(v -> {}); // Already here
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SWAP_CHORE_REQUEST && resultCode == RESULT_OK && data != null) {
            boolean swapSuccess = data.getBooleanExtra("swapSuccess", false);
            if (swapSuccess) {
                String chore1Name = data.getStringExtra("chore1Name");
                String chore2Name = data.getStringExtra("chore2Name");
                Toast.makeText(this,
                    "Successfully swapped: " + chore1Name + " ↔ " + chore2Name,
                    Toast.LENGTH_LONG).show();
            }
        }
    }
}
