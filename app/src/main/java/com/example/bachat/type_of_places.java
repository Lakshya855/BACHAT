package com.example.bachat;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class type_of_places extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private int userId;
    private Database_plans dbPlans;
    private Database_users dbUsers;
    private RecyclerView rvEnvironments;
    private TextView tvEmptyState;
    private DrawerLayout drawerLayout;

    // Barometer & Header Views
    private TextView tvBudgetHeaderTitle;
    private TextView tvBudgetValue;
    private TextView tvBudgetTarget;
    private TextView tvBudgetHeadroom;
    private TextView tvBudgetPercent;
    private TextView tvBudgetStatus;
    private ProgressBar progressBarBudget;
    private View cardBudgetHeader;

    // Feature Chips
    private TextView chipSchedule;
    private TextView chipCompare;
    private TextView chipPeakHours;

    private ArrayList<EnvironmentAdapter.Environment> envList;
    private EnvironmentAdapter adapter;
    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;

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
        toggle.getDrawerArrowDrawable().setColor(ContextCompat.getColor(this, R.color.text_primary));
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
        tvBudgetHeaderTitle = findViewById(R.id.tvBudgetHeaderTitle);
        tvBudgetValue = findViewById(R.id.tvBudgetValue);
        tvBudgetTarget = findViewById(R.id.tvBudgetTarget);
        tvBudgetHeadroom = findViewById(R.id.tvBudgetHeadroom);
        tvBudgetPercent = findViewById(R.id.tvBudgetPercent);
        tvBudgetStatus = findViewById(R.id.tvBudgetStatus);
        progressBarBudget = findViewById(R.id.progressBarBudget);
        cardBudgetHeader = findViewById(R.id.cardBudgetHeader);

        chipSchedule = findViewById(R.id.chipSchedule);
        chipCompare = findViewById(R.id.chipCompare);
        chipPeakHours = findViewById(R.id.chipPeakHours);

        if (cardBudgetHeader != null) {
            cardBudgetHeader.setOnClickListener(v -> showEnergyAuditDialog());
        }

        if (chipSchedule != null) {
            chipSchedule.setOnClickListener(v -> generateAndShowSmartSchedule(false));
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

        requestNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        NotificationHelper.scheduleAll(this, userId);
                    }
                }
        );

        if (userId > 0) {
            getSharedPreferences("BachatPrefs", MODE_PRIVATE)
                    .edit()
                    .putInt("CURRENT_LOGGED_IN_USER_ID", userId)
                    .apply();
            checkNotificationPermission();
            NotificationHelper.scheduleAll(this, userId);
        }

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
        String mode = prefs.getString("budget_mode_" + userId, "monthly");
        float tariffRate = prefs.getFloat("tariff_rate", 8.0f);

        double totalMonthlyCost = dbPlans.getTotalUserMonthlyCost(userId, tariffRate);
        double totalDailyCost = totalMonthlyCost / 30.0;

        View dot = findViewById(R.id.viewBudgetStatusDot);

        if ("daily".equalsIgnoreCase(mode)) {
            float dailyBudget = prefs.getFloat("daily_budget_" + userId, 80f);
            double dailySavings = Math.max(0.0, totalDailyCost - dailyBudget);

            if (tvBudgetHeaderTitle != null) {
                tvBudgetHeaderTitle.setText(String.format(Locale.getDefault(), "DAILY SPENDING TARGET: ₹ %,.0f/DAY", dailyBudget));
            }
            tvBudgetValue.setText(String.format(Locale.getDefault(), "₹ %,.1f", totalDailyCost));
            if (tvBudgetTarget != null) {
                tvBudgetTarget.setText(String.format(Locale.getDefault(), " / ₹ %,.0f limit/day", dailyBudget));
            }

            int percent = (dailyBudget > 0) ? (int) Math.round((totalDailyCost / dailyBudget) * 100.0) : 100;
            if (tvBudgetPercent != null) {
                tvBudgetPercent.setText(String.format(Locale.getDefault(), "TARGET SPEND: ₹ %,.0f/DAY", dailyBudget));
            }

            // Spending is over budget ONLY if strictly greater than dailyBudget by more than 0.05
            if (totalDailyCost > (dailyBudget + 0.05)) {
                if (tvBudgetHeadroom != null) {
                    tvBudgetHeadroom.setText(String.format(Locale.getDefault(), "Run smart hours to save ₹ %,.1f/day and hit ₹ %,.0f limit", dailySavings, dailyBudget));
                    tvBudgetHeadroom.setTextColor(ContextCompat.getColor(this, R.color.accent_coral));
                }
                if (tvBudgetStatus != null) {
                    tvBudgetStatus.setText("TRIM NEEDED");
                    tvBudgetStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_coral));
                }
                if (dot != null) {
                    dot.setBackgroundResource(R.drawable.bg_status_dot_coral);
                }
                if (progressBarBudget != null) {
                    progressBarBudget.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.bg_barometer_progress_warning));
                    progressBarBudget.setProgress(Math.min(100, Math.max(0, percent)));
                }
            } else {
                // Spending is equal or less than budget -> Green line
                if (tvBudgetHeadroom != null) {
                    double cushion = Math.max(0.0, dailyBudget - totalDailyCost);
                    if (cushion > 0.05) {
                        tvBudgetHeadroom.setText(String.format(Locale.getDefault(), "On track · ₹ %,.1f/day headroom", cushion));
                    } else {
                        tvBudgetHeadroom.setText("On track · Within target daily spend");
                    }
                    tvBudgetHeadroom.setTextColor(ContextCompat.getColor(this, R.color.accent_teal));
                }
                if (tvBudgetStatus != null) {
                    tvBudgetStatus.setText("WITHIN BUDGET");
                    tvBudgetStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
                }
                if (dot != null) {
                    dot.setBackgroundResource(R.drawable.bg_status_dot_green);
                }
                if (progressBarBudget != null) {
                    progressBarBudget.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.bg_barometer_progress));
                    progressBarBudget.setProgress(Math.min(100, Math.max(0, percent)));
                }
            }
        } else {
            // Monthly Mode
            float monthlyBudget = prefs.getFloat("monthly_budget_" + userId, 2500f);
            double monthlySavings = Math.max(0.0, totalMonthlyCost - monthlyBudget);

            if (tvBudgetHeaderTitle != null) {
                tvBudgetHeaderTitle.setText(String.format(Locale.getDefault(), "MONTHLY SPENDING TARGET: ₹ %,.0f/MO", monthlyBudget));
            }
            tvBudgetValue.setText(String.format(Locale.getDefault(), "₹ %,.0f", totalMonthlyCost));
            if (tvBudgetTarget != null) {
                tvBudgetTarget.setText(String.format(Locale.getDefault(), " / ₹ %,.0f limit/mo", monthlyBudget));
            }

            int percent = (monthlyBudget > 0) ? (int) Math.round((totalMonthlyCost / monthlyBudget) * 100.0) : 100;
            if (tvBudgetPercent != null) {
                tvBudgetPercent.setText(String.format(Locale.getDefault(), "TARGET SPEND: ₹ %,.0f/MO", monthlyBudget));
            }

            // Spending is over budget ONLY if strictly greater than monthlyBudget by more than 0.5
            if (totalMonthlyCost > (monthlyBudget + 0.5)) {
                if (tvBudgetHeadroom != null) {
                    tvBudgetHeadroom.setText(String.format(Locale.getDefault(), "Run smart hours to save ₹ %,.0f/mo and hit ₹ %,.0f limit", monthlySavings, monthlyBudget));
                    tvBudgetHeadroom.setTextColor(ContextCompat.getColor(this, R.color.accent_coral));
                }
                if (tvBudgetStatus != null) {
                    tvBudgetStatus.setText("TRIM NEEDED");
                    tvBudgetStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_coral));
                }
                if (dot != null) {
                    dot.setBackgroundResource(R.drawable.bg_status_dot_coral);
                }
                if (progressBarBudget != null) {
                    progressBarBudget.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.bg_barometer_progress_warning));
                    progressBarBudget.setProgress(Math.min(100, Math.max(0, percent)));
                }
            } else {
                // Spending is equal or less than budget -> Green line
                if (tvBudgetHeadroom != null) {
                    double cushion = Math.max(0.0, monthlyBudget - totalMonthlyCost);
                    if (cushion > 0.5) {
                        tvBudgetHeadroom.setText(String.format(Locale.getDefault(), "On track · ₹ %,.0f/mo headroom", cushion));
                    } else {
                        tvBudgetHeadroom.setText("On track · Within target monthly spend");
                    }
                    tvBudgetHeadroom.setTextColor(ContextCompat.getColor(this, R.color.accent_teal));
                }
                if (tvBudgetStatus != null) {
                    tvBudgetStatus.setText("WITHIN BUDGET");
                    tvBudgetStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
                }
                if (dot != null) {
                    dot.setBackgroundResource(R.drawable.bg_status_dot_green);
                }
                if (progressBarBudget != null) {
                    progressBarBudget.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.bg_barometer_progress));
                    progressBarBudget.setProgress(Math.min(100, Math.max(0, percent)));
                }
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
        } else if (id == R.id.nav_schedule) {
            generateAndShowSmartSchedule(false);
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
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
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

        SharedPreferences prefs = getSharedPreferences("BachatPrefs", MODE_PRIVATE);
        String initialMode = prefs.getString("budget_mode_" + userId, "monthly");
        final boolean[] isMonthly = new boolean[]{ initialMode.equalsIgnoreCase("monthly") };

        float tariffRate = prefs.getFloat("tariff_rate", 8.0f);
        double currentMonthlyCost = dbPlans.getTotalUserMonthlyCost(userId, tariffRate);
        double currentDailyCost = currentMonthlyCost / 30.0;

        TextView tabDaily = dialog.findViewById(R.id.tabDailyMode);
        TextView tabMonthly = dialog.findViewById(R.id.tabMonthlyMode);
        EditText etAmount = dialog.findViewById(R.id.etBudgetAmount);
        TextView tvUnit = dialog.findViewById(R.id.tvBudgetPeriodUnit);
        TextView tvHint = dialog.findViewById(R.id.tvEquivalentHint);
        TextView chip1 = dialog.findViewById(R.id.chipOption1);
        TextView chip2 = dialog.findViewById(R.id.chipOption2);
        TextView chip3 = dialog.findViewById(R.id.chipOption3);

        Runnable updateUiForMode = () -> {
            if (isMonthly[0]) {
                tabMonthly.setBackgroundResource(R.drawable.bg_chip_selected);
                tabMonthly.setTextColor(ContextCompat.getColor(this, R.color.on_accent_green));
                tabDaily.setBackgroundColor(Color.TRANSPARENT);
                tabDaily.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                tvUnit.setText("/ month");

                chip1.setText("₹ 1,500");
                chip2.setText("₹ 2,500");
                chip3.setText("₹ 3,500");

                float mBudget = prefs.getFloat("monthly_budget_" + userId, 2500f);
                etAmount.setText(String.valueOf((int) mBudget));
                double save = currentMonthlyCost - mBudget;
                if (save > 0) {
                    tvHint.setText(String.format(Locale.getDefault(), "≈ ₹ %,.0f / day spend · Saves ₹ %,.0f / mo vs current ₹ %,.0f spend",
                            mBudget / 30.0, save, currentMonthlyCost));
                } else {
                    tvHint.setText(String.format(Locale.getDefault(), "≈ ₹ %,.0f / day spend · Current spend: ₹ %,.0f/mo",
                            mBudget / 30.0, currentMonthlyCost));
                }
            } else {
                tabDaily.setBackgroundResource(R.drawable.bg_chip_selected);
                tabDaily.setTextColor(ContextCompat.getColor(this, R.color.on_accent_green));
                tabMonthly.setBackgroundColor(Color.TRANSPARENT);
                tabMonthly.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                tvUnit.setText("/ day");

                chip1.setText("₹ 50");
                chip2.setText("₹ 80");
                chip3.setText("₹ 120");

                float dBudget = prefs.getFloat("daily_budget_" + userId, 80f);
                etAmount.setText(String.valueOf((int) dBudget));
                double save = currentDailyCost - dBudget;
                if (save > 0) {
                    tvHint.setText(String.format(Locale.getDefault(), "≈ ₹ %,.0f / month spend · Saves ₹ %,.1f / day vs current ₹ %,.1f spend",
                            dBudget * 30.0, save, currentDailyCost));
                } else {
                    tvHint.setText(String.format(Locale.getDefault(), "≈ ₹ %,.0f / month spend · Current spend: ₹ %,.1f/day",
                            dBudget * 30.0, currentDailyCost));
                }
            }
        };

        updateUiForMode.run();

        tabDaily.setOnClickListener(v -> {
            if (isMonthly[0]) {
                isMonthly[0] = false;
                updateUiForMode.run();
            }
        });

        tabMonthly.setOnClickListener(v -> {
            if (!isMonthly[0]) {
                isMonthly[0] = true;
                updateUiForMode.run();
            }
        });

        chip1.setOnClickListener(v -> etAmount.setText(isMonthly[0] ? "1500" : "50"));
        chip2.setOnClickListener(v -> etAmount.setText(isMonthly[0] ? "2500" : "80"));
        chip3.setOnClickListener(v -> etAmount.setText(isMonthly[0] ? "3500" : "120"));

        etAmount.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                try {
                    String str = s.toString().trim();
                    if (!str.isEmpty()) {
                        double val = Double.parseDouble(str);
                        if (isMonthly[0]) {
                            double save = currentMonthlyCost - val;
                            if (save > 0) {
                                tvHint.setText(String.format(Locale.getDefault(), "≈ ₹ %,.0f / day spend · Saves ₹ %,.0f / mo vs current usage",
                                        val / 30.0, save));
                            } else {
                                tvHint.setText(String.format(Locale.getDefault(), "≈ ₹ %,.0f / day spend · Within current usage (no trim required)",
                                        val / 30.0));
                            }
                        } else {
                            double save = currentDailyCost - val;
                            if (save > 0) {
                                tvHint.setText(String.format(Locale.getDefault(), "≈ ₹ %,.0f / mo spend · Saves ₹ %,.1f / day vs current usage",
                                        val * 30.0, save));
                            } else {
                                tvHint.setText(String.format(Locale.getDefault(), "≈ ₹ %,.0f / mo spend · Within current usage (no trim required)",
                                        val * 30.0));
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        dialog.findViewById(R.id.btnCancelBudget).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnSetBudget).setOnClickListener(v -> {
            String str = etAmount.getText().toString().trim();
            if (!str.isEmpty()) {
                try {
                    double enteredBudget = Double.parseDouble(str);
                    if (enteredBudget <= 0) {
                        Toast.makeText(this, "Target spend must be greater than zero", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    float dBudget, mBudget;
                    String mode = isMonthly[0] ? "monthly" : "daily";

                    if (isMonthly[0]) {
                        mBudget = (float) enteredBudget;
                        dBudget = (float) (enteredBudget / 30.0);
                    } else {
                        dBudget = (float) enteredBudget;
                        mBudget = (float) (enteredBudget * 30.0);
                    }

                    float dSavings = (float) Math.max(0.0, currentDailyCost - dBudget);
                    float mSavings = (float) Math.max(0.0, currentMonthlyCost - mBudget);

                    prefs.edit()
                            .putString("budget_mode_" + userId, mode)
                            .putFloat("daily_budget_" + userId, dBudget)
                            .putFloat("monthly_budget_" + userId, mBudget)
                            .putFloat("daily_savings_" + userId, dSavings)
                            .putFloat("monthly_savings_" + userId, mSavings)
                            .apply();

                    updateBudgetHeader();
                    dialog.dismiss();
                    generateAndShowSmartSchedule(true);

                } catch (NumberFormatException ignored) {}
            }
        });

        dialog.show();
    }

    private void generateAndShowSmartSchedule(boolean afterSettingBudget) {
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

        if (list.isEmpty()) {
            Toast.makeText(this, "Add appliances to your spaces first to generate an optimal schedule!", Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("BachatPrefs", MODE_PRIVATE);
        String mode = prefs.getString("budget_mode_" + userId, "monthly");
        boolean isMonthly = mode.equalsIgnoreCase("monthly");
        float targetBudget = isMonthly ? prefs.getFloat("monthly_budget_" + userId, 2500f)
                : prefs.getFloat("daily_budget_" + userId, 80f);
        float dailyBudget = isMonthly ? (targetBudget / 30.0f) : targetBudget;
        float tariffRate = prefs.getFloat("tariff_rate", 8.0f);
        String userState = dbUsers.getUserState(userId);

        SmartOptimizer.OptimizationResult result = SmartOptimizer.optimize(
                list, dailyBudget, isMonthly, tariffRate, userState);
        showSmartScheduleDialog(result);
    }

    private void showSmartScheduleDialog(SmartOptimizer.OptimizationResult result) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_smart_schedule);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvMoney = dialog.findViewById(R.id.tvMoneySavedHighlight);
        TextView tvEnergy = dialog.findViewById(R.id.tvEnergySavedHighlight);
        TextView tvTargetComp = dialog.findViewById(R.id.tvTargetComparison);
        TextView tvLabel = dialog.findViewById(R.id.tvSavingsLabel);
        RecyclerView rvList = dialog.findViewById(R.id.rvScheduleList);

        if (result.isWithinBudgetAlready) {
            if (tvLabel != null) tvLabel.setText("WITHIN SPENDING TARGET");
            if (tvMoney != null) {
                tvMoney.setText("Within Spend Limit!");
                tvMoney.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
            }
            if (tvEnergy != null) {
                tvEnergy.setText(String.format(Locale.getDefault(), "Safe Headroom: ₹ %,.1f / day", result.dailySavingsMoney));
            }
            if (tvTargetComp != null) {
                if ("daily".equalsIgnoreCase(result.savingsMode)) {
                    tvTargetComp.setText(String.format(Locale.getDefault(), "Limit: ₹ %,.0f / day", result.targetDailyBudget));
                } else {
                    tvTargetComp.setText(String.format(Locale.getDefault(), "Limit: ₹ %,.0f / mo", result.targetMonthlyBudget));
                }
            }
        } else {
            if (tvLabel != null) tvLabel.setText("PROJECTED MONEY SAVED");
            if (tvMoney != null) {
                if ("daily".equalsIgnoreCase(result.savingsMode)) {
                    tvMoney.setText(String.format(Locale.getDefault(), "Save ₹ %,.2f / day", result.dailySavingsMoney));
                } else {
                    tvMoney.setText(String.format(Locale.getDefault(), "Save ₹ %,.0f / month", result.monthlySavingsMoney));
                }
            }
            if (tvEnergy != null) {
                if ("daily".equalsIgnoreCase(result.savingsMode)) {
                    tvEnergy.setText(String.format(Locale.getDefault(), "%.1f kWh power saved / day", result.dailySavingsKwh));
                } else {
                    tvEnergy.setText(String.format(Locale.getDefault(), "%.0f kWh power saved / mo", result.monthlySavingsKwh));
                }
            }
            if (tvTargetComp != null) {
                if ("daily".equalsIgnoreCase(result.savingsMode)) {
                    tvTargetComp.setText(String.format(Locale.getDefault(), "Target Spend: ₹ %,.0f/day", result.targetDailyBudget));
                } else {
                    tvTargetComp.setText(String.format(Locale.getDefault(), "Target Spend: ₹ %,.0f/mo", result.targetMonthlyBudget));
                }
            }
        }

        if (rvList != null) {
            rvList.setLayoutManager(new LinearLayoutManager(this));
            rvList.setAdapter(new SmartScheduleAdapter(result.schedules, "monthly".equalsIgnoreCase(result.savingsMode)));
        }

        dialog.findViewById(R.id.btnDismissSchedule).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnApplySchedule).setOnClickListener(v -> {
            for (SmartOptimizer.ApplianceScheduleItem item : result.schedules) {
                dbPlans.updateAllocatedHours(item.id, item.recommendedHours, item.recommendedDailyCost);
            }
            Toast.makeText(this, "Applied smart hours to " + result.schedules.size() + " devices!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            updateBudgetHeader();
        });

        dialog.show();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void showPeakHoursSaverDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_peak_saver);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        String userState = dbUsers.getUserState(userId);
        String userDiscom = dbUsers.getUserDiscom(userId);
        if (userDiscom == null || userDiscom.isEmpty()) {
            userDiscom = getSharedPreferences("BachatPrefs", MODE_PRIVATE).getString("user_discom_" + userId, "");
        }
        StateToDSchedule schedule = StateToDSchedule.getSchedule(userState, userDiscom);

        TextView tvSubheader = dialog.findViewById(R.id.tvPeakSubheader);
        TextView tvPeakWindow = dialog.findViewById(R.id.tvPeakWindow);
        TextView tvPeakSurcharge = dialog.findViewById(R.id.tvPeakSurcharge);
        TextView tvPeakDesc = dialog.findViewById(R.id.tvPeakDescription);
        TextView tvOffPeakWindow = dialog.findViewById(R.id.tvOffPeakWindow);
        TextView tvOffPeakIncentive = dialog.findViewById(R.id.tvOffPeakIncentive);

        if (tvSubheader != null) {
            tvSubheader.setText(String.format(Locale.getDefault(), "%s · %s",
                    schedule.stateName.toUpperCase(Locale.ROOT),
                    schedule.discomAuthority.toUpperCase(Locale.ROOT)));
        }
        if (tvPeakWindow != null) {
            tvPeakWindow.setText(String.format(Locale.getDefault(), "CRITICAL PEAK: %s", schedule.peakHours));
        }
        if (tvPeakSurcharge != null) {
            tvPeakSurcharge.setText(schedule.peakSurcharge);
        }
        if (tvPeakDesc != null) {
            tvPeakDesc.setText(schedule.summaryNote);
        }
        if (tvOffPeakWindow != null) {
            tvOffPeakWindow.setText(String.format(Locale.getDefault(), "CHEAPEST OFF-PEAK: %s", schedule.offPeakHours));
        }
        if (tvOffPeakIncentive != null) {
            tvOffPeakIncentive.setText(schedule.offPeakIncentive);
        }

        // Push Alert Switch
        SharedPreferences prefs = getSharedPreferences("BachatPrefs", MODE_PRIVATE);
        SwitchMaterial swPeak = dialog.findViewById(R.id.switchPeakReminders);
        boolean remindersEnabled = prefs.getBoolean(NotificationHelper.PREF_PEAK_REMINDERS_ENABLED + userId, true);
        if (swPeak != null) {
            swPeak.setChecked(remindersEnabled);
            swPeak.setOnCheckedChangeListener((btn, isChecked) -> {
                prefs.edit().putBoolean(NotificationHelper.PREF_PEAK_REMINDERS_ENABLED + userId, isChecked).apply();
                if (isChecked) {
                    checkNotificationPermission();
                    NotificationHelper.schedulePeakReminder(type_of_places.this, userId);
                    Toast.makeText(type_of_places.this, "Peak-hour push alerts activated", Toast.LENGTH_SHORT).show();
                } else {
                    NotificationHelper.cancelPeakReminder(type_of_places.this, userId);
                    Toast.makeText(type_of_places.this, "Peak-hour push alerts paused", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Test Peak Notification Button
        View btnTestPeak = dialog.findViewById(R.id.btnTestPeakNotification);
        if (btnTestPeak != null) {
            btnTestPeak.setOnClickListener(v -> {
                checkNotificationPermission();
                NotificationHelper.sendPeakHourNotification(type_of_places.this, userId);
                Toast.makeText(type_of_places.this, "Surge Shield peak alert triggered", Toast.LENGTH_SHORT).show();
            });
        }

        // Test Weekly Summary Notification Button
        View btnTestWeekly = dialog.findViewById(R.id.btnTestWeeklySummary);
        if (btnTestWeekly != null) {
            btnTestWeekly.setOnClickListener(v -> {
                checkNotificationPermission();
                NotificationHelper.sendWeeklySummaryNotification(type_of_places.this, userId);
                Toast.makeText(type_of_places.this, "Weekly summary notification triggered", Toast.LENGTH_SHORT).show();
            });
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

        SharedPreferences prefs = getSharedPreferences("BachatPrefs", MODE_PRIVATE);
        String mode = prefs.getString("budget_mode_" + userId, "monthly");
        boolean isMonthly = mode.equalsIgnoreCase("monthly");
        float targetBudget = isMonthly ? prefs.getFloat("monthly_budget_" + userId, 2500f)
                : prefs.getFloat("daily_budget_" + userId, 80f);
        float dailyBudget = isMonthly ? (targetBudget / 30.0f) : targetBudget;
        float tariffRate = prefs.getFloat("tariff_rate", 8.0f);
        String userState = dbUsers.getUserState(userId);

        SmartOptimizer.OptimizationResult result = SmartOptimizer.optimize(
                list, dailyBudget, isMonthly, tariffRate, userState);

        TextView tvDailySavings = dialog.findViewById(R.id.tvReportDailySavings);
        TextView tvDailyKwh = dialog.findViewById(R.id.tvReportDailyKwh);
        TextView tvMonthlySavings = dialog.findViewById(R.id.tvReportMonthlySavings);
        TextView tvMonthlyKwh = dialog.findViewById(R.id.tvReportMonthlyKwh);
        TextView tvBaselineSpend = dialog.findViewById(R.id.tvReportBaselineSpend);
        TextView tvOptimizedSpend = dialog.findViewById(R.id.tvReportOptimizedSpend);
        TextView tvSavingsPercent = dialog.findViewById(R.id.tvReportSavingsPercent);
        LinearLayout llApplianceContainer = dialog.findViewById(R.id.llReportApplianceContainer);

        if (tvDailySavings != null) {
            if (isMonthly) {
                tvDailySavings.setText(String.format(Locale.getDefault(), "Save ₹ %,.0f / month", result.monthlySavingsMoney));
            } else {
                tvDailySavings.setText(String.format(Locale.getDefault(), "Save ₹ %,.2f / day", result.dailySavingsMoney));
            }
        }
        if (tvDailyKwh != null) {
            if (isMonthly) {
                tvDailyKwh.setText(String.format(Locale.getDefault(), "%.1f kWh saved every month", result.monthlySavingsKwh));
            } else {
                tvDailyKwh.setText(String.format(Locale.getDefault(), "%.2f kWh saved every day", result.dailySavingsKwh));
            }
        }
        if (tvMonthlySavings != null) {
            if (isMonthly) {
                tvMonthlySavings.setText(String.format(Locale.getDefault(), "₹ %,.2f / day saved", result.dailySavingsMoney));
            } else {
                tvMonthlySavings.setText(String.format(Locale.getDefault(), "₹ %,.0f / month saved", result.monthlySavingsMoney));
            }
        }
        if (tvMonthlyKwh != null) {
            if (isMonthly) {
                tvMonthlyKwh.setText(String.format(Locale.getDefault(), "%.2f kWh / day", result.dailySavingsKwh));
            } else {
                tvMonthlyKwh.setText(String.format(Locale.getDefault(), "%.1f kWh / mo", result.monthlySavingsKwh));
            }
        }
        if (tvBaselineSpend != null) {
            if (isMonthly) {
                tvBaselineSpend.setText(String.format(Locale.getDefault(), "₹ %,.0f / mo", result.currentMonthlyCost));
            } else {
                tvBaselineSpend.setText(String.format(Locale.getDefault(), "₹ %,.2f / day", result.currentDailyCost));
            }
        }
        if (tvOptimizedSpend != null) {
            if (isMonthly) {
                tvOptimizedSpend.setText(String.format(Locale.getDefault(), "₹ %,.0f / mo", result.optimizedMonthlyCost));
            } else {
                tvOptimizedSpend.setText(String.format(Locale.getDefault(), "₹ %,.2f / day", result.optimizedDailyCost));
            }
        }
        if (tvSavingsPercent != null) {
            double cutPercent = (result.currentDailyCost > 0)
                    ? (result.dailySavingsMoney / result.currentDailyCost * 100.0)
                    : 0.0;
            tvSavingsPercent.setText(String.format(Locale.getDefault(), "▼ %.1f%% %s Spend Reduction",
                    cutPercent, isMonthly ? "Monthly" : "Daily"));
        }

        if (llApplianceContainer != null) {
            llApplianceContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(this);

            if (result.schedules.isEmpty()) {
                TextView emptyTv = new TextView(this);
                emptyTv.setText("No appliances found in your spaces. Add appliances to generate your savings breakdown.");
                emptyTv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                emptyTv.setTextSize(13);
                emptyTv.setPadding(0, 16, 0, 16);
                llApplianceContainer.addView(emptyTv);
            } else {
                for (SmartOptimizer.ApplianceScheduleItem item : result.schedules) {
                    View itemView = inflater.inflate(R.layout.item_report_appliance, llApplianceContainer, false);
                    TextView tvName = itemView.findViewById(R.id.tvReportAppName);
                    TextView tvRuntime = itemView.findViewById(R.id.tvReportAppRuntime);
                    TextView tvDailySav = itemView.findViewById(R.id.tvReportAppDailySavings);
                    TextView tvDailyKwhItem = itemView.findViewById(R.id.tvReportAppDailyKwh);

                    String loc = (item.environmentName != null && !item.environmentName.isEmpty())
                            ? " · " + item.environmentName : "";
                    if (tvName != null) {
                        tvName.setText(String.format(Locale.getDefault(), "%s%s", item.name, loc));
                    }

                    if (tvRuntime != null) {
                        if (item.dailyCostSavings > 0.001) {
                            double trimmed = item.originalHours - item.allocatedHours;
                            tvRuntime.setText(String.format(Locale.getDefault(), "Should run %.1f hrs/day (Trimmed by %.1fh)",
                                    item.allocatedHours, trimmed));
                        } else {
                            tvRuntime.setText(String.format(Locale.getDefault(), "Should run %.1f hrs/day (Full runtime)",
                                    item.allocatedHours));
                        }
                    }

                    if (tvDailySav != null) {
                        if (isMonthly) {
                            if (item.monthlyCostSavings > 0.5) {
                                tvDailySav.setText(String.format(Locale.getDefault(), "+ ₹ %,.0f / mo", item.monthlyCostSavings));
                                tvDailySav.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
                            } else {
                                tvDailySav.setText("₹ 0 / mo");
                                tvDailySav.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                            }
                        } else {
                            if (item.dailyCostSavings > 0.001) {
                                tvDailySav.setText(String.format(Locale.getDefault(), "+ ₹ %,.2f / day", item.dailyCostSavings));
                                tvDailySav.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
                            } else {
                                tvDailySav.setText("₹ 0.00 / day");
                                tvDailySav.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                            }
                        }
                    }

                    if (tvDailyKwhItem != null) {
                        if (isMonthly) {
                            if (item.monthlyKwhSavings > 0.05) {
                                tvDailyKwhItem.setText(String.format(Locale.getDefault(), "%.1f kWh / mo", item.monthlyKwhSavings));
                            } else {
                                tvDailyKwhItem.setText("0.0 kWh trimmed");
                            }
                        } else {
                            if (item.dailyKwhSavings > 0.001) {
                                tvDailyKwhItem.setText(String.format(Locale.getDefault(), "%.2f kWh / day", item.dailyKwhSavings));
                            } else {
                                tvDailyKwhItem.setText("0.0 kWh trimmed");
                            }
                        }
                    }

                    llApplianceContainer.addView(itemView);
                }
            }
        }

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
