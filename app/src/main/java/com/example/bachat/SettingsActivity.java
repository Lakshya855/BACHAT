package com.example.bachat;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchDarkMode;
    private SwitchCompat switchPeakSaver;
    private EditText etTariffRate;
    private MaterialButton btnSaveTariff;
    private TextView chipRate6, chipRate8, chipRate10;

    private TextView tvSettingsStateDiscom;
    private TextView tvSettingsAreaDesc;
    private MaterialButton btnChangeDiscom;

    private SharedPreferences appSettings;
    private SharedPreferences bachatPrefs;
    private Database_users dbUsers;
    private int userId = -1;

    private String currentState = "Delhi";
    private String currentDiscom = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        userId = getIntent().getIntExtra("USER_ID", -1);
        dbUsers = new Database_users(this);

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

        tvSettingsStateDiscom = findViewById(R.id.tvSettingsStateDiscom);
        tvSettingsAreaDesc = findViewById(R.id.tvSettingsAreaDesc);
        btnChangeDiscom = findViewById(R.id.btnChangeDiscom);

        loadProviderInfo();

        if (btnChangeDiscom != null) {
            btnChangeDiscom.setOnClickListener(v -> showChangeDiscomDialog());
        }

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
                Toast.makeText(this, isChecked ? "Peak alerts enabled" : "Peak alerts disabled", Toast.LENGTH_SHORT).show();
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

    private void loadProviderInfo() {
        if (userId > 0) {
            currentState = dbUsers.getUserState(userId);
            currentDiscom = dbUsers.getUserDiscom(userId);
        }
        if (currentState == null || currentState.trim().isEmpty()) {
            currentState = "Delhi";
        }
        if (currentDiscom == null || currentDiscom.trim().isEmpty()) {
            currentDiscom = bachatPrefs.getString("user_discom_" + userId, "");
        }
        DiscomProvider.Discom discom = DiscomProvider.findDiscom(currentState, currentDiscom);
        if (discom != null) {
            if (tvSettingsStateDiscom != null) {
                tvSettingsStateDiscom.setText(discom.state + " · " + discom.name);
            }
            if (tvSettingsAreaDesc != null) {
                tvSettingsAreaDesc.setText("Coverage: " + discom.coverageArea);
            }
        } else {
            if (tvSettingsStateDiscom != null) {
                tvSettingsStateDiscom.setText(currentState + " · Standard Board");
            }
            if (tvSettingsAreaDesc != null) {
                tvSettingsAreaDesc.setText("State-wide coverage");
            }
        }
    }

    private void showChangeDiscomDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_select_discom);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Spinner spinnerState = dialog.findViewById(R.id.spinnerDialogState);
        Spinner spinnerDiscom = dialog.findViewById(R.id.spinnerDialogDiscom);
        TextView tvTariff = dialog.findViewById(R.id.tvDialogTariffPreview);
        TextView tvCoverage = dialog.findViewById(R.id.tvDialogCoveragePreview);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancelDiscomDialog);
        MaterialButton btnSave = dialog.findViewById(R.id.btnSaveDiscomDialog);

        ArrayAdapter<String> stateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, PowerUsageDataset.INDIAN_STATES);
        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerState.setAdapter(stateAdapter);

        int stateIdx = PowerUsageDataset.INDIAN_STATES.indexOf(currentState);
        if (stateIdx >= 0) {
            spinnerState.setSelection(stateIdx);
        }

        final List<DiscomProvider.Discom>[] currentDiscomList = new List[]{ DiscomProvider.getDiscomsForState(currentState) };

        Runnable updateDiscomSpinner = () -> {
            List<String> labels = new ArrayList<>();
            int selectedIdx = 0;
            for (int i = 0; i < currentDiscomList[0].size(); i++) {
                DiscomProvider.Discom d = currentDiscomList[0].get(i);
                labels.add(d.name + " (" + d.coverageArea + ")");
                if (d.name.equalsIgnoreCase(currentDiscom) || currentDiscom.contains(d.name)) {
                    selectedIdx = i;
                }
            }
            ArrayAdapter<String> discomAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
            discomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDiscom.setAdapter(discomAdapter);
            if (selectedIdx < labels.size()) {
                spinnerDiscom.setSelection(selectedIdx);
            }
        };

        updateDiscomSpinner.run();

        spinnerState.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < PowerUsageDataset.INDIAN_STATES.size()) {
                    String state = PowerUsageDataset.INDIAN_STATES.get(position);
                    currentDiscomList[0] = DiscomProvider.getDiscomsForState(state);
                    updateDiscomSpinner.run();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerDiscom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < currentDiscomList[0].size()) {
                    DiscomProvider.Discom d = currentDiscomList[0].get(position);
                    if (tvTariff != null) {
                        tvTariff.setText(String.format(Locale.getDefault(), "Standard Tariff: ₹ %.2f / kWh", d.defaultTariff));
                    }
                    if (tvCoverage != null) {
                        tvCoverage.setText("Coverage: " + d.coverageArea);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            int sPos = spinnerState.getSelectedItemPosition();
            int dPos = spinnerDiscom.getSelectedItemPosition();
            if (sPos >= 0 && sPos < PowerUsageDataset.INDIAN_STATES.size()) {
                currentState = PowerUsageDataset.INDIAN_STATES.get(sPos);
            }
            if (dPos >= 0 && dPos < currentDiscomList[0].size()) {
                DiscomProvider.Discom d = currentDiscomList[0].get(dPos);
                currentDiscom = d.name;

                bachatPrefs.edit()
                        .putFloat("tariff_rate", (float) d.defaultTariff)
                        .putString("user_discom_" + userId, d.name)
                        .apply();

                if (userId > 0) {
                    dbUsers.updateUserStateAndDiscom(userId, currentState, d.name);
                }

                if (etTariffRate != null) {
                    etTariffRate.setText(String.format(Locale.getDefault(), "%.2f", d.defaultTariff));
                }

                loadProviderInfo();
                Toast.makeText(this, "Provider set to " + d.name + " (₹" + String.format(Locale.getDefault(), "%.2f", d.defaultTariff) + "/u)", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }
}
