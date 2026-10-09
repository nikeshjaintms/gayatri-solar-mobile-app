package com.example.laravel;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LocationSyncWorker extends Worker {

    // IMPORTANT: Make sure this matches your exact Laravel API endpoint URL
    private static final String API_URL = "https://erp.gayatrisolarenergy.com/api/locations/sync";

    public LocationSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("SYNC_DEBUG", "LocationSyncWorker started.");
        DatabaseHelper db = new DatabaseHelper(getApplicationContext());
        
        JSONArray pendingLocations = db.getUnsyncedLocations();
        int pendingCount = pendingLocations.length();

        if (pendingCount == 0) {
            Log.d("SYNC_DEBUG", "No unsynced locations found. Worker finished.");
            return Result.success();
        }

        Log.d("SYNC_DEBUG", "Found " + pendingCount + " pending locations to sync. Preparing network request...");

        try {
            // Build the JSON payload containing the array of location records
            JSONObject payload = new JSONObject();
            payload.put("locations", pendingLocations);
            String jsonBody = payload.toString();

            // Fetch the Sanctum API Token saved in SharedPreferences
            SharedPreferences prefs = getApplicationContext().getSharedPreferences("LaravelUser", Context.MODE_PRIVATE);
            String apiToken = prefs.getString("api_token", "");

            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));
            
            Request.Builder requestBuilder = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json");

            // Attach the Bearer Token if available
            if (!apiToken.isEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer " + apiToken);
            }

            Request request = requestBuilder.build();
            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                Log.d("SYNC_DEBUG", "Sync successful! HTTP 200/201 received.");
                
                // Only mark as synced IF the server responds successfully
                List<String> syncIds = new ArrayList<>();
                for (int i = 0; i < pendingLocations.length(); i++) {
                    syncIds.add(pendingLocations.getJSONObject(i).getString("sync_id"));
                }
                
                db.markAsSynced(syncIds);
                Log.d("SYNC_DEBUG", "Sync complete. " + syncIds.size() + " records updated in local SQLite.");
                
                return Result.success();
            } else {
                Log.e("SYNC_DEBUG", "Sync failed with HTTP Code: " + response.code());
                String errorBody = response.body() != null ? response.body().string() : "No body";
                Log.e("SYNC_DEBUG", "Error response: " + errorBody);
                return Result.retry(); // Will trigger WorkManager to retry automatically later
            }

        } catch (Exception e) {
            Log.e("SYNC_DEBUG", "Exception during sync process.", e);
            return Result.retry();
        }
    }
}
