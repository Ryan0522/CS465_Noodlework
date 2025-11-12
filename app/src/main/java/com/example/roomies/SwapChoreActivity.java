package com.example.roomies;

import android.os.Bundle;
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

        List<String> choreNames = new ArrayList<>();
        for (ChoreEntity c : chores) {
            String assigned = "(Unassigned)";
            for (RoommateEntity r : roommates) {
                if (r.id == c.roommateId) {
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

    private void confirmSwap() {
        int index1 = chore1Spinner.getSelectedItemPosition();
        int index2 = chore2Spinner.getSelectedItemPosition();

        if (index1 == index2) {
            Toast.makeText(this, "Please select two different chores.", Toast.LENGTH_SHORT).show();
            return;
        }

        ChoreEntity c1 = chores.get(index1);
        ChoreEntity c2 = chores.get(index2);

        RoommateEntity r1 = findRoommateById(c1.roommateId);
        RoommateEntity r2 = findRoommateById(c2.roommateId);

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
        int currentWeekOffset = 0;

        ChoreSwapEntity swap = new ChoreSwapEntity();
        swap.weekOffset = currentWeekOffset;
        swap.chore1Id = c1.id;
        swap.chore2Id = c2.id;

        db.choreSwapDao().insert(swap);

        Toast.makeText(this,
                "Swapped: " + r1.name + " ↔ " + r2.name,
                Toast.LENGTH_SHORT).show();

        finish();
    }
}
