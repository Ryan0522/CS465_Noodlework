package com.example.roomues;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class AddChoreActivity extends AppCompatActivity {

    private EditText inputChore;
    private Spinner frequencySpinner, roommateSpinner;
    private ListView choreListView;
    private ArrayAdapter<String> choreListAdapter;

    private RoomiesDatabase db;
    private List<RoommateEntity> roommates = new ArrayList<>();
    private List<ChoreEntity> chores = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_chore);

        inputChore = findViewById(R.id.inputChore);
        frequencySpinner = findViewById(R.id.frequencySpinner);
        roommateSpinner = findViewById(R.id.roommateSpinner);
        Button saveBtn = findViewById(R.id.saveButton);
        Button cancelBtn = findViewById(R.id.cancelButton);
        choreListView = findViewById(R.id.choreList);

        db = RoomiesDatabase.getDatabase(this);

        // Setup frequency dropdown
        String[] frequencies = {
                "1x a Week",
                "2x a Week",
                "3x a Week",
                "Daily"
        };
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                frequencies
        );
        frequencySpinner.setAdapter(freqAdapter);

        loadRoommates();

        saveBtn.setOnClickListener(v -> saveChore());
        cancelBtn.setOnClickListener(v -> finish());

        choreListView.setOnItemLongClickListener((parent, view, position, id) -> {
            ChoreEntity selectedChore = chores.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Delete Chore")
                    .setMessage("Delete chore \"" + selectedChore.name + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        db.choreDao().delete(selectedChore);
                        Toast.makeText(this, "Chore deleted.", Toast.LENGTH_SHORT).show();
                        loadChoreList();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        loadChoreList();
    }

    private void saveChore() {
        String choreName = inputChore.getText().toString().trim();
        if (choreName.isEmpty()) {
            inputChore.setError("Enter a chore name");
            return;
        }

        if (roommates.isEmpty()) {
            Toast.makeText(this, "Add a roommate first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String frequency = frequencySpinner.getSelectedItem().toString();
        int selectedIndex = roommateSpinner.getSelectedItemPosition();
        int roommateId = roommates.get(selectedIndex).id;

        // Create and insert new chore
        ChoreEntity newChore = new ChoreEntity(choreName, frequency, roommateId);
        db.choreDao().insert(newChore);

        // Clear input and refresh list
        inputChore.setText("");
        Toast.makeText(this, "Chore added!", Toast.LENGTH_SHORT).show();
        loadChoreList();
    }

    private void loadRoommates() {
        roommates = db.roommateDao().getAll();
        List<String> names = new ArrayList<>();

        for (RoommateEntity r : roommates) {
            names.add(TextUtils.isEmpty(r.name) ? "(Unnamed)" : r.name);
        }

        ArrayAdapter<String> roommateAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                names
        );
        roommateSpinner.setAdapter(roommateAdapter);
    }

    private void loadChoreList() {
        chores = db.choreDao().getAll();
        List<String> choreNames = new ArrayList<>();

        for (ChoreEntity c : chores) {
            String roommateName = "(Unassigned)";
            for (RoommateEntity r : db.roommateDao().getAll()) {
                if (r.id == c.roommateId) {
                    roommateName = r.name;
                    break;
                }
            }
            choreNames.add(c.name + " • " + c.frequency + " • " + roommateName);
        }

        choreListAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                choreNames
        );
        choreListView.setAdapter(choreListAdapter);
    }
}
