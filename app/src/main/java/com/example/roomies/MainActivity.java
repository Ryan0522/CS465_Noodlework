package com.example.roomies;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.List;

import android.content.SharedPreferences;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_CREATE_ROOM_FILE = 1001;
    private static final int REQ_JOIN_ROOM_FILE   = 1002;

    private static final String CHANNEL_REMINDERS = "roomies_reminders";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        Log.d("INFO","Entrance");
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean("first_run", true);

        if (isFirstRun) {
            Intent intent = new Intent(this, OnboardingActivity.class);
            startActivity(intent);
        }

        setContentView(R.layout.activity_main);

        createReminderChannel();

        // 1) Pull latest data from shared file if we’re linked
        SyncUtils.pullIfRoomLinkedOnStartup(this);
        rescheduleAllReminders();

        // 2) If we already have a current user, go straight to the chores list
        int userId = UserManager.getCurrentUser(this);
        if (userId != -1) {
            Intent intent = new Intent(this, ChoresListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }

        // 3) New user: show explicit Create / Join buttons
        Button createBtn = findViewById(R.id.btnCreateRoom);
        Button joinBtn = findViewById(R.id.btnJoinRoom);

        createBtn.setOnClickListener(v -> pickLocationToCreateRoomFile());
        joinBtn.setOnClickListener(v -> pickExistingRoomFileToJoin());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001
                );
            }
        }
    }

    private void createReminderChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Chore Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notification for upcoming chores.");
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    private void pickLocationToCreateRoomFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json"); // or "application/json"
        intent.putExtra(Intent.EXTRA_TITLE, "roomies-test.json");
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

            startActivity(new Intent(this, ReminderSetupActivity.class));
        }
    }

    private void rescheduleAllReminders() {
        RoomiesDatabase db = RoomiesDatabase.getDatabase(this);
        List<ReminderEntity> all = db.reminderDao().getAll();
        long now = System.currentTimeMillis();
        for (ReminderEntity r : all) {
            if (r.triggerAtMillis > now) {
                ReminderScheduler.scheduleReminder(this, r);
            }
        }
    }
}