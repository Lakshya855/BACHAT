package com.example.bachat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class PeakReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_PEAK_REMINDER = "com.example.bachat.ACTION_PEAK_REMINDER";
    public static final String ACTION_WEEKLY_SUMMARY = "com.example.bachat.ACTION_WEEKLY_SUMMARY";

    private static final String TAG = "PeakReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Log.d(TAG, "Broadcast received: " + action);

        int userId = intent.getIntExtra("USER_ID", -1);
        if (userId <= 0) {
            SharedPreferences prefs = context.getSharedPreferences("BachatPrefs", Context.MODE_PRIVATE);
            userId = prefs.getInt("CURRENT_LOGGED_IN_USER_ID", 1);
        }

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Log.d(TAG, "Device booted - rescheduling all Bachat reminders.");
            NotificationHelper.rescheduleAll(context);
        } else if (ACTION_PEAK_REMINDER.equals(action)) {
            Log.d(TAG, "Triggering Peak Hour Notification for user: " + userId);
            NotificationHelper.sendPeakHourNotification(context, userId);
            // Re-arm for next day
            NotificationHelper.schedulePeakReminder(context, userId);
        } else if (ACTION_WEEKLY_SUMMARY.equals(action)) {
            Log.d(TAG, "Triggering Weekly Summary Notification for user: " + userId);
            NotificationHelper.sendWeeklySummaryNotification(context, userId);
            // Re-arm for next week
            NotificationHelper.scheduleWeeklySummary(context, userId);
        }
    }
}
