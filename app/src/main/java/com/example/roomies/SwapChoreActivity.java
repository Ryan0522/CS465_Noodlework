package com.example.roomies;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class SwapChoreActivity  extends AppCompatActivity{

    private Spinner chore1Spinner, chore2Spinner;
    private Button swapBtn, cancelBtn;

    private RoomiesDatabase db;
    private List<ChoreEntity> chores;
    private List<RoommateEntity> roommates;

    public static final String EXTRA_CHORE1_ID = "chore1_id";
    public static final String EXTRA_CHORE2_ID = "chore2_id";
    public static final String EXTRA_SWAP_NAME1 = "swap_name1";
    public static final String EXTRA_SWAP_NAME2 = "swap_name2";

    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_swap_chore);

        chore1Spinner = findViewById(R.id.chore1Spinner);
        chore2Spinner = findViewById(R.id.chore2Spinner);
        swapBtn = findViewById(R.id.swapButton);
        cancelBtn = findViewById(R.id.cancelButton);

        db = RoomiesDatabase.getDatabase(this);
        loadChores();

        cancelBtn.setOnClickListener(v -> finish());
        swapBtn.setOnClickListener(v -> confirmSwap());
    }

    private void loadChores() {
        chores = db.choreDao().getAll();
        roommates = db.roommateDao().getAll();

        if (chores.isEmpty() || roommates.isEmpty()) {
            Toast.makeText(this, "No chores or roommates found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        List<ChoreSwapEntity> currentSwaps = db.choreSwapDao().getSwapsForWeek(0);

        List<String> choreNames = new ArrayList<>();
        for (ChoreEntity c : chores) {
            int assignedRoommateId = c.roommateId;

            for (ChoreSwapEntity swap : currentSwaps) {
                if (swap.chore1Id == c.id) {
                    ChoreEntity other = findChoreById(swap.chore2Id);
                    if (other != null) assignedRoommateId = other.roommateId;
                } else if (swap.chore2Id == c.id) {
                    ChoreEntity other = findChoreById(swap.chore1Id);
                    if (other != null) assignedRoommateId = other.roommateId;
                }
            }

            String assigned = "(Unassigned)";
            for (RoommateEntity r : roommates) {
                if (r.id == assignedRoommateId) {
                    assigned = r.name;
                    break;
                }
            }
            choreNames.add(assigned + " - " + c.name);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                choreNames
        );
        chore1Spinner.setAdapter(adapter);
        chore2Spinner.setAdapter(adapter);
    }

    private ChoreEntity findChoreById(int id) {
        for (ChoreEntity c : chores) {
            if (c.id == id) return c;
        }
        return null;
    }

    private void confirmSwap() {
        int index1 = chore1Spinner.getSelectedItemPosition();
        int index2 = chore2Spinner.getSelectedItemPosition();

        if (index1 == index2) {
            Toast.makeText(this, "Please select two different chores.", Toast.LENGTH_SHORT).show();
            return;
        }

        ChoreEntity c1 = chores.get(index1);
        ChoreEntity c2 = chores.get(index2);

        List<ChoreSwapEntity> currentSwaps = db.choreSwapDao().getSwapsForWeek(0);

        int assignedId1 = c1.roommateId;
        int assignedId2 = c2.roommateId;

        for (ChoreSwapEntity swap : currentSwaps) {
            if (swap.chore1Id == c1.id) {
                ChoreEntity other = findChoreById(swap.chore2Id);
                if (other != null) assignedId1 = other.roommateId;
            } else if (swap.chore2Id == c1.id) {
                ChoreEntity other = findChoreById(swap.chore1Id);
                if (other != null) assignedId1 = other.roommateId;
            }

            if (swap.chore1Id == c2.id) {
                ChoreEntity other = findChoreById(swap.chore2Id);
                if (other != null) assignedId2 = other.roommateId;
            } else if (swap.chore2Id == c2.id) {
                ChoreEntity other = findChoreById(swap.chore1Id);
                if (other != null) assignedId2 = other.roommateId;
            }
        }

        RoommateEntity r1 = findRoommateById(assignedId1);
        RoommateEntity r2 = findRoommateById(assignedId2);

        if (r1 == null || r2 == null) {
            Toast.makeText(this, "Roommate assignment missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        showConfirmationDialog(c1, c2, r1, r2);
    }

    private RoommateEntity findRoommateById(int id) {
        for (RoommateEntity r : roommates) {
            if (r.id == id) return r;
        }
        return null;
    }

    private void showConfirmationDialog(ChoreEntity c1, ChoreEntity c2, RoommateEntity r1, RoommateEntity r2) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout dialogLayout = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.dialog_confirm_swap, null);

        TextView chore1Text = dialogLayout.findViewById(R.id.chore1Text);
        TextView chore2Text = dialogLayout.findViewById(R.id.chore2Text);
        chore1Text.setText(r1.name + " - " + c1.name);
        chore2Text.setText(r2.name + " - " + c2.name);

        builder.setView(dialogLayout)
                .setTitle("Confirm Swap")
                .setPositiveButton("Confirm", (dialog, which) -> performSwap(c1, c2, r1, r2))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performSwap(ChoreEntity c1, ChoreEntity c2, RoommateEntity r1, RoommateEntity r2) {
        List<ChoreSwapEntity> currentSwaps = db.choreSwapDao().getSwapsForWeek(0);

        for (ChoreSwapEntity swap : currentSwaps) {
            if (swap.chore1Id == c1.id || swap.chore2Id == c1.id ||
                swap.chore1Id == c2.id || swap.chore2Id == c2.id) {
                db.choreSwapDao().deleteById((int) swap.id);
            }
        }

        int temp = c1.roommateId;
        c1.roommateId = c2.roommateId;
        c2.roommateId = temp;

        db.choreDao().update(c1);
        db.choreDao().update(c2);

        SyncUtils.pushIfRoomLinked(this);

        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_CHORE1_ID, c1.id);
        resultIntent.putExtra(EXTRA_CHORE2_ID, c2.id);
        resultIntent.putExtra(EXTRA_SWAP_NAME1, r1.name);
        resultIntent.putExtra(EXTRA_SWAP_NAME2, r2.name);
        setResult(RESULT_OK, resultIntent);

        finish();
    }
}
