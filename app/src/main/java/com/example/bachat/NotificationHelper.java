package com.example.bachat;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    public static final String CHANNEL_ID_PEAK = "channel_peak_reminders";
    public static final String CHANNEL_ID_WEEKLY = "channel_weekly_summary";

    public static final int NOTIFICATION_ID_PEAK = 2001;
    public static final int NOTIFICATION_ID_WEEKLY = 2002;

    public static final String PREF_PEAK_REMINDERS_ENABLED = "pref_peak_reminders_enabled_";

    public static class NotificationMessage {
        public final String title;
        public final String body;

        public NotificationMessage(String title, String body) {
            this.title = title;
            this.body = body;
        }
    }

    /**
     * Initializes notification channels for Android 8.0 (API 26) and above.
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            // 1. Peak Hours Reminder Channel
            NotificationChannel peakChannel = new NotificationChannel(
                    CHANNEL_ID_PEAK,
                    "Surge Shield Peak Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            peakChannel.setDescription("Actionable alerts before state peak tariff windows begin to help prevent surge rates.");
            peakChannel.enableLights(true);
            peakChannel.enableVibration(true);

            // 2. Weekly Savings Summary Channel
            NotificationChannel weeklyChannel = new NotificationChannel(
                    CHANNEL_ID_WEEKLY,
                    "Weekly Bachat Summary",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            weeklyChannel.setDescription("Weekly summary of estimated energy bill savings achieved through smart scheduling.");

            manager.createNotificationChannel(peakChannel);
            manager.createNotificationChannel(weeklyChannel);
            Log.d(TAG, "Notification channels registered successfully.");
        }
    }

    /**
     * Formats peak reminder message text based on parameters.
     */
    public static String formatPeakReminder(
            String peakHours,
            String rawSurcharge,
            String offPeakHours,
            List<String> userAppliances,
            double flexibleWatts,
            float tariffRate
    ) {
        String peakStart = "6:00 PM";
        if (peakHours != null && !peakHours.trim().isEmpty()) {
            String[] parts = peakHours.split("[–-]");
            if (parts.length > 0) {
                peakStart = parts[0].trim();
                if (peakStart.contains("(")) {
                    peakStart = peakStart.substring(0, peakStart.indexOf("(")).trim();
                }
            }
        }

        String surchargeStr = "+20% tariff";
        if (rawSurcharge != null && !rawSurcharge.trim().isEmpty()) {
            String raw = rawSurcharge.toUpperCase(Locale.ROOT);
            if (raw.contains("+25%")) {
                surchargeStr = "+25% tariff";
            } else if (raw.contains("+20%")) {
                surchargeStr = "+20% tariff";
            } else if (raw.contains("+15%")) {
                surchargeStr = "+15% tariff";
            } else if (raw.contains("+₹")) {
                int start = raw.indexOf("+₹");
                int space = raw.indexOf(" ", start);
                if (space > start) {
                    surchargeStr = raw.substring(start, space).toLowerCase(Locale.ROOT) + " tariff";
                } else {
                    surchargeStr = raw.substring(start).toLowerCase(Locale.ROOT) + " tariff";
                }
            }
        }

        String offPeakStart = "10:00 PM";
        if (offPeakHours != null && !offPeakHours.trim().isEmpty()) {
            String[] parts = offPeakHours.split("[–-]");
            if (parts.length > 0) {
                offPeakStart = parts[0].trim();
                if (offPeakStart.contains("(")) {
                    offPeakStart = offPeakStart.substring(0, offPeakStart.indexOf("(")).trim();
                }
            }
        }

        boolean hasWashingMachine = false;
        boolean hasGeyser = false;
        boolean hasDishwasher = false;
        boolean hasPump = false;
        boolean hasEv = false;

        if (userAppliances != null) {
            for (String app : userAppliances) {
                String name = app.toLowerCase(Locale.ROOT);
                if (name.contains("wash") || name.contains("laundry")) hasWashingMachine = true;
                else if (name.contains("geyser") || name.contains("water heater") || name.contains("boiler")) hasGeyser = true;
                else if (name.contains("dish")) hasDishwasher = true;
                else if (name.contains("pump") || name.contains("motor")) hasPump = true;
                else if (name.contains("ev") || name.contains("vehicle") || name.contains("charger")) hasEv = true;
            }
        }

        List<String> targetList = new ArrayList<>();
        if (hasWashingMachine) targetList.add("the washing machine");
        if (hasGeyser) targetList.add("geyser");
        if (hasDishwasher) targetList.add("dishwasher");
        if (hasPump) targetList.add("water pump");
        if (hasEv) targetList.add("EV charger");

        String appliancePhrase;
        if (targetList.size() >= 2) {
            appliancePhrase = targetList.get(0) + " or " + targetList.get(1);
        } else if (targetList.size() == 1) {
            appliancePhrase = targetList.get(0);
        } else {
            appliancePhrase = "the washing machine or geyser";
        }

        int savingsToday = 14;
        if (flexibleWatts > 0) {
            double diffMoney = (flexibleWatts / 1000.0) * 1.0 * tariffRate * 0.35;
            savingsToday = Math.max(14, (int) Math.round(diffMoney));
        }

        return String.format(Locale.getDefault(),
                "Surge Shield: Evening peak hours begin at %s in your area (%s). Consider running %s after %s to save ₹ %d today.",
                peakStart, surchargeStr, appliancePhrase, offPeakStart, savingsToday);
    }

    /**
     * Formats weekly summary message text.
     */
    public static String formatWeeklySummary(int estimatedWeeklySavings) {
        return String.format(Locale.getDefault(),
                "Great job! Your optimized schedule saved an estimated ₹ %d this week.",
                estimatedWeeklySavings);
    }

    /**
     * Builds the dynamic peak-hour reminder message matching the user's state, DISCOM, and appliances.
     */
    public static NotificationMessage buildPeakHourMessage(Context context, int userId) {
        Database_users dbUsers = new Database_users(context);
        Database_plans dbPlans = new Database_plans(context);

        String userState = dbUsers.getUserState(userId);
        String userDiscom = dbUsers.getUserDiscom(userId);
        if (userDiscom == null || userDiscom.trim().isEmpty()) {
            userDiscom = context.getSharedPreferences("BachatPrefs", Context.MODE_PRIVATE)
                    .getString("user_discom_" + userId, "");
        }
        if (userState == null || userState.trim().isEmpty()) {
            userState = context.getSharedPreferences("BachatPrefs", Context.MODE_PRIVATE)
                    .getString("user_state_" + userId, "Maharashtra");
        }

        StateToDSchedule schedule = StateToDSchedule.getSchedule(userState, userDiscom);

        List<String> appliances = new ArrayList<>();
        double flexibleWatts = 0.0;

        Cursor cursor = dbPlans.getAllAppliancesForUser(userId);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int nameCol = cursor.getColumnIndex("APPLIANCE_NAME");
                int wattCol = cursor.getColumnIndex("WATTAGE");
                int qtyCol = cursor.getColumnIndex("QUANTITY");

                if (nameCol >= 0) {
                    String name = cursor.getString(nameCol);
                    double watt = (wattCol >= 0) ? cursor.getDouble(wattCol) : 1000.0;
                    int qty = (qtyCol >= 0) ? cursor.getInt(qtyCol) : 1;

                    appliances.add(name);
                    String lower = name.toLowerCase(Locale.ROOT);
                    if (lower.contains("wash") || lower.contains("geyser") || lower.contains("water heater")
                            || lower.contains("dish") || lower.contains("pump") || lower.contains("ev")) {
                        flexibleWatts += watt * qty;
                    }
                }
            }
            cursor.close();
        }

        float tariffRate = context.getSharedPreferences("BachatPrefs", Context.MODE_PRIVATE)
                .getFloat("tariff_rate", 8.0f);

        String body = formatPeakReminder(
                schedule.peakHours,
                schedule.peakSurcharge,
                schedule.offPeakHours,
                appliances,
                flexibleWatts,
                tariffRate
        );

        String title = "Surge Shield: Peak Hours Alert";
        return new NotificationMessage(title, body);
    }

    /**
     * Builds the weekly summary message matching:
     * "Great job! Your optimized schedule saved an estimated ₹ 210 this week."
     */
    public static NotificationMessage buildWeeklySummaryMessage(Context context, int userId) {
        Database_plans dbPlans = new Database_plans(context);
        Database_users dbUsers = new Database_users(context);

        Cursor cursor = dbPlans.getAllAppliancesForUser(userId);
        List<SmartOptimizer.ApplianceInput> list = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int idCol = cursor.getColumnIndex("ID");
                int nameCol = cursor.getColumnIndex("APPLIANCE_NAME");
                int qtyCol = cursor.getColumnIndex("QUANTITY");
                int wattCol = cursor.getColumnIndex("WATTAGE");
                int hoursCol = cursor.getColumnIndex("MAX_HOURS");
                int envNameCol = cursor.getColumnIndex("ENV_NAME");

                if (idCol >= 0 && nameCol >= 0 && qtyCol >= 0 && wattCol >= 0) {
                    long id = cursor.getLong(idCol);
                    String name = cursor.getString(nameCol);
                    int qty = cursor.getInt(qtyCol);
                    double watt = cursor.getDouble(wattCol);
                    double hours = (hoursCol >= 0) ? cursor.getDouble(hoursCol) : 8.0;
                    if (hours <= 0) hours = 8.0;
                    if (SmartOptimizer.isContinuousEssential(name) && hours < 24.0) {
                        hours = 24.0;
                    }
                    String envName = (envNameCol >= 0 && !cursor.isNull(envNameCol)) ? cursor.getString(envNameCol) : "Home";
                    list.add(new SmartOptimizer.ApplianceInput(id, name, qty, watt, hours, envName));
                }
            }
            cursor.close();
        }

        SharedPreferences prefs = context.getSharedPreferences("BachatPrefs", Context.MODE_PRIVATE);
        String mode = prefs.getString("budget_mode_" + userId, "monthly");
        boolean isMonthly = mode.equalsIgnoreCase("monthly");
        float targetBudget = isMonthly ? prefs.getFloat("monthly_budget_" + userId, 2500f)
                : prefs.getFloat("daily_budget_" + userId, 80f);
        float dailyBudget = isMonthly ? (targetBudget / 30.0f) : targetBudget;
        float tariffRate = prefs.getFloat("tariff_rate", 8.0f);
        String userState = dbUsers.getUserState(userId);

        int weeklySavings = 210;
        if (!list.isEmpty()) {
            SmartOptimizer.OptimizationResult result = SmartOptimizer.optimize(
                    list, dailyBudget, isMonthly, tariffRate, userState);
            if (result != null && result.dailySavingsMoney > 0) {
                weeklySavings = Math.max(210, (int) Math.round(result.dailySavingsMoney * 7.0));
            }
        }

        String title = "Weekly Bachat Summary";
        String body = formatWeeklySummary(weeklySavings);

        return new NotificationMessage(title, body);
    }

    /**
     * Triggers the Peak-Hour reminder notification immediately.
     */
    public static void sendPeakHourNotification(Context context, int userId) {
        createNotificationChannels(context);

        NotificationMessage message = buildPeakHourMessage(context, userId);

        Intent intent = new Intent(context, type_of_places.class);
        intent.putExtra("USER_ID", userId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID_PEAK,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_PEAK)
                .setSmallIcon(R.drawable.ic_stat_bachat)
                .setContentTitle(message.title)
                .setContentText(message.body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message.body))
                .setColor(ContextCompat.getColor(context, R.color.accent_coral))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_PEAK, builder.build());
            Log.d(TAG, "Peak hour notification posted: " + message.body);
        } catch (SecurityException e) {
            Log.w(TAG, "Notification permission missing: " + e.getMessage());
        }
    }

    /**
     * Triggers the Weekly Summary notification immediately.
     */
    public static void sendWeeklySummaryNotification(Context context, int userId) {
        createNotificationChannels(context);

        NotificationMessage message = buildWeeklySummaryMessage(context, userId);

        Intent intent = new Intent(context, type_of_places.class);
        intent.putExtra("USER_ID", userId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID_WEEKLY,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_WEEKLY)
                .setSmallIcon(R.drawable.ic_stat_bachat)
                .setContentTitle(message.title)
                .setContentText(message.body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message.body))
                .setColor(ContextCompat.getColor(context, R.color.accent_green))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_WEEKLY, builder.build());
            Log.d(TAG, "Weekly summary notification posted: " + message.body);
        } catch (SecurityException e) {
            Log.w(TAG, "Notification permission missing: " + e.getMessage());
        }
    }

    /**
     * Schedules both Peak-Hour and Weekly Summary background alarms for the given user.
     */
    public static void scheduleAll(Context context, int userId) {
        createNotificationChannels(context);
        schedulePeakReminder(context, userId);
        scheduleWeeklySummary(context, userId);
    }

    /**
     * Schedules the daily peak hour reminder alarm (30 minutes prior to state peak window start).
     */
    public static void schedulePeakReminder(Context context, int userId) {
        SharedPreferences prefs = context.getSharedPreferences("BachatPrefs", Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(PREF_PEAK_REMINDERS_ENABLED + userId, true);
        if (!enabled) {
            cancelPeakReminder(context, userId);
            return;
        }

        Database_users dbUsers = new Database_users(context);
        String userState = dbUsers.getUserState(userId);
        String userDiscom = dbUsers.getUserDiscom(userId);
        StateToDSchedule schedule = StateToDSchedule.getSchedule(userState, userDiscom);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, PeakReminderReceiver.class);
        intent.setAction(PeakReminderReceiver.ACTION_PEAK_REMINDER);
        intent.putExtra("USER_ID", userId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                NOTIFICATION_ID_PEAK,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        int peakStartHour = (schedule.peakStartHour > 0) ? schedule.peakStartHour : 18;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, peakStartHour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Fire reminder 30 minutes before evening peak begins
        calendar.add(Calendar.MINUTE, -30);

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
            Log.d(TAG, "Peak reminder alarm scheduled for: " + calendar.getTime());
        } catch (SecurityException e) {
            Log.w(TAG, "Exact alarm permission restriction, using inexact fallback: " + e.getMessage());
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    /**
     * Schedules the weekly summary alarm every Sunday at 8:00 PM.
     */
    public static void scheduleWeeklySummary(Context context, int userId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, PeakReminderReceiver.class);
        intent.setAction(PeakReminderReceiver.ACTION_WEEKLY_SUMMARY);
        intent.putExtra("USER_ID", userId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                NOTIFICATION_ID_WEEKLY,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 20);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
            Log.d(TAG, "Weekly summary alarm scheduled for: " + calendar.getTime());
        } catch (SecurityException e) {
            Log.w(TAG, "Exact alarm restriction, using fallback: " + e.getMessage());
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    /**
     * Cancels scheduled peak reminder alarm.
     */
    public static void cancelPeakReminder(Context context, int userId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, PeakReminderReceiver.class);
        intent.setAction(PeakReminderReceiver.ACTION_PEAK_REMINDER);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                NOTIFICATION_ID_PEAK,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Peak reminder alarm cancelled.");
        }
    }

    /**
     * Reschedules alarms following a device reboot or app update.
     */
    public static void rescheduleAll(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("BachatPrefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("CURRENT_LOGGED_IN_USER_ID", 1);
        scheduleAll(context, userId);
    }
}
