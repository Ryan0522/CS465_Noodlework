package com.example.roomues;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class EditRoommateActivity extends AppCompatActivity {

    private Spinner roommateSelector;
    private EditText editName;
    private Button saveButton, deleteButton, cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_roommate);

        roommateSelector = findViewById(R.id.roommateSelector);
        editName = findViewById(R.id.editName);
        saveButton = findViewById(R.id.saveButton);
        deleteButton = findViewById(R.id.deleteButton);
        cancelButton = findViewById(R.id.cancelButton);

        RoomiesDatabase db = RoomiesDatabase.getDatabase(this);
        List<RoommateEntity> roommates = db.roommateDao().getAllRoommates();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                roommates.stream().map(RoommateEntity::getName).toArray(String[]::new)
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roommateSelector.setAdapter(adapter);

        roommateSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                editName.setText(roommates.get(position).getName());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        saveButton.setOnClickListener(v -> {
            int pos = roommateSelector.getSelectedItemPosition();
            RoommateEntity selected = roommates.get(pos);
            selected.setName(editName.getText().toString());
            db.roommateDao().update(selected);
            finish();
        });

        deleteButton.setOnClickListener(v -> {
            int pos = roommateSelector.getSelectedItemPosition();
            db.roommateDao().delete(roommates.get(pos));
            finish();
        });

        cancelButton.setOnClickListener(v -> finish());
    }
}
