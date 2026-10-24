package com.example.bachat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class Database_plans extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Users.db";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_ENVIRONMENTS = "environments";
    private static final String TABLE_APPLIANCES = "appliances";
    private static final String TABLE_PLANS = "energy_plans";
    private static final String TABLE_PLAN_APPLIANCES = "plan_appliances";

    public Database_plans(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        try {
            // Heal any continuous baseline appliances (e.g. refrigerators/freezers) previously reduced below 24h
            db.execSQL("UPDATE " + TABLE_PLAN_APPLIANCES + " SET MAX_HOURS = 24.0, ALLOCATED_HOURS = 24.0 " +
                    "WHERE (LOWER(APPLIANCE_NAME) LIKE '%fridge%' OR LOWER(APPLIANCE_NAME) LIKE '%refrigerator%' " +
                    "OR LOWER(APPLIANCE_NAME) LIKE '%freezer%' OR LOWER(APPLIANCE_NAME) LIKE '%chiller%' " +
                    "OR LOWER(APPLIANCE_NAME) LIKE '%double door%' OR LOWER(APPLIANCE_NAME) LIKE '%single door%') " +
                    "AND (MAX_HOURS < 24.0 OR ALLOCATED_HOURS < 24.0)");
        } catch (Exception ignored) {}
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    private void createTables(SQLiteDatabase db) {
        // Ensure users table exists first
        Database_users.createTables(db);

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ENVIRONMENTS + " (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "NAME TEXT NOT NULL, " +
                "USER_ID INTEGER, " +
                "FOREIGN KEY(USER_ID) REFERENCES users(ID))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_APPLIANCES + " (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "NAME TEXT NOT NULL, " +
                "WATTAGE REAL NOT NULL, " +
                "IMAGE TEXT, " +
                "USER_ID INTEGER, " +
                "FOREIGN KEY(USER_ID) REFERENCES users(ID))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PLANS + " (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "USER_ID INTEGER NOT NULL, " +
                "ENVIRONMENT_ID INTEGER NOT NULL, " +
                "DURATION_DAYS INTEGER NOT NULL, " +
                "DAILY_BUDGET REAL NOT NULL, " +
                "CREATED_AT TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(USER_ID) REFERENCES users(ID), " +
                "FOREIGN KEY(ENVIRONMENT_ID) REFERENCES environments(ID))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PLAN_APPLIANCES + " (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "PLAN_ID INTEGER NOT NULL, " +
                "APPLIANCE_ID INTEGER, " +
                "APPLIANCE_NAME TEXT NOT NULL, " +
                "QUANTITY INTEGER NOT NULL DEFAULT 1, " +
                "WATTAGE REAL NOT NULL, " +
                "MAX_HOURS REAL NOT NULL, " +
                "ALLOCATED_HOURS REAL DEFAULT 0, " +
                "COST_CONTRIBUTION REAL DEFAULT 0, " +
                "FOREIGN KEY(PLAN_ID) REFERENCES energy_plans(ID) " +
                "ON DELETE CASCADE)");

        insertEnvironments(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        createTables(db);
    }

    private void insertEnvironments(SQLiteDatabase db) {
        String[] environments = {"Home", "Office", "Factory", "Shop"};

        for (String environment : environments) {
            Cursor cursor = db.rawQuery("SELECT ID FROM " + TABLE_ENVIRONMENTS + " WHERE NAME = ? AND USER_ID IS NULL", new String[]{environment});
            boolean exists = cursor.moveToFirst();
            cursor.close();

            if (!exists) {
                ContentValues values = new ContentValues();
                values.put("NAME", environment);
                values.putNull("USER_ID");
                db.insert(TABLE_ENVIRONMENTS, null, values);
            }
        }
    }

    public Cursor getEnvironments() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_ENVIRONMENTS + " WHERE USER_ID IS NULL ORDER BY ID", null);
    }

    public Cursor getEnvironments(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        createTables(db);
        return db.rawQuery("SELECT * FROM " + TABLE_ENVIRONMENTS + " WHERE USER_ID IS NULL OR USER_ID = ? ORDER BY ID", new String[]{String.valueOf(userId)});
    }

    public Cursor getUserEnvironments(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_ENVIRONMENTS + " WHERE USER_ID = ? ORDER BY ID", new String[]{String.valueOf(userId)});
    }

    public boolean addEnvironment(String name, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        createTables(db);

        name = name != null ? name.trim() : "";
        if (name.isEmpty()) {
            return false;
        }

        Cursor cursor = db.rawQuery("SELECT ID FROM " + TABLE_ENVIRONMENTS + " WHERE NAME = ? AND (USER_ID = ? OR USER_ID IS NULL)", new String[]{name, String.valueOf(userId)});
        boolean exists = cursor.moveToFirst();
        cursor.close();

        if (exists) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("NAME", name);
        values.put("USER_ID", userId);

        long result = db.insert(TABLE_ENVIRONMENTS, null, values);
        return result != -1;
    }

    public boolean addAppliance(String name, double wattage, String image, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        createTables(db);

        name = name != null ? name.trim() : "";
        if (name.isEmpty() || wattage <= 0) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("NAME", name);
        values.put("WATTAGE", wattage);

        if (image == null || image.trim().isEmpty()) {
            values.putNull("IMAGE");
        } else {
            values.put("IMAGE", image);
        }

        values.put("USER_ID", userId);
        long result = db.insert(TABLE_APPLIANCES, null, values);
        return result != -1;
    }

    public Cursor getAppliances(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_APPLIANCES + " WHERE USER_ID IS NULL OR USER_ID = ? ORDER BY ID", new String[]{String.valueOf(userId)});
    }

    public long createPlan(int userId, int environmentId, int durationDays, double dailyBudget) {
        SQLiteDatabase db = this.getWritableDatabase();
        createTables(db);

        if (durationDays <= 0 || dailyBudget <= 0) {
            return -1;
        }

        ContentValues values = new ContentValues();
        values.put("USER_ID", userId);
        values.put("ENVIRONMENT_ID", environmentId);
        values.put("DURATION_DAYS", durationDays);
        values.put("DAILY_BUDGET", dailyBudget);

        return db.insert(TABLE_PLANS, null, values);
    }

    public long addApplianceToPlan(long planId, long applianceId, String applianceName, int quantity, double wattage, double maxHours) {
        SQLiteDatabase db = this.getWritableDatabase();
        createTables(db);

        if (quantity <= 0 || wattage <= 0 || maxHours < 0) {
            return -1;
        }

        ContentValues values = new ContentValues();
        values.put("PLAN_ID", planId);
        values.put("APPLIANCE_ID", applianceId);
        values.put("APPLIANCE_NAME", applianceName);
        values.put("QUANTITY", quantity);
        values.put("WATTAGE", wattage);
        values.put("MAX_HOURS", maxHours);
        values.put("ALLOCATED_HOURS", maxHours);

        return db.insert(TABLE_PLAN_APPLIANCES, null, values);
    }

    public Cursor getPlansForUser(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        createTables(db);
        return db.rawQuery("SELECT * FROM " + TABLE_PLANS + " WHERE USER_ID = ? ORDER BY ID DESC", new String[]{String.valueOf(userId)});
    }

    public Cursor getAppliancesForPlan(long planId) {
        SQLiteDatabase db = this.getReadableDatabase();
        createTables(db);
        return db.rawQuery("SELECT * FROM " + TABLE_PLAN_APPLIANCES + " WHERE PLAN_ID = ?", new String[]{String.valueOf(planId)});
    }

    public boolean updateAppliance(long applianceRecordId, int quantity, double wattage) {
        return updateApplianceDetails(applianceRecordId, quantity, wattage, 8.0);
    }

    public boolean updateApplianceDetails(long applianceRecordId, int quantity, double wattage, double hoursPerDay) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("QUANTITY", quantity);
        values.put("WATTAGE", wattage);
        values.put("MAX_HOURS", hoursPerDay > 0 ? hoursPerDay : 8.0);
        values.put("ALLOCATED_HOURS", hoursPerDay > 0 ? hoursPerDay : 8.0);
        int result = db.update(TABLE_PLAN_APPLIANCES, values, "ID = ?", new String[]{String.valueOf(applianceRecordId)});
        return result > 0;
    }

    public boolean deleteApplianceFromPlan(long applianceRecordId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_PLAN_APPLIANCES, "ID = ?", new String[]{String.valueOf(applianceRecordId)});
        return result > 0;
    }

    public double getTotalUserMonthlyCost(int userId, double tariffRate) {
        SQLiteDatabase db = this.getReadableDatabase();
        createTables(db);
        double totalCost = 0.0;
        String query = "SELECT pa.QUANTITY, pa.WATTAGE, pa.MAX_HOURS FROM " + TABLE_PLAN_APPLIANCES + " pa " +
                "INNER JOIN " + TABLE_PLANS + " p ON pa.PLAN_ID = p.ID WHERE p.USER_ID = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int qty = cursor.getInt(0);
                double watt = cursor.getDouble(1);
                double hours = cursor.getDouble(2);
                if (hours <= 0) hours = 8.0;
                double monthlyKwh = (qty * watt * hours * 30.0) / 1000.0;
                totalCost += (monthlyKwh * tariffRate);
            }
            cursor.close();
        }
        return totalCost;
    }

    public double getTotalUserWattage(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        createTables(db);
        double totalWatt = 0.0;
        String query = "SELECT pa.QUANTITY, pa.WATTAGE FROM " + TABLE_PLAN_APPLIANCES + " pa " +
                "INNER JOIN " + TABLE_PLANS + " p ON pa.PLAN_ID = p.ID WHERE p.USER_ID = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int qty = cursor.getInt(0);
                double watt = cursor.getDouble(1);
                totalWatt += (qty * watt);
            }
            cursor.close();
        }
        return totalWatt;
    }

    public String getTopConsumingAppliance(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        createTables(db);
        String topApp = "None";
        double maxMonthlyWatt = 0;
        String query = "SELECT pa.APPLIANCE_NAME, (pa.QUANTITY * pa.WATTAGE * pa.MAX_HOURS) as totalPower FROM " + TABLE_PLAN_APPLIANCES + " pa " +
                "INNER JOIN " + TABLE_PLANS + " p ON pa.PLAN_ID = p.ID WHERE p.USER_ID = ? ORDER BY totalPower DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                topApp = cursor.getString(0);
            }
            cursor.close();
        }
        return topApp;
    }

    public Cursor getAllAppliancesForUser(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        createTables(db);
        String query = "SELECT pa.ID, pa.PLAN_ID, pa.APPLIANCE_NAME, pa.QUANTITY, pa.WATTAGE, pa.MAX_HOURS, pa.ALLOCATED_HOURS, pa.COST_CONTRIBUTION, e.NAME as ENV_NAME " +
                "FROM " + TABLE_PLAN_APPLIANCES + " pa " +
                "INNER JOIN " + TABLE_PLANS + " p ON pa.PLAN_ID = p.ID " +
                "LEFT JOIN " + TABLE_ENVIRONMENTS + " e ON p.ENVIRONMENT_ID = e.ID " +
                "WHERE p.USER_ID = ? ORDER BY pa.ID DESC";
        return db.rawQuery(query, new String[]{String.valueOf(userId)});
    }

    public boolean updateAllocatedHours(long applianceRecordId, double allocatedHours, double costContribution) {
        SQLiteDatabase db = this.getWritableDatabase();
        createTables(db);

        // Continuous essential baseline appliances (refrigerators, freezers, etc.) must NEVER be set below 24.0h
        Cursor cursor = db.rawQuery("SELECT APPLIANCE_NAME FROM " + TABLE_PLAN_APPLIANCES + " WHERE ID = ?",
                new String[]{String.valueOf(applianceRecordId)});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (SmartOptimizer.isContinuousEssential(name) && allocatedHours < 24.0) {
                    allocatedHours = 24.0;
                }
            }
            cursor.close();
        }

        ContentValues values = new ContentValues();
        values.put("ALLOCATED_HOURS", allocatedHours);
        values.put("MAX_HOURS", allocatedHours);
        values.put("COST_CONTRIBUTION", costContribution);

        int result = db.update(TABLE_PLAN_APPLIANCES, values, "ID = ?", new String[]{String.valueOf(applianceRecordId)});
        return result > 0;
    }

    public boolean deletePlan(long planId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_PLANS, "ID = ?", new String[]{String.valueOf(planId)});
        return result > 0;
    }
}
