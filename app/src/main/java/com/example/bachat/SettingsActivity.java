package com.example.bachat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchDarkMode;
    private SwitchCompat switchPeakSaver;
    private EditText etTariffRate;
    private MaterialButton btnSaveTariff;
    private TextView chipRate6, chipRate8, chipRate10;

    private SharedPreferences appSettings;
    private SharedPreferences bachatPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbarSettings);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        appSettings = getSharedPreferences("AppSettings", MODE_PRIVATE);
        bachatPrefs = getSharedPreferences("BachatPrefs", MODE_PRIVATE);

        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchPeakSaver = findViewById(R.id.switchPeakSaver);
        etTariffRate = findViewById(R.id.etTariffRate);
        btnSaveTariff = findViewById(R.id.btnSaveTariff);
        chipRate6 = findViewById(R.id.chipRate6);
        chipRate8 = findViewById(R.id.chipRate8);
        chipRate10 = findViewById(R.id.chipRate10);

        boolean isDarkModeOn = appSettings.getBoolean("isDarkModeOn", true);
        switchDarkMode.setChecked(isDarkModeOn);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appSettings.edit().putBoolean("isDarkModeOn", isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // Peak hours saver toggle
        boolean isPeakSaverOn = bachatPrefs.getBoolean("peak_saver_enabled", true);
        if (switchPeakSaver != null) {
            switchPeakSaver.setChecked(isPeakSaverOn);
            switchPeakSaver.setOnCheckedChangeListener((buttonView, isChecked) -> {
                bachatPrefs.edit().putBoolean("peak_saver_enabled", isChecked).apply();
                Toast.makeText(this, isChecked ? "Peak alerts enabled (6 PM – 10 PM)" : "Peak alerts disabled", Toast.LENGTH_SHORT).show();
            });
        }

        // Electricity tariff rate
        float currentRate = bachatPrefs.getFloat("tariff_rate", 8.0f);
        if (etTariffRate != null) {
            etTariffRate.setText(String.format(Locale.getDefault(), "%.2f", currentRate));
        }

        if (btnSaveTariff != null && etTariffRate != null) {
            btnSaveTariff.setOnClickListener(v -> {
                String input = etTariffRate.getText().toString().trim();
                try {
                    float rate = Float.parseFloat(input);
                    if (rate <= 0) {
                        Toast.makeText(this, "Tariff rate must be positive", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    bachatPrefs.edit().putFloat("tariff_rate", rate).apply();
                    Toast.makeText(this, "Tariff rate updated to ₹" + String.format(Locale.getDefault(), "%.2f", rate) + " / kWh", Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter a valid rate", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (chipRate6 != null && etTariffRate != null) {
            chipRate6.setOnClickListener(v -> etTariffRate.setText("6.50"));
        }
        if (chipRate8 != null && etTariffRate != null) {
            chipRate8.setOnClickListener(v -> etTariffRate.setText("8.00"));
        }
        if (chipRate10 != null && etTariffRate != null) {
            chipRate10.setOnClickListener(v -> etTariffRate.setText("10.50"));
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
