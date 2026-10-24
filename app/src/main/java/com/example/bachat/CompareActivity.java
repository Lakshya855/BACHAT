package com.example.bachat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class CompareActivity extends AppCompatActivity {

    private TextView chipCatFans, chipCatAC, chipCatLights;
    private TextView tvBrandA, tvNameA, tvCostA, tvWattA;
    private TextView tvBrandB, tvNameB, tvCostB, tvWattB;
    private TextView tvVerdictTitle, tvVerdictDesc;
    private TextView tvRowWattA, tvRowWattB;
    private TextView tvRowCostA, tvRowCostB;
    private TextView tvRowAnnualA, tvRowAnnualB;
    private ImageView ivIconA, ivIconB;

    private double tariffRate = 8.0;
    private int userId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_compare);

        userId = getIntent().getIntExtra("USER_ID", -1);

        Toolbar toolbar = findViewById(R.id.toolbarCompare);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        tariffRate = getSharedPreferences("BachatPrefs", MODE_PRIVATE).getFloat("tariff_rate", 8.0f);

        chipCatFans = findViewById(R.id.chipCatFans);
        chipCatAC = findViewById(R.id.chipCatAC);
        chipCatLights = findViewById(R.id.chipCatLights);

        tvBrandA = findViewById(R.id.tvBrandA);
        tvNameA = findViewById(R.id.tvNameA);
        tvCostA = findViewById(R.id.tvCostA);
        tvWattA = findViewById(R.id.tvWattA);

        tvBrandB = findViewById(R.id.tvBrandB);
        tvNameB = findViewById(R.id.tvNameB);
        tvCostB = findViewById(R.id.tvCostB);
        tvWattB = findViewById(R.id.tvWattB);

        tvVerdictTitle = findViewById(R.id.tvVerdictTitle);
        tvVerdictDesc = findViewById(R.id.tvVerdictDesc);

        tvRowWattA = findViewById(R.id.tvRowWattA);
        tvRowWattB = findViewById(R.id.tvRowWattB);
        tvRowCostA = findViewById(R.id.tvRowCostA);
        tvRowCostB = findViewById(R.id.tvRowCostB);
        tvRowAnnualA = findViewById(R.id.tvRowAnnualA);
        tvRowAnnualB = findViewById(R.id.tvRowAnnualB);

        ivIconA = findViewById(R.id.ivIconA);
        ivIconB = findViewById(R.id.ivIconB);

        chipCatFans.setOnClickListener(v -> selectCategory(0));
        chipCatAC.setOnClickListener(v -> selectCategory(1));
        chipCatLights.setOnClickListener(v -> selectCategory(2));

        selectCategory(0); // Default to Fans

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void selectCategory(int index) {
        // Reset chips
        chipCatFans.setBackgroundResource(R.drawable.bg_chip);
        chipCatFans.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        chipCatAC.setBackgroundResource(R.drawable.bg_chip);
        chipCatAC.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        chipCatLights.setBackgroundResource(R.drawable.bg_chip);
        chipCatLights.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

        if (index == 0) {
            chipCatFans.setBackgroundResource(R.drawable.bg_chip_selected);
            chipCatFans.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
            setupComparison(
                    "BLDC 5-Star", 28, android.R.drawable.ic_menu_rotate,
                    "Induction Fan", 75, android.R.drawable.ic_menu_rotate,
                    8.0, "BLDC Fan fits your efficiency goals better",
                    "62% less energy consumed. Saves over ₹1,000 annually per ceiling fan."
            );
        } else if (index == 1) {
            chipCatAC.setBackgroundResource(R.drawable.bg_chip_selected);
            chipCatAC.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
            setupComparison(
                    "5-Star Inverter AC", 950, android.R.drawable.ic_menu_sort_by_size,
                    "3-Star Regular AC", 1600, android.R.drawable.ic_menu_sort_by_size,
                    8.0, "5-Star Inverter AC wins on long-term cooling costs",
                    "Cuts compressor power by 40%. Saves approx ₹1,248 every summer month."
            );
        } else {
            chipCatLights.setBackgroundResource(R.drawable.bg_chip_selected);
            chipCatLights.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
            setupComparison(
                    "LED Batten 20W", 20, android.R.drawable.ic_menu_day,
                    "Old Tubelight", 55, android.R.drawable.ic_menu_day,
                    8.0, "LED Batten produces equal light for 63% less power",
                    "Saves ₹672 per light tube each year and lasts 3x longer."
            );
        }
    }

    private void setupComparison(String nameA, double wattA, int iconA,
                                String nameB, double wattB, int iconB,
                                double hoursPerDay, String verdictTitle, String verdictDesc) {
        tvNameA.setText(nameA);
        tvWattA.setText(String.format(Locale.getDefault(), "%.0f Watts", wattA));
        ivIconA.setImageResource(iconA);

        tvNameB.setText(nameB);
        tvWattB.setText(String.format(Locale.getDefault(), "%.0f Watts", wattB));
        ivIconB.setImageResource(iconB);

        double monthlyKwhA = (wattA * hoursPerDay * 30.0) / 1000.0;
        double monthlyCostA = monthlyKwhA * tariffRate;
        double annualCostA = monthlyCostA * 12.0;

        double monthlyKwhB = (wattB * hoursPerDay * 30.0) / 1000.0;
        double monthlyCostB = monthlyKwhB * tariffRate;
        double annualCostB = monthlyCostB * 12.0;

        SharedPreferences prefs = getSharedPreferences("BachatPrefs", MODE_PRIVATE);
        String mode = prefs.getString("budget_mode_" + userId, "monthly");
        boolean isMonthly = "monthly".equalsIgnoreCase(mode);

        if (isMonthly) {
            tvCostA.setText(String.format(Locale.getDefault(), "₹ %,.0f / mo", monthlyCostA));
            tvCostB.setText(String.format(Locale.getDefault(), "₹ %,.0f / mo", monthlyCostB));
            tvRowCostA.setText(String.format(Locale.getDefault(), "₹ %,.0f / mo", monthlyCostA));
            tvRowCostB.setText(String.format(Locale.getDefault(), "₹ %,.0f / mo", monthlyCostB));
        } else {
            tvCostA.setText(String.format(Locale.getDefault(), "₹ %,.2f / day", monthlyCostA / 30.0));
            tvCostB.setText(String.format(Locale.getDefault(), "₹ %,.2f / day", monthlyCostB / 30.0));
            tvRowCostA.setText(String.format(Locale.getDefault(), "₹ %,.2f / day", monthlyCostA / 30.0));
            tvRowCostB.setText(String.format(Locale.getDefault(), "₹ %,.2f / day", monthlyCostB / 30.0));
        }

        tvRowWattA.setText(String.format(Locale.getDefault(), "%.0f W", wattA));
        tvRowWattB.setText(String.format(Locale.getDefault(), "%.0f W", wattB));

        tvRowAnnualA.setText(String.format(Locale.getDefault(), "₹ %,.0f / yr", annualCostA));
        tvRowAnnualB.setText(String.format(Locale.getDefault(), "₹ %,.0f / yr", annualCostB));

        tvVerdictTitle.setText(verdictTitle);
        tvVerdictDesc.setText(verdictDesc);
    }
}
