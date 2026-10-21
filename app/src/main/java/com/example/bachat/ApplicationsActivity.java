package com.example.bachat;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ApplicationsActivity extends AppCompatActivity {

    private int userId;
    private int envId;
    private String envName;

    private ListView listViewApplications;
    private Button buttonAddApplication;
    private TextView textViewPlaceName;

    private ArrayList<ApplicationItem> appList;
    private AppListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_applications);

        userId = getIntent().getIntExtra("USER_ID", -1);
        envId = getIntent().getIntExtra("ENVIRONMENT_ID", -1);
        envName = getIntent().getStringExtra("ENVIRONMENT_NAME");

        listViewApplications = findViewById(R.id.listViewApplications);
        buttonAddApplication = findViewById(R.id.buttonAddApplication);
        textViewPlaceName = findViewById(R.id.textViewPlaceName);

        if (envName != null) {
            textViewPlaceName.setText(envName);
        }

        appList = new ArrayList<>();
        adapter = new AppListAdapter(this, appList);
        listViewApplications.setAdapter(adapter);

        loadApplications();

        buttonAddApplication.setOnClickListener(v -> showAddApplicationDialog());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadApplications() {
        appList.clear();
        File file = new File(getExternalFilesDir(null), "applications.csv");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 7) {
                        int fUserId = Integer.parseInt(parts[0]);
                        int fEnvId = Integer.parseInt(parts[1]);
                        if (fUserId == userId && fEnvId == envId) {
                            String name = parts[2];
                            String power = parts[3];
                            String voltage = parts[4];
                            String budget = parts[5];
                            String type = parts[6];
                            appList.add(new ApplicationItem(name, power, voltage, budget, type));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showAddApplicationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Application");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_application, null);
        builder.setView(dialogView);

        Spinner spinnerApplianceType = dialogView.findViewById(R.id.spinnerApplianceType);
        EditText editTextAppName = dialogView.findViewById(R.id.editTextAppName);
        EditText editTextPower = dialogView.findViewById(R.id.editTextPower);
        EditText editTextVoltage = dialogView.findViewById(R.id.editTextVoltage);
        EditText editTextBudget = dialogView.findViewById(R.id.editTextBudget);

        String[] types = {"Custom", "AC", "Fridge", "TV", "Bulb", "Fan"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerApplianceType.setAdapter(spinnerAdapter);

        spinnerApplianceType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = types[position];
                switch (selected) {
                    case "AC":
                        editTextAppName.setText("AC");
                        editTextPower.setText("1500");
                        editTextVoltage.setText("220");
                        editTextBudget.setText("50");
                        break;
                    case "Fridge":
                        editTextAppName.setText("Fridge");
                        editTextPower.setText("400");
                        editTextVoltage.setText("220");
                        editTextBudget.setText("20");
                        break;
                    case "TV":
                        editTextAppName.setText("TV");
                        editTextPower.setText("100");
                        editTextVoltage.setText("220");
                        editTextBudget.setText("10");
                        break;
                    case "Bulb":
                        editTextAppName.setText("Bulb");
                        editTextPower.setText("10");
                        editTextVoltage.setText("220");
                        editTextBudget.setText("2");
                        break;
                    case "Fan":
                        editTextAppName.setText("Fan");
                        editTextPower.setText("50");
                        editTextVoltage.setText("220");
                        editTextBudget.setText("5");
                        break;
                    case "Custom":
                        editTextAppName.setText("");
                        editTextPower.setText("");
                        editTextVoltage.setText("");
                        editTextBudget.setText("");
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = editTextAppName.getText().toString().trim();
            String power = editTextPower.getText().toString().trim();
            String voltage = editTextVoltage.getText().toString().trim();
            String budget = editTextBudget.getText().toString().trim();
            String type = spinnerApplianceType.getSelectedItem().toString();

            if (!name.isEmpty() && !power.isEmpty()) {
                saveApplicationToCSV(name, power, voltage, budget, type);
                loadApplications();
                Toast.makeText(this, "Application added", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveApplicationToCSV(String name, String power, String voltage, String budget, String type) {
        try {
            File file = new File(getExternalFilesDir(null), "applications.csv");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file, true);
            // Format: userId,envId,name,power,voltage,budget,type
            writer.append(String.valueOf(userId)).append(",")
                    .append(String.valueOf(envId)).append(",")
                    .append(name).append(",")
                    .append(power).append(",")
                    .append(voltage).append(",")
                    .append(budget).append(",")
                    .append(type).append("\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ApplicationItem {
        String name, power, voltage, budget, type;
        ApplicationItem(String name, String power, String voltage, String budget, String type) {
            this.name = name;
            this.power = power;
            this.voltage = voltage;
            this.budget = budget;
            this.type = type;
        }
    }

    private class AppListAdapter extends ArrayAdapter<ApplicationItem> {
        AppListAdapter(AppCompatActivity context, ArrayList<ApplicationItem> list) {
            super(context, 0, list);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_application, parent, false);
            }
            ApplicationItem item = getItem(position);
            TextView name = convertView.findViewById(R.id.textViewAppName);
            TextView details = convertView.findViewById(R.id.textViewAppDetails);

            if (item != null) {
                name.setText(item.name);
                details.setText("Power: " + item.power + "W | Voltage: " + item.voltage + "V | Budget: $" + item.budget);
            }
            return convertView;
        }
    }
}
