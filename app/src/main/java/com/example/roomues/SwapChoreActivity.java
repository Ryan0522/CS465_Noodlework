package com.example.roomues;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class SwapChoreActivity extends AppCompatActivity {

    private Spinner chore1Spinner, chore2Spinner;
    private Button swapButton, cancelButton;
    private List<ChoreWithRoommate> choresList;
    private RoomiesDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swap_chore);

        chore1Spinner = findViewById(R.id.chore1Spinner);
        chore2Spinner = findViewById(R.id.chore2Spinner);
        swapButton = findViewById(R.id.swapButton);
        cancelButton = findViewById(R.id.cancelButton);

        db = RoomiesDatabase.getDatabase(this);

        loadChores();

        swapButton.setOnClickListener(v -> showConfirmationDialog());
        cancelButton.setOnClickListener(v -> finish());
    }

    private void loadChores() {
        List<ChoreEntity> chores = db.choreDao().getAllChores();
        List<RoommateEntity> roommates = db.roommateDao().getAllRoommates();

        choresList = new ArrayList<>();
        List<String> displayNames = new ArrayList<>();

        for (ChoreEntity chore : chores) {
            String roommateName = "Unassigned";
            for (RoommateEntity roommate : roommates) {
                if (roommate.getId() == chore.getRoommateId()) {
                    roommateName = roommate.getName();
                    break;
                }
            }
            ChoreWithRoommate item = new ChoreWithRoommate(chore, roommateName);
            choresList.add(item);
            displayNames.add(roommateName + ", " + chore.getName());
        }

        if (choresList.isEmpty()) {
            Toast.makeText(this, "No chores available to swap", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, displayNames);
        chore1Spinner.setAdapter(adapter);
        chore2Spinner.setAdapter(adapter);

        // Ensure different items are selected by default
        if (choresList.size() > 1) {
            chore2Spinner.setSelection(1);
        }
    }

    private void showConfirmationDialog() {
        int pos1 = chore1Spinner.getSelectedItemPosition();
        int pos2 = chore2Spinner.getSelectedItemPosition();

        if (pos1 == pos2) {
            Toast.makeText(this, "Please select two different chores", Toast.LENGTH_SHORT).show();
            return;
        }

        ChoreWithRoommate chore1 = choresList.get(pos1);
        ChoreWithRoommate chore2 = choresList.get(pos2);

        // Inflate custom dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_swap, null);

        // Set the chore information
        TextView chore1Text = dialogView.findViewById(R.id.chore1Text);
        TextView chore2Text = dialogView.findViewById(R.id.chore2Text);

        chore1Text.setText(chore1.roommateName + " - " + chore1.chore.getName());
        chore2Text.setText(chore2.roommateName + " - " + chore2.chore.getName());

        new AlertDialog.Builder(this)
                .setTitle("Confirm Swap")
                .setView(dialogView)
                .setPositiveButton("Confirm", (dialog, which) -> performSwap(chore1, chore2))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performSwap(ChoreWithRoommate chore1, ChoreWithRoommate chore2) {
        // Swap the roommate IDs
        int tempRoommateId = chore1.chore.getRoommateId();
        chore1.chore.setRoommateId(chore2.chore.getRoommateId());
        chore2.chore.setRoommateId(tempRoommateId);

        // Update in database
        db.choreDao().update(chore1.chore);
        db.choreDao().update(chore2.chore);

        // Return to ChoresListActivity with success message
        Intent resultIntent = new Intent();
        resultIntent.putExtra("swapSuccess", true);
        resultIntent.putExtra("chore1Name", chore1.chore.getName());
        resultIntent.putExtra("chore2Name", chore2.chore.getName());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    // Helper class to hold chore and roommate name together
    private static class ChoreWithRoommate {
        ChoreEntity chore;
        String roommateName;

        ChoreWithRoommate(ChoreEntity chore, String roommateName) {
            this.chore = chore;
            this.roommateName = roommateName;
        }
    }
}
