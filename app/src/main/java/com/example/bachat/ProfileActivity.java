package com.example.bachat;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private int userId;
    private Database_users dbUsers;

    private EditText etName;
    private EditText etEmail;
    private EditText etBudget;
    private TextView tvProfileState;
    private TextView tvProfileDiscom;

    private ImageButton btnEditName;
    private ImageButton btnEditEmail;
    private ImageButton btnEditBudget;
    private ImageButton btnEditProvider;
    private LinearLayout layoutEditProvider;
    private Spinner spinnerProfileState;
    private Spinner spinnerProfileDiscom;
    private MaterialButton btnSave;

    private String selectedState = "Delhi";
    private String selectedDiscom = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        userId = getIntent().getIntExtra("USER_ID", -1);
        dbUsers = new Database_users(this);

        Toolbar toolbar = findViewById(R.id.toolbarProfile);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        etName = findViewById(R.id.etProfileName);
        etEmail = findViewById(R.id.etProfileEmail);
        etBudget = findViewById(R.id.etProfileBudget);
        tvProfileState = findViewById(R.id.tvProfileState);
        tvProfileDiscom = findViewById(R.id.tvProfileDiscom);

        btnEditName = findViewById(R.id.btnEditName);
        btnEditEmail = findViewById(R.id.btnEditEmail);
        btnEditBudget = findViewById(R.id.btnEditBudget);
        btnEditProvider = findViewById(R.id.btnEditProvider);
        layoutEditProvider = findViewById(R.id.layoutEditProvider);
        spinnerProfileState = findViewById(R.id.spinnerProfileState);
        spinnerProfileDiscom = findViewById(R.id.spinnerProfileDiscom);
        btnSave = findViewById(R.id.btnSaveProfile);

        loadProfileData();

        btnEditName.setOnClickListener(v -> {
            etName.setEnabled(true);
            etName.requestFocus();
            etName.setSelection(etName.getText().length());
            btnSave.setVisibility(View.VISIBLE);
        });

        btnEditEmail.setOnClickListener(v -> {
            etEmail.setEnabled(true);
            etEmail.requestFocus();
            etEmail.setSelection(etEmail.getText().length());
            btnSave.setVisibility(View.VISIBLE);
        });

        btnEditBudget.setOnClickListener(v -> {
            etBudget.setEnabled(true);
            etBudget.requestFocus();
            etBudget.setSelection(etBudget.getText().length());
            btnSave.setVisibility(View.VISIBLE);
        });

        btnEditProvider.setOnClickListener(v -> {
            layoutEditProvider.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.VISIBLE);
            initProviderSpinners();
        });

        btnSave.setOnClickListener(v -> saveProfileData());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initProviderSpinners() {
        ArrayAdapter<String> stateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, PowerUsageDataset.INDIAN_STATES);
        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProfileState.setAdapter(stateAdapter);

        int stateIdx = PowerUsageDataset.INDIAN_STATES.indexOf(selectedState);
        if (stateIdx >= 0) {
            spinnerProfileState.setSelection(stateIdx);
        }

        updateDiscomList(selectedState);

        spinnerProfileState.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < PowerUsageDataset.INDIAN_STATES.size()) {
                    selectedState = PowerUsageDataset.INDIAN_STATES.get(position);
                    updateDiscomList(selectedState);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateDiscomList(String state) {
        List<DiscomProvider.Discom> discoms = DiscomProvider.getDiscomsForState(state);
        List<String> labels = new ArrayList<>();
        int selectIdx = 0;
        for (int i = 0; i < discoms.size(); i++) {
            DiscomProvider.Discom d = discoms.get(i);
            labels.add(d.name + " (" + d.coverageArea + ")");
            if (d.name.equalsIgnoreCase(selectedDiscom) || selectedDiscom.contains(d.name)) {
                selectIdx = i;
            }
        }
        ArrayAdapter<String> discomAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        discomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProfileDiscom.setAdapter(discomAdapter);
        if (selectIdx < labels.size()) {
            spinnerProfileDiscom.setSelection(selectIdx);
        }
    }

    private void loadProfileData() {
        Cursor cursor = dbUsers.getReadableDatabase().rawQuery("SELECT NAME, EMAIL FROM users WHERE ID=?", new String[]{String.valueOf(userId)});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                etName.setText(cursor.getString(0));
                etEmail.setText(cursor.getString(1));
            }
            cursor.close();
        }

        selectedState = dbUsers.getUserState(userId);
        if (selectedState == null || selectedState.trim().isEmpty()) {
            selectedState = "Delhi";
        }

        selectedDiscom = dbUsers.getUserDiscom(userId);
        if (selectedDiscom == null || selectedDiscom.trim().isEmpty()) {
            selectedDiscom = getSharedPreferences("BachatPrefs", MODE_PRIVATE).getString("user_discom_" + userId, "");
        }
        if (selectedDiscom.isEmpty()) {
            List<DiscomProvider.Discom> discoms = DiscomProvider.getDiscomsForState(selectedState);
            if (!discoms.isEmpty()) {
                selectedDiscom = discoms.get(0).name;
            }
        }

        tvProfileState.setText("State: " + selectedState);
        tvProfileDiscom.setText("Provider: " + selectedDiscom);

        float budget = getSharedPreferences("BachatPrefs", MODE_PRIVATE).getFloat("monthly_budget_" + userId, 5000f);
        etBudget.setText(String.format(Locale.getDefault(), "%.0f", budget));
    }

    private void saveProfileData() {
        String newName = etName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        String newBudgetStr = etBudget.getText().toString().trim();

        if (newName.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "Name and Email cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put("NAME", newName);
        values.put("EMAIL", newEmail);
        dbUsers.getWritableDatabase().update("users", values, "ID=?", new String[]{String.valueOf(userId)});

        try {
            float budget = Float.parseFloat(newBudgetStr);
            getSharedPreferences("BachatPrefs", MODE_PRIVATE).edit().putFloat("monthly_budget_" + userId, budget).apply();
        } catch (NumberFormatException ignored) {}

        if (layoutEditProvider != null && layoutEditProvider.getVisibility() == View.VISIBLE) {
            if (spinnerProfileState.getSelectedItem() != null) {
                selectedState = spinnerProfileState.getSelectedItem().toString();
            }
            String discomLabel = (spinnerProfileDiscom.getSelectedItem() != null) ? spinnerProfileDiscom.getSelectedItem().toString() : "";
            DiscomProvider.Discom discomObj = DiscomProvider.findDiscom(selectedState, discomLabel);
            if (discomObj != null) {
                selectedDiscom = discomObj.name;
                getSharedPreferences("BachatPrefs", MODE_PRIVATE).edit()
                        .putFloat("tariff_rate", (float) discomObj.defaultTariff)
                        .putString("user_discom_" + userId, discomObj.name)
                        .apply();
            }
            dbUsers.updateUserStateAndDiscom(userId, selectedState, selectedDiscom);
            tvProfileState.setText("State: " + selectedState);
            tvProfileDiscom.setText("Provider: " + selectedDiscom);
            layoutEditProvider.setVisibility(View.GONE);
        }

        etName.setEnabled(false);
        etEmail.setEnabled(false);
        etBudget.setEnabled(false);
        btnSave.setVisibility(View.GONE);

        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
    }
}
