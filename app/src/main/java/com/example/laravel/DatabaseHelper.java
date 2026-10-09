package com.example.laravel;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "location_tracker.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "employee_locations";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "employee_id TEXT, " +
                "latitude REAL, " +
                "longitude REAL, " +
                "accuracy REAL, " +
                "speed REAL, " +
                "battery_level INTEGER, " +
                "tracked_at INTEGER, " +
                "device_id TEXT, " +
                "sync_id TEXT UNIQUE, " +
                "synced INTEGER DEFAULT 0, " +
                "created_at INTEGER)";
        db.execSQL(createTable);
        Log.d("SQLITE_DEBUG", "Database and table created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void insertLocation(String employeeId, double lat, double lng, float accuracy, float speed, int battery, long trackedAt, String deviceId, String syncId) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        db.beginTransaction(); // <-- START TRANSACTION
        try {
            ContentValues values = new ContentValues();
            values.put("employee_id", employeeId);
            values.put("latitude", lat);
            values.put("longitude", lng);
            values.put("accuracy", accuracy);
            values.put("speed", speed);
            values.put("battery_level", battery);
            values.put("tracked_at", trackedAt);
            values.put("device_id", deviceId);
            values.put("sync_id", syncId);
            values.put("synced", 0);
            values.put("created_at", System.currentTimeMillis());

            long result = db.insert(TABLE_NAME, null, values);
            
            if (result != -1) {
                db.setTransactionSuccessful(); // <-- COMMIT TRANSACTION IF SUCCESSFUL
                Log.d("SQLITE_DEBUG", "Successfully inserted location. DB ID: " + result + " | SyncID: " + syncId);
            } else {
                Log.e("SQLITE_DEBUG", "Failed to insert location record: " + syncId);
            }
        } finally {
            db.endTransaction(); // <-- END TRANSACTION (Rolls back if setTransactionSuccessful() wasn't called)
            db.close();
        }
    }

    public JSONArray getUnsyncedLocations() {
        SQLiteDatabase db = this.getReadableDatabase();
        JSONArray jsonArray = new JSONArray();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE synced = 0", null);

        if (cursor.moveToFirst()) {
            do {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("employee_id", cursor.getString(cursor.getColumnIndexOrThrow("employee_id")));
                    obj.put("latitude", cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")));
                    obj.put("longitude", cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")));
                    obj.put("accuracy", cursor.getFloat(cursor.getColumnIndexOrThrow("accuracy")));
                    obj.put("speed", cursor.getFloat(cursor.getColumnIndexOrThrow("speed")));
                    obj.put("battery_level", cursor.getInt(cursor.getColumnIndexOrThrow("battery_level")));
                    
                    // Laravel expects a valid date format (Y-m-d H:i:s), not a Unix timestamp (long integer).
                    // Convert the stored millisecond timestamp into a SQL-friendly string.
                    long trackedAtMs = cursor.getLong(cursor.getColumnIndexOrThrow("tracked_at"));
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                    String formattedDate = sdf.format(new java.util.Date(trackedAtMs));
                    obj.put("tracked_at", formattedDate);
                    
                    obj.put("device_id", cursor.getString(cursor.getColumnIndexOrThrow("device_id")));
                    obj.put("sync_id", cursor.getString(cursor.getColumnIndexOrThrow("sync_id")));
                    jsonArray.put(obj);
                } catch (JSONException e) {
                    Log.e("SQLITE_DEBUG", "Error building JSON for sync", e);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return jsonArray;
    }

    public void markAsSynced(List<String> syncIds) {
        if (syncIds == null || syncIds.isEmpty()) return;

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (String syncId : syncIds) {
                ContentValues values = new ContentValues();
                values.put("synced", 1);
                db.update(TABLE_NAME, values, "sync_id = ?", new String[]{syncId});
            }
            db.setTransactionSuccessful();
            Log.d("SQLITE_DEBUG", "Marked " + syncIds.size() + " records as synced (synced=1)");
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public int getPendingCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE synced = 0", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }
}
