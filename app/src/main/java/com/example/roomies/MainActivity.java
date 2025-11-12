package com.example.roomies;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int userId = UserManager.getCurrentUser(this);

        if (userId != -1) {
            Intent intent = new Intent(this, ChoresListActivity.class);
            startActivity(intent);
            finish();
        }

        setContentView(R.layout.activity_main);
        setupNav();

    }

    private void setupNav() {
        Button startButton = findViewById(R.id.startButton);

        // When pressed -> open the reminder setup screen
        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReminderSetupActivity.class);
            startActivity(intent);
        });
    }
}
