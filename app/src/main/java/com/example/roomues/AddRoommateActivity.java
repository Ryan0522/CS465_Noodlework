package com.example.roomues;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class AddRoommateActivity extends AppCompatActivity {

    private EditText inputRoommate;
    private Button saveButton, cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_roomate);
        inputRoommate = findViewById(R.id.inputRoommate);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        // Populate spinner

        // Cancel -> close
        cancelButton.setOnClickListener(v -> finish());

        // Save -> (for now) return to ChoresListActivity
        saveButton.setOnClickListener(v -> {
            String roommateName = inputRoommate.getText().toString().trim();
            if (!roommateName.isEmpty()) {
                RoommateEntity newRoommate = new RoommateEntity(roommateName);
                RoomiesDatabase db = RoomiesDatabase.getDatabase(this);
                db.roommateDao().insert(newRoommate);
            }
            finish();
        });

    }
}
