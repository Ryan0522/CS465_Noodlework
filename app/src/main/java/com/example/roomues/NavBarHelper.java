package com.example.roomues;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

public class NavBarHelper {

    public static void setupBottomNav(Activity activity, String currentScreen) {
        // Home
        View homeContainer = activity.findViewById(R.id.navHomeContainer);
        View scheduleContainer = activity.findViewById(R.id.navScheduleContainer);
        View remindersContainer = activity.findViewById(R.id.navRemindersContainer);
        View settingsContainer = activity.findViewById(R.id.navSettingsContainer);

        // Image + text color change for active screen
        setHighlight(activity, currentScreen);

        homeContainer.setOnClickListener(v -> {
            if (!currentScreen.equals("home")) {
                activity.startActivity(new Intent(activity, ChoresListActivity.class));
                activity.overridePendingTransition(0, 0);
            }
        });

        scheduleContainer.setOnClickListener(v -> {
            if (!currentScreen.equals("schedule")) {
                activity.startActivity(new Intent(activity, ScheduleViewActivity.class));
                activity.overridePendingTransition(0, 0);
            }
        });

        remindersContainer.setOnClickListener(v -> {
            if (!currentScreen.equals("reminder")) {
                activity.startActivity(new Intent(activity, ReminderSetupActivity.class));
                activity.overridePendingTransition(0, 0);
            }
        });

        settingsContainer.setOnClickListener(v -> {
            if (!currentScreen.equals("settings")) {
                activity.startActivity(new Intent(activity, ReminderSetupActivity.class));
                activity.overridePendingTransition(0, 0);
            }
        });
    }

    private static void setHighlight(Activity activity, String screen) {
        highlight(activity, R.id.navHome, R.id.navHomeLabel, screen.equals("home"));
        highlight(activity, R.id.navSchedule, R.id.navScheduleLabel, screen.equals("schedule"));
        highlight(activity, R.id.navReminders, R.id.navRemindersLabel, screen.equals("reminder"));
        highlight(activity, R.id.navSettings, R.id.navSettingsLabel, screen.equals("settings"));
    }

    private static void highlight(Activity activity, int iconId, int labelId, boolean active) {
        ImageView icon = activity.findViewById(iconId);
        TextView label = activity.findViewById(labelId);
        if (icon == null || label == null) return;

        int color = ContextCompat.getColor(activity,
                active ? R.color.nav_selected : R.color.nav_unselected);

        // tint icon + set label color
        icon.setColorFilter(color);
        label.setTextColor(color);
    }
}