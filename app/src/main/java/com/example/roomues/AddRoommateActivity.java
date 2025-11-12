package com.example.roomues;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import java.util.List;
import java.util.ArrayList;

public class AddRoommateActivity extends AppCompatActivity {

    private EditText inputRoommate;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<RoommateEntity> roommates = new ArrayList<>();
    private RoomiesDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_roomate);

        db = RoomiesDatabase.getDatabase(this);
        inputRoommate = findViewById(R.id.inputRoommate);
        listView = findViewById(R.id.roommateList);

        Button saveBtn = findViewById(R.id.saveButton);
        Button cancelBtn = findViewById(R.id.cancelButton);

        saveBtn.setOnClickListener(v -> addRoommate());
        cancelBtn.setOnClickListener(v -> finish());;

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            RoommateEntity r = roommates.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Delete roommate?")
                    .setMessage("Remove " + r.name + " from the list?")
                    .setPositiveButton("Delete", (d, w) -> {
                        db.roommateDao().delete(r);
                        loadList();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        loadList();
    }

    private void addRoommate() {
        String name = inputRoommate.getText().toString().trim();
        if (name.isEmpty()) {
            inputRoommate.setError("Enter name");
            return;
        }
        db.roommateDao().insert(new RoommateEntity(name));
        inputRoommate.setText("");
        loadList();
    }

    private void loadList() {
        roommates = db.roommateDao().getAll();
        List<String> names = new ArrayList<>();
        for (RoommateEntity r : roommates) names.add(r.name);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        listView.setAdapter(adapter);
    }
}
