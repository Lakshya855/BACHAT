package com.example.bachat;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class type_of_places extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private int userId;
    private Database_plans dbPlans;
    private Database_users dbUsers;
    private RecyclerView rvEnvironments;
    private TextView tvEmptyState;
    private DrawerLayout drawerLayout;

    // Barometer & Header Views
    private TextView tvBudgetValue;
    private TextView tvBudgetTarget;
    private TextView tvBudgetHeadroom;
    private TextView tvBudgetPercent;
    private TextView tvBudgetStatus;
    private ProgressBar progressBarBudget;
    private View cardBudgetHeader;

    // Feature Chips
    private TextView chipCompare;
    private TextView chipPeakHours;
    private TextView chipAuditReport;

    private ArrayList<EnvironmentAdapter.Environment> envList;
    private EnvironmentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_type_of_places);

        userId = getIntent().getIntExtra("USER_ID", -1);
        dbPlans = new Database_plans(this);
        dbUsers = new Database_users(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Update nav drawer header with user name if available
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null && userId != -1) {
            TextView navHeaderName = headerView.findViewById(R.id.nav_header_name);
            Cursor userCursor = dbUsers.getReadableDatabase().rawQuery("SELECT NAME FROM users WHERE ID=?", new String[]{String.valueOf(userId)});
            if (userCursor != null && userCursor.moveToFirst()) {
                String userName = userCursor.getString(0);
                if (userName != null && !userName.isEmpty()) {
                    navHeaderName.setText(userName);
                }
                userCursor.close();
            }
        }

        rvEnvironments = findViewById(R.id.rvEnvironments);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvBudgetValue = findViewById(R.id.tvBudgetValue);
        tvBudgetTarget = findViewById(R.id.tvBudgetTarget);
        tvBudgetHeadroom = findViewById(R.id.tvBudgetHeadroom);
        tvBudgetPercent = findViewById(R.id.tvBudgetPercent);
        tvBudgetStatus = findViewById(R.id.tvBudgetStatus);
        progressBarBudget = findViewById(R.id.progressBarBudget);
        cardBudgetHeader = findViewById(R.id.cardBudgetHeader);

        chipCompare = findViewById(R.id.chipCompare);
        chipPeakHours = findViewById(R.id.chipPeakHours);
        chipAuditReport = findViewById(R.id.chipAuditReport);

        if (cardBudgetHeader != null) {
            cardBudgetHeader.setOnClickListener(v -> showBudgetDialog());
        }

        if (chipCompare != null) {
            chipCompare.setOnClickListener(v -> {
                Intent intent = new Intent(this, CompareActivity.class);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
            });
        }

        if (chipPeakHours != null) {
            chipPeakHours.setOnClickListener(v -> showPeakHoursSaverDialog());
        }

        if (chipAuditReport != null) {
            chipAuditReport.setOnClickListener(v -> showEnergyAuditDialog());
        }

        updateBudgetHeader();

        envList = new ArrayList<>();
        adapter = new EnvironmentAdapter(envList, selectedPlace -> {
            if (selectedPlace.isAddButton) {
                showAddPlaceDialog();
            } else {
                Intent intent = new Intent(type_of_places.this, ApplicationsActivity.class);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("ENVIRONMENT_ID", (int) selectedPlace.id);
                intent.putExtra("ENVIRONMENT_NAME", selectedPlace.name);
                startActivity(intent);
            }
        });

        rvEnvironments.setLayoutManager(new GridLayoutManager(this, 2));
        rvEnvironments.setAdapter(adapter);

        loadPlaces();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> WindowInsetsCompat.CONSUMED);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBudgetHeader();
        loadPlaces();
    }

    private void updateBudgetHeader() {
        if (tvBudgetValue == null) return;

        SharedPreferences prefs = getSharedPreferences("BachatPrefs", MODE_PRIVATE);
        float budget = prefs.getFloat("monthly_budget_" + userId, 5000f);
        float tariffRate = prefs.getFloat("tariff_rate", 8.0f);

        double totalMonthlyCost = dbPlans.getTotalUserMonthlyCost(userId, tariffRate);

        tvBudgetValue.setText(String.format(Locale.getDefault(), "₹ %,.0f", totalMonthlyCost));

        if (tvBudgetTarget != null) {
            tvBudgetTarget.setText(String.format(Locale.getDefault(), " / ₹ %,.0f target", budget));
        }

        int percent = (budget > 0) ? (int) Math.round((totalMonthlyCost / budget) * 100.0) : 0;

        if (progressBarBudget != null) {
            progressBarBudget.setProgress(Math.min(100, Math.max(0, percent)));
        }

        if (tvBudgetPercent != null) {
            tvBudgetPercent.setText(percent + "% COMMITTED");
        }

        if (totalMonthlyCost > budget) {
            double overage = totalMonthlyCost - budget;
            if (tvBudgetHeadroom != null) {
                tvBudgetHeadroom.setText(String.format(Locale.getDefault(), "Over target by ₹ %,.0f", overage));
                tvBudgetHeadroom.setTextColor(ContextCompat.getColor(this, R.color.accent_coral));
            }
            if (tvBudgetStatus != null) {
                tvBudgetStatus.setText("OVER LIMIT");
                tvBudgetStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_coral));
            }
            if (progressBarBudget != null) {
                progressBarBudget.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.bg_barometer_progress_warning));
            }
        } else {
            double headroom = budget - totalMonthlyCost;
            if (tvBudgetHeadroom != null) {
                tvBudgetHeadroom.setText(String.format(Locale.getDefault(), "On track · ₹ %,.0f savings cushion", headroom));
                tvBudgetHeadroom.setTextColor(ContextCompat.getColor(this, R.color.accent_teal));
            }
            if (tvBudgetStatus != null) {
                tvBudgetStatus.setText("ON TARGET");
                tvBudgetStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
            }
            if (progressBarBudget != null) {
                progressBarBudget.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.bg_barometer_progress));
            }
        }
    }

    private void loadPlaces() {
        envList.clear();
        Cursor cursor = dbPlans.getEnvironments(userId);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int idIndex = cursor.getColumnIndex("ID");
                int nameIndex = cursor.getColumnIndex("NAME");
                if (idIndex != -1 && nameIndex != -1) {
                    envList.add(new EnvironmentAdapter.Environment(cursor.getInt(idIndex), cursor.getString(nameIndex), false));
                }
            }
            cursor.close();
        }

        if (envList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }

        // Add the "Create Space" card at the end
        envList.add(new EnvironmentAdapter.Environment(-1, "Create Space", true));
        adapter.notifyDataSetChanged();
    }

    private void showAddPlaceDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_environment);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.findViewById(R.id.optHome).setOnClickListener(v -> {
            addPlaceToDB("Home");
            dialog.dismiss();
        });

        dialog.findViewById(R.id.optOffice).setOnClickListener(v -> {
            addPlaceToDB("Office");
            dialog.dismiss();
        });

        dialog.findViewById(R.id.optFactory).setOnClickListener(v -> {
            addPlaceToDB("Factory");
            dialog.dismiss();
        });

        dialog.findViewById(R.id.optShop).setOnClickListener(v -> {
            addPlaceToDB("Shop");
            dialog.dismiss();
        });

        dialog.findViewById(R.id.optCustom).setOnClickListener(v -> {
            dialog.dismiss();
            showCustomPlaceDialog();
        });

        dialog.findViewById(R.id.btnCancelAddPlace).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showCustomPlaceDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_custom_place);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText input = dialog.findViewById(R.id.etCustomPlaceName);
        dialog.findViewById(R.id.btnCancelCustomPlace).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnAddCustomPlace).setOnClickListener(v -> {
            String placeName = input.getText().toString().trim();
            if (!placeName.isEmpty()) {
                addPlaceToDB(placeName);
                dialog.dismiss();
            } else {
                Toast.makeText(type_of_places.this, "Please enter a space name", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void addPlaceToDB(String placeName) {
        boolean added = dbPlans.addEnvironment(placeName, userId);
        if (added) {
            savePlaceToCSV(placeName);
            loadPlaces();
            Toast.makeText(type_of_places.this, placeName + " added successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(type_of_places.this, "Environment already exists", Toast.LENGTH_SHORT).show();
        }
    }

    private void savePlaceToCSV(String placeName) {
        try {
            File file = new File(getExternalFilesDir(null), "places.csv");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file, true);
            String safePlaceName = placeName.replace("\"", "\"\"");
            writer.append(String.valueOf(userId)).append(",\"").append(safePlaceName).append("\"\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_budget) {
            showBudgetDialog();
        } else if (id == R.id.nav_compare) {
            Intent intent = new Intent(this, CompareActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        } else if (id == R.id.nav_audit) {
            showEnergyAuditDialog();
        } else if (id == R.id.nav_profile) {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        } else if (id == R.id.nav_report) {
            showReportDialog();
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_feedback) {
            showFeedbackDialog();
        } else if (id == R.id.nav_exit) {
            finishAffinity();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showBudgetDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_set_budget);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText input = dialog.findViewById(R.id.etBudgetAmount);
        float currentBudget = getSharedPreferences("BachatPrefs", MODE_PRIVATE).getFloat("monthly_budget_" + userId, 5000f);
        input.setText(String.valueOf((int) currentBudget));

        dialog.findViewById(R.id.chip2500).setOnClickListener(v -> input.setText("2500"));
        dialog.findViewById(R.id.chip5000).setOnClickListener(v -> input.setText("5000"));
        dialog.findViewById(R.id.chip10000).setOnClickListener(v -> input.setText("10000"));

        dialog.findViewById(R.id.btnCancelBudget).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnSetBudget).setOnClickListener(v -> {
            String b = input.getText().toString().trim();
            if (!b.isEmpty()) {
                try {
                    double budget = Double.parseDouble(b);
                    getSharedPreferences("BachatPrefs", MODE_PRIVATE).edit().putFloat("monthly_budget_" + userId, (float) budget).apply();
                    updateBudgetHeader();
                    Toast.makeText(this, "Monthly ceiling set to ₹" + (int) budget, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } catch (NumberFormatException ignored) {}
            }
        });

        dialog.show();
    }

    private void showPeakHoursSaverDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_peak_saver);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.findViewById(R.id.btnClosePeakSaver).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showEnergyAuditDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_audit_report);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        float tariffRate = getSharedPreferences("BachatPrefs", MODE_PRIVATE).getFloat("tariff_rate", 8.0f);
        double totalSpend = dbPlans.getTotalUserMonthlyCost(userId, tariffRate);
        double totalWattage = dbPlans.getTotalUserWattage(userId);
        String topAppliance = dbPlans.getTopConsumingAppliance(userId);

        double monthlyUnitsKwh = (tariffRate > 0) ? (totalSpend / tariffRate) : 0.0;
        double connectedLoadKw = totalWattage / 1000.0;
        // Average grid carbon intensity in India is ~0.82 kg CO2 per kWh
        double carbonKg = monthlyUnitsKwh * 0.82;
        // Estimated BLDC / 5-star optimization savings (approx 35% of total spend or min ₹300)
        double estimatedSavings = Math.max(0, totalSpend * 0.35);

        TextView tvSpend = dialog.findViewById(R.id.tvAuditMonthlySpend);
        TextView tvUnits = dialog.findViewById(R.id.tvAuditMonthlyUnits);
        TextView tvConnected = dialog.findViewById(R.id.tvAuditConnectedLoad);
        TextView tvTopApp = dialog.findViewById(R.id.tvAuditTopAppliance);
        TextView tvCarbon = dialog.findViewById(R.id.tvAuditCarbonFootprint);
        TextView tvSavings = dialog.findViewById(R.id.tvAuditSavingsEst);

        if (tvSpend != null) tvSpend.setText(String.format(Locale.getDefault(), "₹ %,.2f", totalSpend));
        if (tvUnits != null) tvUnits.setText(String.format(Locale.getDefault(), "%.1f kWh / mo", monthlyUnitsKwh));
        if (tvConnected != null) tvConnected.setText(String.format(Locale.getDefault(), "%.2f kW connected", connectedLoadKw));
        if (tvTopApp != null) tvTopApp.setText(topAppliance.isEmpty() ? "None" : topAppliance);
        if (tvCarbon != null) tvCarbon.setText(String.format(Locale.getDefault(), "%.0f kg CO₂ / mo", carbonKg));
        if (tvSavings != null) tvSavings.setText(String.format(Locale.getDefault(), "Save up to ₹ %,.0f / mo", estimatedSavings));

        dialog.findViewById(R.id.btnCloseAudit).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showReportDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_report_problem);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText inputProblem = dialog.findViewById(R.id.etReportTitle);
        EditText inputDesc = dialog.findViewById(R.id.etReportDesc);

        dialog.findViewById(R.id.btnCancelReport).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnSubmitReport).setOnClickListener(v -> {
            String prob = inputProblem.getText().toString().trim();
            String desc = inputDesc.getText().toString().trim();
            if (prob.isEmpty()) {
                Toast.makeText(this, "Please enter an issue title", Toast.LENGTH_SHORT).show();
                return;
            }
            saveReportToCSV(prob, desc);
            Toast.makeText(type_of_places.this, "Support ticket submitted successfully", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveReportToCSV(String problem, String description) {
        try {
            File file = new File(getExternalFilesDir(null), "reports.csv");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file, true);
            String safeProb = problem.replace("\"", "\"\"");
            String safeDesc = description.replace("\"", "\"\"");
            writer.append(String.valueOf(userId)).append(",\"").append(safeProb).append("\",\"").append(safeDesc).append("\"\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showFeedbackDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_feedback);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        RatingBar ratingBar = dialog.findViewById(R.id.ratingBarFeedback);
        EditText input = dialog.findViewById(R.id.etFeedbackText);

        dialog.findViewById(R.id.btnCancelFeedback).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnSubmitFeedback).setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String feedback = input.getText().toString().trim();
            saveFeedbackToCSV(rating, feedback);
            Toast.makeText(type_of_places.this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveFeedbackToCSV(float rating, String feedback) {
        try {
            File file = new File(getExternalFilesDir(null), "feedback.csv");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file, true);
            String safeFb = feedback.replace("\"", "\"\"");
            writer.append(String.valueOf(userId)).append(",").append(String.valueOf(rating)).append(",\"").append(safeFb).append("\"\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
