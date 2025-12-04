package com.example.roomies;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.widget.*;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class AddRoommateActivity extends AppCompatActivity {

    public static final String EXTRA_NEW_ROOMMATE_ID = "extra_new_roommate_id";
    public static final String EXTRA_NEW_ROOMMATE_NAME = "extra_new_roommate_name";

    private EditText inputRoommate;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<RoommateEntity> roommates = new ArrayList<>();
    private RoomiesDatabase db;
    private Button saveBtn;

    private LinearLayout undoBar;
    private TextView undoMessage, undoButton;
    private CountDownTimer countDownTimer;
    private RoommateEntity pendingDeleteRoommate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_roomate);

        ImageButton closeButton = findViewById(R.id.closeButtonRoommates);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        db = RoomiesDatabase.getDatabase(this);
        inputRoommate = findViewById(R.id.inputRoommate);
        listView = findViewById(R.id.roommateList);
        saveBtn = findViewById(R.id.saveButton);
        undoBar = findViewById(R.id.undoBar);
        undoMessage = findViewById(R.id.undoMessage);
        undoButton = findViewById(R.id.undoButton);

        // Disable save until text present
        saveBtn.setEnabled(false);

        inputRoommate.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                boolean hasText = s.toString().trim().length() > 0;
                saveBtn.setEnabled(hasText);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        saveBtn.setOnClickListener(v -> addRoommate(true));

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            RoommateEntity r = roommates.get(position);

            // Protect all owned/linked roommates from deletion
            if (r.owned) {
                Toast.makeText(this,
                        "You can't delete a roommate that is linked on a device.",
                        Toast.LENGTH_SHORT
                ).show();
                return true;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Delete roommate?")
                    .setMessage("Remove " + r.name + " from the list?")
                    .setPositiveButton("Delete", (d, w) -> {
                        pendingDeleteRoommate = r;
                        db.roommateDao().delete(r);
                        loadList();
                        SyncUtils.pushIfRoomLinked(this);
                        showUndoBar(r.name);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        undoButton.setOnClickListener(v -> performUndo());

        loadList();
    }

    private void showUndoBar(String roommateName) {
        undoMessage.setText("Deleted: " + roommateName);
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
                pendingDeleteRoommate = null;
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
        if (pendingDeleteRoommate != null) {
            db.roommateDao().insert(pendingDeleteRoommate);
            pendingDeleteRoommate = null;
            hideUndoBar();
            loadList();

            SyncUtils.pushIfRoomLinked(this);
        }
    }

    private void addRoommate(boolean closeAfterSave) {
        String name = inputRoommate.getText().toString().trim();
        if (name.isEmpty()) {
            inputRoommate.setError("Enter name");
            return;
        }
        if (db.roommateDao().countByName(name) > 0) {
            Toast.makeText(this, "Roommate already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        RoommateEntity entity = new RoommateEntity(name);
        long id = db.roommateDao().insert(entity);
        SyncUtils.pushIfRoomLinked(this);

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(inputRoommate.getWindowToken(), 0);

        if (closeAfterSave) {
            Intent data = new Intent();
            data.putExtra(EXTRA_NEW_ROOMMATE_ID, (int) id);
            data.putExtra(EXTRA_NEW_ROOMMATE_NAME, name);
            setResult(RESULT_OK, data);
            finish(); // ← close after save
        } else {
            inputRoommate.setText("");
            inputRoommate.requestFocus();
            Toast.makeText(this, "Saved. Add another…", Toast.LENGTH_SHORT).show();
            loadList();
        }
    }

    private void loadList() {
        roommates = db.roommateDao().getAll();
        List<String> names = new ArrayList<>();
        for (RoommateEntity r : roommates) {
            String label = r.name;
            if (r.owned) {
                label += " (linked)";
            }
            names.add(label);
        }
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        listView.setAdapter(adapter);
    }
}
