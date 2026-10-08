package com.example.laravel;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.webkit.JavascriptInterface;

import androidx.annotation.NonNull;

public class WebAppInterface {
    private static final String PREF_NAME = "LaravelUser";

    private final Context context;

    public WebAppInterface(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @JavascriptInterface
    public void setUserData(String userId, String employeeId, String name) {
        // 12. Laravel user data received
        Log.d("WEB_APP_DATA", "Laravel user data received");
        // 13. User ID received
        Log.d("WEB_APP_DATA", "User ID received: " + userId);
        // 14. Employee ID received
        Log.d("WEB_APP_DATA", "Employee ID received: " + employeeId);
        // 15. User name received
        Log.d("WEB_APP_DATA", "User name received: " + name);

        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        preferences.edit()
                .putString("userId", userId)
                .putString("employeeID", employeeId)
                .putString("name", name)
                .apply();

        // 16. User ID successfully saved in SharedPreferences
        Log.d("WEB_APP_DATA", "User ID successfully saved in SharedPreferences: " + userId);
    }
    
    // Fallback if Laravel sends userId as an integer/Number (e.g. auth()->id() is an int)
    @JavascriptInterface
    public void setUserData(int userId, String employeeId, String name) {
        // Reroute to the main method, converting the integer to a String
        setUserData(String.valueOf(userId), employeeId, name);
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
    }
}
