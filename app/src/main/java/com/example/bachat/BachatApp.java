package com.example.bachat;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class BachatApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences appSettings = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isDarkModeOn = appSettings.getBoolean("isDarkModeOn", true);
        if (isDarkModeOn) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
