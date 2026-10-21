package com.example.bachat;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class type_of_places extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private int userId;
    private Database_plans dbPlans;
    private ListView listViewPlaces;
    private Button buttonAddPlace;
    private DrawerLayout drawerLayout;

    private ArrayList<PlaceItem> placeList;
    private ArrayAdapter<PlaceItem> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_type_of_places);

        userId = getIntent().getIntExtra("USER_ID", -1);
        dbPlans = new Database_plans(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        listViewPlaces = findViewById(R.id.listViewPlaces);
        buttonAddPlace = findViewById(R.id.buttonAddPlace);

        placeList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, R.layout.list_item_place, R.id.textViewPlaceName, placeList);
        listViewPlaces.setAdapter(adapter);

        loadPlaces();

        buttonAddPlace.setOnClickListener(v -> showAddPlaceDialog());

        listViewPlaces.setOnItemClickListener((parent, view, position, id) -> {
            PlaceItem selectedPlace = placeList.get(position);
            Intent intent = new Intent(type_of_places.this, ApplicationsActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("ENVIRONMENT_ID", selectedPlace.id);
            intent.putExtra("ENVIRONMENT_NAME", selectedPlace.name);
            startActivity(intent);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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

    private void loadPlaces() {
        placeList.clear();
        Cursor cursor = dbPlans.getUserEnvironments(userId);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int idIndex = cursor.getColumnIndex("ID");
                int nameIndex = cursor.getColumnIndex("NAME");
                if (idIndex != -1 && nameIndex != -1) {
                    placeList.add(new PlaceItem(cursor.getInt(idIndex), cursor.getString(nameIndex)));
                }
            }
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    private void showAddPlaceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add a New Place");

        final EditText input = new EditText(this);
        input.setHint("Enter place name");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String placeName = input.getText().toString().trim();
            if (!placeName.isEmpty()) {
                boolean added = dbPlans.addEnvironment(placeName, userId);
                if (added) {
                    savePlaceToCSV(placeName);
                    loadPlaces();
                    Toast.makeText(type_of_places.this, "Place added successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(type_of_places.this, "Place already exists or error", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void savePlaceToCSV(String placeName) {
        try {
            File file = new File(getExternalFilesDir(null), "places.csv");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file, true);
            writer.append(String.valueOf(userId)).append(",").append(placeName).append("\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            showProfileDialog();
        } else if (id == R.id.nav_report) {
            showReportDialog();
        } else if (id == R.id.nav_settings) {
            showSettingsDialog();
        } else if (id == R.id.nav_feedback) {
            showFeedbackDialog();
        } else if (id == R.id.nav_exit) {
            finishAffinity();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showProfileDialog() {
        Database_users dbUsers = new Database_users(this);
        // We might not have an easy method to get all info by userId, but let's assume we can query it
        // Since we only have userId, we can get cursor
        Cursor cursor = dbUsers.getReadableDatabase().rawQuery("SELECT * FROM Userdetails WHERE id=?", new String[]{String.valueOf(userId)});
        String profileInfo = "Name: Unknown\nEmail: Unknown\nGender: Unknown\nPolicy: Unknown";
        if (cursor.moveToFirst()) {
            profileInfo = "Name: " + cursor.getString(1) + "\n" +
                          "Email: " + cursor.getString(2) + "\n" +
                          "Gender: " + cursor.getString(4) + "\n" +
                          "Policy Accepted: " + cursor.getString(5);
        }
        cursor.close();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Profile");
        builder.setMessage(profileInfo);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void showReportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Report a Problem");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputProblem = new EditText(this);
        inputProblem.setHint("Problem Title");
        layout.addView(inputProblem);

        final EditText inputDesc = new EditText(this);
        inputDesc.setHint("Description");
        layout.addView(inputDesc);

        builder.setView(layout);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String prob = inputProblem.getText().toString();
            String desc = inputDesc.getText().toString();
            saveReportToCSV(prob, desc);
            Toast.makeText(type_of_places.this, "Report sent successfully", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveReportToCSV(String problem, String description) {
        try {
            File file = new File(getExternalFilesDir(null), "reports.csv");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file, true);
            writer.append(String.valueOf(userId)).append(",").append(problem).append(",").append(description).append("\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings - Theme");
        String[] options = {"Light Mode", "Dark Mode", "System Default"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
            } else if (which == 1) {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        });
        builder.show();
    }

    private void showFeedbackDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Give Feedback");

        // We can create a simple layout programmatically for RatingBar and EditText
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final RatingBar ratingBar = new RatingBar(this);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1.0f);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = android.view.Gravity.CENTER;
        ratingBar.setLayoutParams(lp);
        layout.addView(ratingBar);

        final EditText input = new EditText(this);
        input.setHint("Write your feedback here...");
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton("Send", (dialog, which) -> {
            float rating = ratingBar.getRating();
            String feedback = input.getText().toString();
            saveFeedbackToCSV(rating, feedback);
            Toast.makeText(type_of_places.this, "Feedback sent successfully", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveFeedbackToCSV(float rating, String feedback) {
        try {
            File file = new File(getExternalFilesDir(null), "feedback.csv");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file, true);
            writer.append(String.valueOf(userId)).append(",").append(String.valueOf(rating)).append(",").append(feedback).append("\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class PlaceItem {
        int id;
        String name;

        PlaceItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
