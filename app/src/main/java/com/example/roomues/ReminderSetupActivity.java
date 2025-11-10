package com.example.roomues;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;

public class ReminderSetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_screen);

        Button continueButton = findViewById(R.id.continueButton);
        Button cancelButton = findViewById(R.id.cancelButton);
        RadioGroup yesNoGroup = findViewById(R.id.yesNoGroup);

        continueButton.setOnClickListener(v -> {
            // For now, just go to Chores List page (we’ll build next)
            Intent intent = new Intent(ReminderSetupActivity.this, ChoresListActivity.class);
            startActivity(intent);
        });

        cancelButton.setOnClickListener(v -> finish());
    }
}
