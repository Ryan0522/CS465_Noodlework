package com.example.roomies;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ReminderSetupActivity extends AppCompatActivity {
    private EditText inputName, inputUrl;
    private Button confirmationBtn, urlConfirmBtn;
    private RoommateDao roommateDao;
    private int currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_screen);

        inputName = findViewById(R.id.inputName);
        inputUrl = findViewById(R.id.inputUrl);
        confirmationBtn = findViewById(R.id.confirmNameButton);
        urlConfirmBtn = findViewById(R.id.urlConfirmButton);
        Button continueBtn = findViewById(R.id.continueButton);
        Button cancelBtn = findViewById(R.id.cancelButton);

        NavBarHelper.setupBottomNav(this, "settings");

        // access DAO
        RoomiesDatabase db = RoomiesDatabase.getDatabase(this);
        roommateDao = db.roommateDao();

        // NEW: prefill if a user is already set, and lock the field
        int savedId = UserManager.getCurrentUser(this);
        if (savedId != -1) {
            RoommateEntity me = roommateDao.getById(savedId);
            if (me != null) {
                inputName.setText(me.name);
                inputName.setEnabled(false);
                confirmationBtn.setEnabled(false);
                confirmationBtn.setBackgroundColor(getResources().getColor(R.color.gray));
            }
        }

        // --- PREFILL Existing URL ---
        String savedUrl = UserManager.getSharedUrl(this);
        if (savedUrl == null) savedUrl = "";
        if (!savedUrl.isEmpty()) {
            inputUrl.setText(savedUrl);
            inputUrl.setEnabled(false);
            urlConfirmBtn.setEnabled(false);
            urlConfirmBtn.setBackgroundColor(getResources().getColor(R.color.gray));
        }

        // --- Button Actions ---
        confirmationBtn.setOnClickListener(v -> handleNameConfirm());
        urlConfirmBtn.setOnClickListener(v -> handleUrlConfirm());

        continueBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChoresListActivity.class);
            startActivity(intent);
        });
        cancelBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChoresListActivity.class);
            startActivity(intent);
        });
    }

    private void handleNameConfirm() {
        String name = inputName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        // check if roommate already exists
        RoommateEntity existing = roommateDao.getRoommateByName(name);
        if (existing != null) {
            currentUserId = existing.id;
            Toast.makeText(this, "Welcome back, " + name + "!", Toast.LENGTH_SHORT).show();
        } else {
            // create new roommate
            RoommateEntity newRoommate = new RoommateEntity(name);
            roommateDao.insert(newRoommate);
            RoommateEntity inserted = roommateDao.getRoommateByName(name);
            currentUserId = inserted.id;
            Toast.makeText(this, "New roommate added: " + name, Toast.LENGTH_SHORT).show();
        }

        UserManager.setCurrentUser(this, currentUserId);

        inputName.setText(name);
        inputName.setEnabled(false);
        confirmationBtn.setEnabled(false);
    }

    private void handleUrlConfirm() {
        String url = inputUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save in UserManager
        UserManager.setSharedUrl(this, url);
        Toast.makeText(this, "URL saved!", Toast.LENGTH_SHORT).show();

        inputUrl.setEnabled(false);
        urlConfirmBtn.setEnabled(false);
        urlConfirmBtn.setBackgroundColor(getResources().getColor(R.color.gray));
    }
}
