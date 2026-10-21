package com.example.bachat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;
public class Database_users extends SQLiteOpenHelper{

    private static final String DATABASE_NAME = "Users.db";
    private static final String TABLE_NAME = "users";
    private static final String COL_1 = "ID";
    private static final String COL_2 = "NAME";
    private static final String COL_3 = "EMAIL";
    private static final String COL_4 = "PASSWORD";
    private static final String COL_5 = "GENDER";
    private static final String COL_6 = "POLICY";
    public Database_users(@Nullable Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + "(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME TEXT, EMAIL TEXT, PASSWORD TEXT, GENDER TEXT, POLICY TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(String name,String email, String password, String gender, String policy){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COL_2,name);
        contentValues.put(COL_3,email);
        contentValues.put(COL_4,password);
        contentValues.put(COL_5,gender);
        contentValues.put(COL_6,policy);

        long result = db.insert(TABLE_NAME,null,contentValues);
        return result != -1;
    }

    public boolean checkData(String Email,String Password){
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COL_3 + " = ? AND " + COL_4 + " = ?", new String[]{Email, Password});

        boolean result = cursor.getCount() > 0;
        cursor.close();

        return result;
    }


    public boolean insertGoogleUser(String name, String email, String policy) {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_NAME, new String[]{COL_1}, COL_3 + " = ?", new String[]{email}, null, null, null);

        boolean exists = cursor.moveToFirst();
        cursor.close();

        if (exists) {
            return true;
        }

        ContentValues values = new ContentValues();
        values.put(COL_2, name);
        values.put(COL_3, email);
        values.putNull(COL_4);
        values.putNull(COL_5);
        values.put(COL_6, policy);

        long result = db.insert(TABLE_NAME, null, values);

        return result != -1;
    }


    public boolean updateGoogleUserDetails(String email, String password, String gender) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_4, password);
        values.put(COL_5, gender);

        int result = db.update(TABLE_NAME,values, COL_3 + " = ?", new String[]{email});
        return result > 0;
    }
}
