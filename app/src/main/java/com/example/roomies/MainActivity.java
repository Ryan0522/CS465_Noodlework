package com.example.roomies;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_CREATE_ROOM_FILE = 1001;
    private static final int REQ_JOIN_ROOM_FILE   = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1) Pull latest data from shared file if we’re linked
        SyncUtils.pullIfRoomLinkedOnStartup(this);

        Button startBtn = findViewById(R.id.startButton);
        startBtn.setOnClickListener(v -> onStartClicked());

        // 2) If we already have a current user, skip splash & go straight to chores
        int userId = UserManager.getCurrentUser(this);
        if (userId != -1) {
            Intent intent = new Intent(this, ChoresListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void onStartClicked() {
        int userId = UserManager.getCurrentUser(this);

        if (userId != -1) {
            // Existing user: just go to chores list
            Intent intent = new Intent(this, ChoresListActivity.class);
            startActivity(intent);
            return;
        }

        // New user: ask if they want to create or join a shared room
        showRoomChoiceDialog();
    }

    private void showRoomChoiceDialog() {
        CharSequence[] options = new CharSequence[] {
                "Create new shared room",
                "Join existing shared room",
                "Skip (this device only)"
        };

        new AlertDialog.Builder(this)
                .setTitle("Create or Join Room")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            pickLocationToCreateRoomFile();
                            break;
                        case 1:
                            pickExistingRoomFileToJoin();
                            break;
                        case 2:
                        default:
                            // No sync: go into normal onboarding (ReminderSetup)
                            startActivity(new Intent(this, ReminderSetupActivity.class));
                            break;
                    }
                })
                .show();
    }

    private void pickLocationToCreateRoomFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream"); // or "application/json"
        intent.putExtra(Intent.EXTRA_TITLE, "roomies-room.db");
        startActivityForResult(intent, REQ_CREATE_ROOM_FILE);
    }

    private void pickExistingRoomFileToJoin() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // or a more specific type if you want
        startActivityForResult(intent, REQ_JOIN_ROOM_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        Uri uri = data.getData();
        if (uri == null) return;

        if (requestCode == REQ_CREATE_ROOM_FILE) {
            // First device: we create a shared file & link it.
            SyncUtils.linkAndPushNewRoomFile(this, uri);

            // Then go into normal onboarding (user name, reminders, etc.)
            startActivity(new Intent(this, ReminderSetupActivity.class));

        } else if (requestCode == REQ_JOIN_ROOM_FILE) {
            // Joining an existing shared DB
            SyncUtils.linkAndPullExistingRoomFile(this, uri);

            // After pulling, we should now see the existing roommates/chores.
            // If a current user is already stored, jump to chores.
            int userId = UserManager.getCurrentUser(this);
            if (userId != -1) {
                Intent intent = new Intent(this, ChoresListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                // No current user yet: go through ReminderSetup to pick yourself.
                startActivity(new Intent(this, ReminderSetupActivity.class));
            }
        }
    }
}