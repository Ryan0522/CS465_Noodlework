package com.example.roomies;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
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

    private static final int SWAP_REQUEST_CODE = 100;

    private ImageButton prevWeekBtn, nextWeekBtn;
    private RecyclerView recyclerView;
    private ChoreAdapter adapter;
    private TextView weekLabel;

    private LinearLayout undoBar;
    private TextView undoMessage, undoButton;

    private CountDownTimer countDownTimer;
    private int pendingChore1Id = -1;
    private int pendingChore2Id = -1;

    private int currentWeek = 0;

    private List<RoommateEntity> roommates = new ArrayList<>();
    private List<ChoreEntity> chores = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chores_list);

        recyclerView = findViewById(R.id.choresRecyclerView);
        weekLabel = findViewById(R.id.weekDisplayText);
        prevWeekBtn = findViewById(R.id.prevWeekBtn);
        nextWeekBtn = findViewById(R.id.nextWeekBtn);
        Button addRoommateBtn = findViewById(R.id.addRoommateBtn);
        Button addChoreBtn = findViewById(R.id.addChoreBtn);
        Button swapBtn = findViewById(R.id.swapChoreBtn);

        undoBar = findViewById(R.id.undoBar);
        undoMessage = findViewById(R.id.undoMessage);
        undoButton = findViewById(R.id.undoButton);

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
                startActivityForResult(new Intent(this, SwapChoreActivity.class), SWAP_REQUEST_CODE)
        );

        undoButton.setOnClickListener(v -> performUndo());

        NavBarHelper.setupBottomNav(this, "home");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SWAP_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            pendingChore1Id = data.getIntExtra(SwapChoreActivity.EXTRA_CHORE1_ID, -1);
            pendingChore2Id = data.getIntExtra(SwapChoreActivity.EXTRA_CHORE2_ID, -1);
            String name1 = data.getStringExtra(SwapChoreActivity.EXTRA_SWAP_NAME1);
            String name2 = data.getStringExtra(SwapChoreActivity.EXTRA_SWAP_NAME2);

            if (pendingChore1Id != -1 && pendingChore2Id != -1) {
                showUndoBar(name1, name2);
                refreshList();
            }
        }
    }

    private void showUndoBar(String name1, String name2) {
        undoMessage.setText("Swapped: " + name1 + " ↔ " + name2);
        undoBar.setVisibility(View.VISIBLE);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                undoButton.setText("UNDO (" + (millisUntilFinished / 1000) + "s)");
            }

            @Override
            public void onFinish() {
                hideUndoBar();
                pendingChore1Id = -1;
                pendingChore2Id = -1;
            }
        }.start();
    }

    private void hideUndoBar() {
        undoBar.setVisibility(View.GONE);
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void performUndo() {
        if (pendingChore1Id != -1 && pendingChore2Id != -1) {
            RoomiesDatabase db = RoomiesDatabase.getDatabase(this);

            List<ChoreEntity> allChores = db.choreDao().getAll();
            ChoreEntity c1 = null;
            ChoreEntity c2 = null;

            for (ChoreEntity c : allChores) {
                if (c.id == pendingChore1Id) c1 = c;
                if (c.id == pendingChore2Id) c2 = c;
            }

            if (c1 != null && c2 != null) {
                int temp = c1.roommateId;
                c1.roommateId = c2.roommateId;
                c2.roommateId = temp;

                db.choreDao().update(c1);
                db.choreDao().update(c2);
            }

            pendingChore1Id = -1;
            pendingChore2Id = -1;
            hideUndoBar();
            refreshList();
        }
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
            adapter.setHighlightName(null);
            return;
        }

        for (RoommateEntity r : roommates) {
            List<String> theirChores = new ArrayList<>();

            for (ChoreEntity c : chores) {
                int baseRoommateIndex = findRoommateIndexById(roommates, c.roommateId);
                int assignedIndex = (baseRoommateIndex + currentWeek) % roommates.size();

                if (roommates.get(assignedIndex).id == r.id) {
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
        prevWeekBtn.setAlpha(currentWeek > 0 ? 1.0f : 0.5f);
    }

    private int findRoommateIndexById(List<RoommateEntity> list, int id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == id) return i;
        }
        return 0;
    }
}
