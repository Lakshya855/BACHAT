
package com.example.bachat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class Database_plans extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Users.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_ENVIRONMENTS = "environments";
    private static final String TABLE_APPLIANCES = "appliances";
    private static final String TABLE_PLANS = "energy_plans";
    private static final String TABLE_PLAN_APPLIANCES = "plan_appliances";

    public Database_plans(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        SQLiteDatabase db = this.getWritableDatabase();
        createTables(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    private void createTables(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ENVIRONMENTS + " (" + "ID INTEGER PRIMARY KEY AUTOINCREMENT, " + "NAME TEXT NOT NULL, " + "USER_ID INTEGER, " + "FOREIGN KEY(USER_ID) REFERENCES users(ID))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_APPLIANCES + " (" + "ID INTEGER PRIMARY KEY AUTOINCREMENT, " + "NAME TEXT NOT NULL, " + "WATTAGE REAL NOT NULL, " + "IMAGE TEXT, " + "USER_ID INTEGER, " + "FOREIGN KEY(USER_ID) REFERENCES users(ID))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PLANS + " (" + "ID INTEGER PRIMARY KEY AUTOINCREMENT, " + "USER_ID INTEGER NOT NULL, " + "ENVIRONMENT_ID INTEGER NOT NULL, " + "DURATION_DAYS INTEGER NOT NULL, " + "DAILY_BUDGET REAL NOT NULL, " + "CREATED_AT TEXT DEFAULT CURRENT_TIMESTAMP, " + "FOREIGN KEY(USER_ID) REFERENCES users(ID), " + "FOREIGN KEY(ENVIRONMENT_ID) REFERENCES environments(ID))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PLAN_APPLIANCES + " (" + "ID INTEGER PRIMARY KEY AUTOINCREMENT, " + "PLAN_ID INTEGER NOT NULL, " + "APPLIANCE_ID INTEGER, " + "APPLIANCE_NAME TEXT NOT NULL, " + "QUANTITY INTEGER NOT NULL DEFAULT 1, " + "WATTAGE REAL NOT NULL, " + "MAX_HOURS REAL NOT NULL, " + "ALLOCATED_HOURS REAL DEFAULT 0, " + "COST_CONTRIBUTION REAL DEFAULT 0, " + "FOREIGN KEY(PLAN_ID) REFERENCES energy_plans(ID) " + "ON DELETE CASCADE)");

        insertEnvironments(db);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        createTables(db);
    }

    private void insertEnvironments(SQLiteDatabase db) {

        String[] environments = {"Home", "Factory", "Warehouse", "Office"};

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

        return db.rawQuery("SELECT * FROM " + TABLE_ENVIRONMENTS + " WHERE USER_ID IS NULL OR USER_ID = ? ORDER BY ID", new String[]{String.valueOf(userId)});
    }

    public boolean addEnvironment(String name, int userId) {

        SQLiteDatabase db = this.getWritableDatabase();

        name = name.trim();

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

        name = name.trim();

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

        return db.insert(TABLE_PLAN_APPLIANCES, null, values);
    }

    public Cursor getPlansForUser(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT * FROM " + TABLE_PLANS + " WHERE USER_ID = ? ORDER BY ID DESC", new String[]{String.valueOf(userId)});
    }

    public Cursor getAppliancesForPlan(long planId) {
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery("SELECT * FROM " + TABLE_PLAN_APPLIANCES + " WHERE PLAN_ID = ?", new String[]{String.valueOf(planId)});
    }

    public boolean updateAllocatedHours(long applianceRecordId, double allocatedHours, double costContribution) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("ALLOCATED_HOURS", allocatedHours);
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
