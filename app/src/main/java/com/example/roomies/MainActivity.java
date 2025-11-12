package com.example.roomies;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startBtn = findViewById(R.id.startButton);
        startBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReminderSetupActivity.class);
            startActivity(intent);
        });

        int userId = UserManager.getCurrentUser(this);
        if (userId != -1) {
            Intent intent = new Intent(this, ChoresListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }
}
