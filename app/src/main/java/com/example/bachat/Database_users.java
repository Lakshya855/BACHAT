package com.example.bachat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Database_users extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Users.db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_NAME = "users";
    public static final String COL_1 = "ID";
    public static final String COL_2 = "NAME";
    public static final String COL_3 = "EMAIL";
    public static final String COL_4 = "PASSWORD";
    public static final String COL_5 = "GENDER";
    public static final String COL_6 = "POLICY";
    public static final String COL_7 = "STATE";

    public Database_users(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    public static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COL_1 + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_2 + " TEXT, " +
                COL_3 + " TEXT UNIQUE, " +
                COL_4 + " TEXT, " +
                COL_5 + " TEXT, " +
                COL_6 + " TEXT, " +
                COL_7 + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Safe additive migration without dropping user data
        createTables(db);
    }

    public static String hashPassword(String password) {
        if (password == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return password;
        }
    }

    public boolean insertData(String name, String email, String password, String gender, String policy, String state) {
        SQLiteDatabase db = this.getWritableDatabase();
        createTables(db);
        ContentValues contentValues = new ContentValues();

        contentValues.put(COL_2, name != null ? name.trim() : "");
        contentValues.put(COL_3, email != null ? email.trim().toLowerCase() : "");
        contentValues.put(COL_4, hashPassword(password));
        contentValues.put(COL_5, gender);
        contentValues.put(COL_6, policy);
        contentValues.put(COL_7, state);

        long result = db.insertWithOnConflict(TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }

    public boolean checkData(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        createTables(db);
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        String hashed = hashPassword(password);

        // Check both hashed and legacy plain text
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COL_3 + " = ? AND (" + COL_4 + " = ? OR " + COL_4 + " = ?)",
                new String[]{cleanEmail, hashed, password});

        boolean result = cursor.getCount() > 0;
        cursor.close();

        return result;
    }

    public int getUserId(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        createTables(db);
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        String hashed = hashPassword(password);

        Cursor cursor = db.rawQuery("SELECT " + COL_1 + " FROM " + TABLE_NAME + " WHERE " + COL_3 + " = ? AND (" + COL_4 + " = ? OR " + COL_4 + " = ?)",
                new String[]{cleanEmail, hashed, password});
        int id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }
        cursor.close();
        return id;
    }

    public boolean insertGoogleUser(String name, String email, String policy, String state) {
        SQLiteDatabase db = this.getWritableDatabase();
        createTables(db);
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";

        Cursor cursor = db.query(TABLE_NAME, new String[]{COL_1}, COL_3 + " = ?", new String[]{cleanEmail}, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();

        if (exists) {
            return true;
        }

        ContentValues values = new ContentValues();
        values.put(COL_2, name);
        values.put(COL_3, cleanEmail);
        values.putNull(COL_4);
        values.putNull(COL_5);
        values.put(COL_6, policy);
        values.put(COL_7, state);

        long result = db.insert(TABLE_NAME, null, values);
        return result != -1;
    }

    public boolean updateGoogleUserDetails(String email, String password, String gender, String state) {
        SQLiteDatabase db = this.getWritableDatabase();
        createTables(db);
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";

        ContentValues values = new ContentValues();
        values.put(COL_4, hashPassword(password));
        values.put(COL_5, gender);
        values.put(COL_7, state);

        int result = db.update(TABLE_NAME, values, COL_3 + " = ?", new String[]{cleanEmail});
        return result > 0;
    }

    public String getUserState(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        createTables(db);
        Cursor cursor = db.rawQuery("SELECT " + COL_7 + " FROM " + TABLE_NAME + " WHERE " + COL_1 + " = ?", new String[]{String.valueOf(userId)});
        String state = "";
        if (cursor.moveToFirst()) {
            state = cursor.getString(0);
        }
        cursor.close();
        return state != null ? state : "";
    }
}
