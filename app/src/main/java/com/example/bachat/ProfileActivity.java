package com.example.bachat;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private int userId;
    private Database_users dbUsers;

    private EditText etName;
    private EditText etEmail;
    private EditText etBudget;

    private ImageButton btnEditName;
    private ImageButton btnEditEmail;
    private ImageButton btnEditBudget;
    private MaterialButton btnSave;

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

        btnEditName = findViewById(R.id.btnEditName);
        btnEditEmail = findViewById(R.id.btnEditEmail);
        btnEditBudget = findViewById(R.id.btnEditBudget);
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

        btnSave.setOnClickListener(v -> saveProfileData());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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

        etName.setEnabled(false);
        etEmail.setEnabled(false);
        etBudget.setEnabled(false);
        btnSave.setVisibility(View.GONE);

        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
    }
}
