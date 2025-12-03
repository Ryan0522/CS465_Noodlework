package com.example.roomies;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class AddChoreActivity extends AppCompatActivity {

    private EditText inputChore;
    private Spinner frequencySpinner, roommateSpinner;
    private LinearLayout daysLayout;
    private ListView choreListView;
    private Button saveBtn;

    private RoomiesDatabase db;
    private List<RoommateEntity> roommates = new ArrayList<>();
    private List<ChoreEntity> chores = new ArrayList<>();
    private ArrayAdapter<String> choreListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_chore);

        ImageButton closeButton = findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();  // closes the screen
            }
        });

        inputChore = findViewById(R.id.inputChore);
        frequencySpinner = findViewById(R.id.frequencySpinner);
        roommateSpinner = findViewById(R.id.roommateSpinner);
        choreListView = findViewById(R.id.choreList);
        daysLayout = findViewById(R.id.daysLayout);
        saveBtn = findViewById(R.id.saveButton);

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

        // load roommates and chores
        loadRoommates();
        loadChoreList();

        // Button actions
        saveBtn.setOnClickListener(v -> saveChore(true));

        // IME Done key triggers save & close
        inputChore.setImeOptions(EditorInfo.IME_ACTION_DONE);
        inputChore.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveChore(true);
                return true;
            }
            return false;
        });

        // Delete chore on long click
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
    }

    private void saveChore(boolean closeAfterSave) {
        String choreName = inputChore.getText().toString().trim();
        if (choreName.isEmpty()) {
            inputChore.setError("Enter a chore name");
            return;
        }

        if (roommates.isEmpty()) {
            Toast.makeText(this, "Add a roommate first.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get frequency and map it to number of required days
        String frequency = frequencySpinner.getSelectedItem().toString();
        int requiredDays = 1;
        switch (frequency) {
            case "1x a Week":
                requiredDays = 1;
                break;
            case "2x a Week":
                requiredDays = 2;
                break;
            case "3x a Week":
                requiredDays = 3;
                break;
            case "Daily":
                requiredDays = 7;
                break;
        }

        // Collect selected days
        List<String> selectedDays = new ArrayList<>();
        for (int i = 0; i < daysLayout.getChildCount(); i++) {
            View v = daysLayout.getChildAt(i);
            if (v instanceof CheckBox && ((CheckBox) v).isChecked()) {
                selectedDays.add(((CheckBox) v).getText().toString());
            }
        }
        if (selectedDays.size() != requiredDays) {
            Toast.makeText(
                    this,
                    "You selected " + selectedDays.size() +
                            " day(s), but " + frequency + " requires " + requiredDays + ".",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String dueDaysCsv = TextUtils.join(",", selectedDays);
        int roommateId = roommates.get(roommateSpinner.getSelectedItemPosition()).id;

        // Insert new chore
        ChoreEntity newChore = new ChoreEntity(choreName, frequency, roommateId, dueDaysCsv);
        db.choreDao().insert(newChore);

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(inputChore.getWindowToken(), 0);

        if (closeAfterSave) {
            setResult(RESULT_OK, new Intent());
            finish();
        } else {
            inputChore.setText("");
            Toast.makeText(this, "Chore added. Add another...", Toast.LENGTH_SHORT).show();
            loadChoreList();
        }
    }

    private void loadRoommates() {
        roommates = db.roommateDao().getAll();
        List<String> names = new ArrayList<>();

        if (roommates.isEmpty()) {
            names.add("No roommates yet");
        } else {
            for (RoommateEntity r : roommates) {
                names.add(TextUtils.isEmpty(r.name) ? "(Unnamed)" : r.name);
            }
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
            choreNames.add(c.name + " • " + c.frequency + " • Due: " + c.dueDays + " • " + roommateName);
        }

        choreListAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                choreNames
        );
        choreListView.setAdapter(choreListAdapter);
    }
}
