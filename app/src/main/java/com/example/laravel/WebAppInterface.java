package com.example.laravel;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class WebAppInterface {
    private static final String PREF_NAME = "LaravelUser";

    private final Context context;

    public WebAppInterface(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    // Modified to match the requested parameter order: uid, uname, employeeId, token
    @JavascriptInterface
    public void setUserData(String uid, String uname, String employeeId, String token) {
        // 12. Laravel user data received
        Log.d("WEB_APP_DATA", "Laravel user data received");
        // 13. User ID received
        Log.d("WEB_APP_DATA", "User ID received: " + uid);
        // 14. Employee ID received
        Log.d("WEB_APP_DATA", "Employee ID received: " + employeeId);
        // 15. User name received
        Log.d("WEB_APP_DATA", "User name received: " + uname);
        Log.d("WEB_APP_DATA", "Sanctum Token received");

        // Store using the exact requested keys in the existing preferences file
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // Safety check for nulls
        String safeUid = uid != null ? uid : "";
        String safeUname = uname != null ? uname : "";
        String safeEmpId = employeeId != null ? employeeId : "";
        String safeToken = token != null ? token : "";

        preferences.edit()
                .putString("user_id", safeUid)
                .putString("employee_id", safeEmpId)
                .putString("user_name", safeUname)
                .putString("api_token", safeToken) // Save the Sanctum token
                // Keep the old keys as well just in case other parts of the app rely on them
                .putString("userId", safeUid)
                .putString("employeeID", safeEmpId)
                .putString("name", safeUname)
                .apply();

        // 16. User ID successfully saved in SharedPreferences
        Log.d("WEB_APP_DATA", "User ID successfully saved in SharedPreferences: " + safeUid);
        
        // --- START TRACKING & SYNC ON LOGIN ---
        // Instead of starting the service immediately, we check if the user has permission first.
        // If they don't, MainActivity will handle starting it after they grant permission.
        if (context instanceof MainActivity) {
            ((MainActivity) context).checkAndStartLocationService();
        }
    }

    // Overload for backward compatibility (in case Laravel is not updated immediately)
    @JavascriptInterface
    public void setUserData(String uid, String uname, String employeeId) {
        setUserData(uid, uname, employeeId, "");
    }

    // Overload for backward compatibility if Laravel still passes an Integer for ID
    @JavascriptInterface
    public void setUserData(int uid, String uname, String employeeId) {
        setUserData(String.valueOf(uid), uname, employeeId);
    }

    public String getUserId(){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("userId", null);
    }

    public String getEmployeeId(){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("employeeID", null);
    }

    public String getName(){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("name", null);
    }

    public void clearUserData(){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        
        // --- STOP TRACKING & SYNC ON LOGOUT ---
        stopLocationTrackingAndSync();
    }

    public void startLocationTrackingAndSync() {
        Log.d("LOCATION_DEBUG", "Starting Foreground Service and Sync Worker...");

        // 1. Start Foreground Location Service
        Intent serviceIntent = new Intent(context, LocationTrackingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        // 2. Start WorkManager for Background Sync (every 15 mins when connected to internet)
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(LocationSyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "LaravelLocationSync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
        );
    }

    private void stopLocationTrackingAndSync() {
        Log.d("LOCATION_DEBUG", "Stopping Foreground Service and Sync Worker...");

        // 1. Stop Foreground Location Service
        Intent serviceIntent = new Intent(context, LocationTrackingService.class);
        context.stopService(serviceIntent);

        // 2. Stop WorkManager Background Sync
        WorkManager.getInstance(context).cancelUniqueWork("LaravelLocationSync");
    }
}
