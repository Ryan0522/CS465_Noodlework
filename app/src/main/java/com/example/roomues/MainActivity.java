package com.example.roomues;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startButton = findViewById(R.id.startButton);

        // When pressed -> open the reminder setup screen
        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReminderSetupActivity.class);
            startActivity(intent);
        });
    }
}
