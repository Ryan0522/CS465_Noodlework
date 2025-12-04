package com.example.roomies;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int reminderId = intent.getIntExtra("reminderId", -1);
        String message = intent.getStringExtra("message");
        Log.d("ReminderDebug", "NotificationReceiver triggered for reminderId=" + reminderId + " message=" + message);

        NotificationHelper.sendNotification(context, reminderId, "Chore Reminder", message);
    }
}
