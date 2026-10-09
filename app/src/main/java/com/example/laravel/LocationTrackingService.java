package com.example.laravel;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.UUID;

public class LocationTrackingService extends Service {

    private static final String CHANNEL_ID = "LocationTrackingChannel";
    private static final int NOTIFICATION_ID = 1005;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseHelper dbHelper;
    private String employeeId;
    private String deviceId;

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new DatabaseHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        createNotificationChannel();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    saveLocationToDatabase(location);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("LOCATION_DEBUG", "Foreground Location Tracking Service Started");

        // Fetch the employeeID saved by WebAppInterface
        SharedPreferences prefs = getSharedPreferences("LaravelUser", Context.MODE_PRIVATE);
        // Look for the newly requested "employee_id" key, fallback to "employeeID" if needed
        employeeId = prefs.getString("employee_id", prefs.getString("employeeID", "unknown"));

        startForeground(NOTIFICATION_ID, getNotification());
        requestLocationUpdates();

        return START_STICKY;
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LOCATION_DEBUG", "Missing location permissions to start background tracking.");
            stopSelf();
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000)
                .setMinUpdateIntervalMillis(10000)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Log.d("LOCATION_DEBUG", "Location updates requested successfully.");
    }

    private void saveLocationToDatabase(Location location) {
        int batteryLevel = getBatteryLevel();
        String syncId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();

        dbHelper.insertLocation(
                employeeId,
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                location.getSpeed(),
                batteryLevel,
                timestamp,
                deviceId,
                syncId
        );
        
        Log.d("LOCATION_DEBUG", "Captured Location -> Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude() + " Pending syncs: " + dbHelper.getPendingCount());
    }

    private int getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent != null) {
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level != -1 && scale != -1) {
                return (int) ((level / (float) scale) * 100);
            }
        }
        return -1;
    }

    private Notification getNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Employee Tracking Active")
                .setContentText("Your location is being tracked for attendance.")
                .setSmallIcon(R.mipmap.ic_launcher) // Fallback to app icon
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Live Location Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("LOCATION_DEBUG", "Foreground Location Tracking Service Stopped");
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
