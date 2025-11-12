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

        Button continueBtn = findViewById(R.id.continueButton);
        Button cancelBtn = findViewById(R.id.cancelButton);

        continueBtn.setOnClickListener(v -> {
            // For now, just go to Chores List page (we’ll build next)
            Intent intent = new Intent(this, ChoresListActivity.class);
            startActivity(intent);
        });

        cancelBtn.setOnClickListener(v -> finish());
    }
}
