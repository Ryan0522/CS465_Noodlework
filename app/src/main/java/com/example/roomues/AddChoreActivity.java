package com.example.roomues;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class AddChoreActivity extends AppCompatActivity {

    private EditText inputChore;
    private Spinner frequencySpinner, roommateSpinner;
    private Button saveButton, cancelButton;
    private List<RoommateEntity> roommatesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_chore);

        inputChore = findViewById(R.id.inputChore);
        frequencySpinner = findViewById(R.id.frequencySpinner);
        roommateSpinner = findViewById(R.id.roommateSpinner);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        // Frequency dropdown
        String[] frequencies = {"Daily", "Weekly", "Monthly"};
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, frequencies);
        frequencySpinner.setAdapter(freqAdapter);

        // 🔹 Load roommates from DB
        RoomiesDatabase db = RoomiesDatabase.getDatabase(this);
        roommatesList = db.roommateDao().getAllRoommates();
        List<String> roommateNames = new ArrayList<>();
        for (RoommateEntity r : roommatesList) {
            roommateNames.add(r.getName());
        }

        ArrayAdapter<String> roommateAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, roommateNames);
        roommateSpinner.setAdapter(roommateAdapter);

        cancelButton.setOnClickListener(v -> finish());

        saveButton.setOnClickListener(v -> {
            String choreName = inputChore.getText().toString().trim();
            String frequency = frequencySpinner.getSelectedItem().toString();

            if (!choreName.isEmpty() && !roommatesList.isEmpty()) {
                int selectedIndex = roommateSpinner.getSelectedItemPosition();
                int roommateId = roommatesList.get(selectedIndex).getId();

                ChoreEntity newChore = new ChoreEntity(choreName, frequency, roommateId);
                db.choreDao().insert(newChore);
            }
            finish();
        });
    }
}
