package com.example.bachat;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ApplicationsActivity extends AppCompatActivity {

    private int userId;
    private int envId;
    private String envName;
    private String state;

    private Database_plans dbPlans;
    private Database_users dbUsers;

    private RecyclerView rvAppliances;
    private MaterialButton buttonAddAppliance;
    private ImageButton btnVoiceInput;
    private ImageButton btnBack;
    private TextView tvTitle;
    private TextView tvEmptyState;
    private TextView tvTotalWattage;
    private TextView tvTotalCost;

    private ArrayList<ApplianceAdapter.ApplianceItem> appList;
    private ApplianceAdapter adapter;

    private long currentPlanId = -1;

    private ActivityResultLauncher<Intent> voiceResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_applications);

        userId = getIntent().getIntExtra("USER_ID", -1);
        envId = getIntent().getIntExtra("ENVIRONMENT_ID", -1);
        envName = getIntent().getStringExtra("ENVIRONMENT_NAME");
        if (envName == null) envName = "Space";

        dbPlans = new Database_plans(this);
        dbUsers = new Database_users(this);
        state = dbUsers.getUserState(userId);

        rvAppliances = findViewById(R.id.rvAppliances);
        buttonAddAppliance = findViewById(R.id.buttonAddAppliance);
        btnVoiceInput = findViewById(R.id.btnVoiceInput);
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvTotalWattage = findViewById(R.id.tvTotalWattage);
        tvTotalCost = findViewById(R.id.tvTotalCost);

        tvTitle.setText(envName + " Appliances");

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        appList = new ArrayList<>();
        adapter = new ApplianceAdapter(appList, this::showApplianceDetailsDialog, (item, newQty) -> {
            dbPlans.updateApplianceDetails(item.id, newQty, item.wattage, item.hoursPerDay);
            loadApplications();
        });

        rvAppliances.setLayoutManager(new GridLayoutManager(this, 2));
        rvAppliances.setAdapter(adapter);

        ensurePlanExists();
        loadApplications();

        buttonAddAppliance.setOnClickListener(v -> showAddApplicationDialog());

        voiceResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0);
                    processVoiceInput(spokenText);
                }
            }
        });

        btnVoiceInput.setOnClickListener(v -> startVoiceRecognition());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApplications();
    }

    private void ensurePlanExists() {
        Cursor cursor = dbPlans.getPlansForUser(userId);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int envIdCol = cursor.getColumnIndex("ENVIRONMENT_ID");
                if (envIdCol >= 0) {
                    int planEnvId = cursor.getInt(envIdCol);
                    if (planEnvId == envId) {
                        int idCol = cursor.getColumnIndex("ID");
                        if (idCol >= 0) {
                            currentPlanId = cursor.getLong(idCol);
                        }
                        break;
                    }
                }
            }
            cursor.close();
        }
        if (currentPlanId == -1) {
            currentPlanId = dbPlans.createPlan(userId, envId, 30, 100.0);
        }
    }

    private void loadApplications() {
        appList.clear();
        double sumWattage = 0.0;
        double sumMonthlyCost = 0.0;

        float tariffRate = getSharedPreferences("BachatPrefs", MODE_PRIVATE).getFloat("tariff_rate", 8.0f);
        adapter.setTariffRate(tariffRate);

        Cursor cursor = dbPlans.getAppliancesForPlan(currentPlanId);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int idCol = cursor.getColumnIndex("ID");
                int nameCol = cursor.getColumnIndex("APPLIANCE_NAME");
                int qtyCol = cursor.getColumnIndex("QUANTITY");
                int wattCol = cursor.getColumnIndex("WATTAGE");
                int hoursCol = cursor.getColumnIndex("MAX_HOURS");

                if (idCol >= 0 && nameCol >= 0 && qtyCol >= 0 && wattCol >= 0) {
                    long id = cursor.getLong(idCol);
                    String name = cursor.getString(nameCol);
                    int qty = cursor.getInt(qtyCol);
                    double watt = cursor.getDouble(wattCol);
                    double hours = (hoursCol >= 0) ? cursor.getDouble(hoursCol) : 8.0;
                    if (hours <= 0) hours = 8.0;

                    appList.add(new ApplianceAdapter.ApplianceItem(id, name, qty, watt, hours));

                    double applianceWattage = watt * qty;
                    sumWattage += applianceWattage;
                    // Monthly kWh = (wattage * hours * 30d) / 1000 * tariffRate
                    double monthlyKwh = (applianceWattage * hours * 30.0) / 1000.0;
                    sumMonthlyCost += monthlyKwh * tariffRate;
                }
            }
            cursor.close();
        }

        if (appList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }

        if (tvTotalWattage != null) {
            if (sumWattage >= 1000) {
                tvTotalWattage.setText(String.format(Locale.getDefault(), "%.2f kW", sumWattage / 1000.0));
            } else {
                tvTotalWattage.setText(String.format(Locale.getDefault(), "%.0f W", sumWattage));
            }
        }
        if (tvTotalCost != null) {
            tvTotalCost.setText(String.format(Locale.getDefault(), "₹ %,.2f", sumMonthlyCost));
        }

        adapter.notifyDataSetChanged();
    }

    private void showApplianceDetailsDialog(ApplianceAdapter.ApplianceItem item) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_appliance_details);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvName = dialog.findViewById(R.id.tvDetailName);
        TextView tvMonthlyCost = dialog.findViewById(R.id.tvDetailMonthlyCost);
        TextView tvMonthlyKwh = dialog.findViewById(R.id.tvDetailMonthlyKwh);
        TextView tvQty = dialog.findViewById(R.id.tvDetailQty);
        TextView tvWattage = dialog.findViewById(R.id.tvDetailWattage);
        TextView tvDutyCycle = dialog.findViewById(R.id.tvDetailDutyCycle);
        TextView tvTariffRate = dialog.findViewById(R.id.tvDetailTariffRate);
        ImageView ivIcon = dialog.findViewById(R.id.ivDetailIcon);

        tvName.setText(item.name);
        tvQty.setText(item.quantity + " Unit" + (item.quantity > 1 ? "s" : ""));
        tvWattage.setText(String.format(Locale.getDefault(), "%.0f Watts", item.wattage));

        double hoursPerDay = item.hoursPerDay > 0 ? item.hoursPerDay : 8.0;
        double daysPerMonth = 30.0;
        float kwhRate = getSharedPreferences("BachatPrefs", MODE_PRIVATE).getFloat("tariff_rate", 8.0f);
        double monthlyKwh = (item.wattage * item.quantity * hoursPerDay * daysPerMonth) / 1000.0;
        double monthlyCost = monthlyKwh * kwhRate;

        tvMonthlyCost.setText(String.format(Locale.getDefault(), "₹ %,.2f", monthlyCost));
        tvMonthlyKwh.setText(String.format(Locale.getDefault(), "%.2f kWh monthly energy consumption", monthlyKwh));

        if (tvDutyCycle != null) {
            tvDutyCycle.setText(String.format(Locale.getDefault(), "%.0f hrs/day", hoursPerDay));
        }
        if (tvTariffRate != null) {
            tvTariffRate.setText(String.format(Locale.getDefault(), "₹%.2f / kWh", kwhRate));
        }

        // Tint icon
        String n = item.name.toLowerCase();
        int iconRes = android.R.drawable.ic_menu_agenda;
        int colorRes = R.color.icon_default;
        if (n.contains("fan")) {
            iconRes = android.R.drawable.ic_menu_rotate;
            colorRes = R.color.icon_fan;
        } else if (n.contains("ac") || n.contains("air") || n.contains("cooler")) {
            iconRes = android.R.drawable.ic_menu_sort_by_size;
            colorRes = R.color.icon_ac;
        } else if (n.contains("light") || n.contains("lamp") || n.contains("bulb") || n.contains("tube")) {
            iconRes = android.R.drawable.ic_menu_day;
            colorRes = R.color.icon_light;
        } else if (n.contains("tv") || n.contains("screen") || n.contains("qled") || n.contains("oled")) {
            iconRes = android.R.drawable.ic_menu_slideshow;
            colorRes = R.color.icon_tv;
        } else if (n.contains("fridge") || n.contains("refrigerator")) {
            iconRes = android.R.drawable.ic_menu_directions;
            colorRes = R.color.icon_fridge;
        } else if (n.contains("wash")) {
            iconRes = android.R.drawable.ic_menu_compass;
            colorRes = R.color.icon_washing;
        } else if (n.contains("computer") || n.contains("pc") || n.contains("laptop")) {
            iconRes = android.R.drawable.ic_menu_call;
            colorRes = R.color.icon_computer;
        }
        int color = ContextCompat.getColor(this, colorRes);
        ivIcon.setImageResource(iconRes);
        ivIcon.setImageTintList(ColorStateList.valueOf(color));

        dialog.findViewById(R.id.btnCloseDetails).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnEditFromDetails).setOnClickListener(v -> {
            dialog.dismiss();
            showEditApplianceDialog(item);
        });

        dialog.show();
    }

    private void showEditApplianceDialog(ApplianceAdapter.ApplianceItem item) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_edit_appliance);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvEditApplianceTitle);
        EditText inputQty = dialog.findViewById(R.id.etEditQty);
        EditText inputHours = dialog.findViewById(R.id.etEditHours);
        EditText inputWatt = dialog.findViewById(R.id.etEditWattage);
        View btnDelete = dialog.findViewById(R.id.btnDeleteAppliance);

        tvTitle.setText("Edit " + item.name);
        inputQty.setText(String.valueOf(item.quantity));
        if (inputHours != null) {
            inputHours.setText(String.format(Locale.getDefault(), "%.0f", item.hoursPerDay));
        }
        inputWatt.setText(String.format(Locale.getDefault(), "%.0f", item.wattage));

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                dbPlans.deleteApplianceFromPlan(item.id);
                loadApplications();
                Toast.makeText(this, item.name + " removed", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        dialog.findViewById(R.id.btnCancelEdit).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnSaveEdit).setOnClickListener(v -> {
            try {
                int qty = Integer.parseInt(inputQty.getText().toString().trim());
                double watt = Double.parseDouble(inputWatt.getText().toString().trim());
                double hours = item.hoursPerDay;
                if (inputHours != null) {
                    String hStr = inputHours.getText().toString().trim();
                    if (!hStr.isEmpty()) hours = Double.parseDouble(hStr);
                }

                if (qty <= 0 || watt <= 0 || hours <= 0) {
                    Toast.makeText(this, "Quantity, wattage, and hours must be positive", Toast.LENGTH_SHORT).show();
                    return;
                }
                dbPlans.updateApplianceDetails(item.id, qty, watt, hours);
                loadApplications();
                Toast.makeText(this, "Appliance updated", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } catch (Exception e) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showAddApplicationDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_appliance_modern);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Spinner spinnerCat = dialog.findViewById(R.id.spinnerApplianceCategory);
        Spinner spinnerModel = dialog.findViewById(R.id.spinnerApplianceModel);
        TextView tvModelLabel = dialog.findViewById(R.id.tvModelLabel);
        View layoutCustom = dialog.findViewById(R.id.layoutCustomFields);
        EditText etCustomName = dialog.findViewById(R.id.etCustomAppName);
        EditText etCustomWatt = dialog.findViewById(R.id.etCustomAppWattage);
        EditText etQty = dialog.findViewById(R.id.etAppQty);
        EditText etHours = dialog.findViewById(R.id.etAppHours);

        List<PowerUsageDataset.ApplianceCategory> catalog = PowerUsageDataset.getApplianceCatalog();
        List<String> catNames = new ArrayList<>();
        for (PowerUsageDataset.ApplianceCategory cat : catalog) {
            catNames.add(cat.name);
        }
        catNames.add("Custom Device");

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, catNames);
        spinnerCat.setAdapter(catAdapter);

        spinnerCat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == catNames.size() - 1) {
                    // Custom
                    if (layoutCustom != null) layoutCustom.setVisibility(View.VISIBLE);
                    if (spinnerModel != null) spinnerModel.setVisibility(View.GONE);
                    if (tvModelLabel != null) tvModelLabel.setVisibility(View.GONE);
                    if (etHours != null) etHours.setText("8");
                } else {
                    if (layoutCustom != null) layoutCustom.setVisibility(View.GONE);
                    if (spinnerModel != null) spinnerModel.setVisibility(View.VISIBLE);
                    if (tvModelLabel != null) tvModelLabel.setVisibility(View.VISIBLE);

                    PowerUsageDataset.ApplianceCategory selectedCat = catalog.get(position);
                    List<String> modelStrings = new ArrayList<>();
                    for (PowerUsageDataset.ApplianceModel model : selectedCat.models) {
                        modelStrings.add(model.name + " (" + (int) model.baseWattage + "W - " + model.badge + ")");
                    }
                    ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(ApplicationsActivity.this,
                            android.R.layout.simple_spinner_dropdown_item, modelStrings);
                    spinnerModel.setAdapter(modelAdapter);
                    if (!selectedCat.models.isEmpty() && etHours != null) {
                        etHours.setText(String.format(Locale.getDefault(), "%.0f", selectedCat.models.get(0).defaultHours));
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int catPos = spinnerCat.getSelectedItemPosition();
                if (catPos >= 0 && catPos < catalog.size()) {
                    PowerUsageDataset.ApplianceCategory cat = catalog.get(catPos);
                    if (position >= 0 && position < cat.models.size() && etHours != null) {
                        etHours.setText(String.format(Locale.getDefault(), "%.0f", cat.models.get(position).defaultHours));
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        dialog.findViewById(R.id.btnCancelAddApp).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnConfirmAddApp).setOnClickListener(v -> {
            int catPos = spinnerCat.getSelectedItemPosition();
            String qtyStr = etQty.getText().toString().trim();
            int qty = qtyStr.isEmpty() ? 1 : Integer.parseInt(qtyStr);

            String hoursStr = etHours != null ? etHours.getText().toString().trim() : "8";
            double hours = hoursStr.isEmpty() ? 8.0 : Double.parseDouble(hoursStr);

            String name;
            double baseWatt;

            if (catPos == catNames.size() - 1) {
                name = etCustomName.getText().toString().trim();
                String wStr = etCustomWatt.getText().toString().trim();
                baseWatt = wStr.isEmpty() ? 100.0 : Double.parseDouble(wStr);
            } else {
                PowerUsageDataset.ApplianceCategory cat = catalog.get(catPos);
                int modelPos = spinnerModel.getSelectedItemPosition();
                if (modelPos < 0 || modelPos >= cat.models.size()) modelPos = 0;
                PowerUsageDataset.ApplianceModel model = cat.models.get(modelPos);
                name = model.name;
                baseWatt = model.baseWattage;
            }

            if (!name.isEmpty() && baseWatt > 0 && qty > 0) {
                double adjustedWatt = PowerUsageDataset.applyStateMultiplier(baseWatt, state);
                dbPlans.addApplianceToPlan(currentPlanId, 0, name, qty, adjustedWatt, hours);
                loadApplications();
                Toast.makeText(this, name + " added to " + envName, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please enter valid appliance details", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say appliances (e.g. 2 BLDC fans, 1 inverter AC, 4 LED bulbs)");

        try {
            voiceResultLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition is not supported on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    private void processVoiceInput(String spokenText) {
        String lowerText = spokenText.toLowerCase(Locale.ROOT);
        // Split on 'and', comma, semicolons
        String[] segments = lowerText.split("(?i)\\b(and)\\b|[,;]");

        List<String> addedSummaries = new ArrayList<>();

        for (String segment : segments) {
            String phrase = segment.trim();
            if (phrase.isEmpty()) continue;

            // Extract quantity if present
            int qty = 1;
            String[] tokens = phrase.split("\\s+");
            for (String t : tokens) {
                if (t.matches("\\d+")) {
                    qty = Integer.parseInt(t);
                    break;
                } else if (t.equals("one") || t.equals("a") || t.equals("an")) {
                    qty = 1;
                    break;
                } else if (t.equals("two")) {
                    qty = 2;
                    break;
                } else if (t.equals("three")) {
                    qty = 3;
                    break;
                } else if (t.equals("four")) {
                    qty = 4;
                    break;
                } else if (t.equals("five")) {
                    qty = 5;
                    break;
                }
            }

            PowerUsageDataset.ApplianceModel matched = PowerUsageDataset.matchVoiceAppliance(phrase);
            if (matched != null) {
                double adjustedWatt = PowerUsageDataset.applyStateMultiplier(matched.baseWattage, state);
                dbPlans.addApplianceToPlan(currentPlanId, 0, matched.name, qty, adjustedWatt, matched.defaultHours);
                addedSummaries.add(qty + "x " + matched.name);
            }
        }

        if (!addedSummaries.isEmpty()) {
            loadApplications();
            Toast.makeText(this, "Added: " + TextUtils.join(", ", addedSummaries), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Could not match appliances in: \"" + spokenText + "\". Try saying '2 BLDC fans' or '1 AC'", Toast.LENGTH_LONG).show();
        }
    }
}
