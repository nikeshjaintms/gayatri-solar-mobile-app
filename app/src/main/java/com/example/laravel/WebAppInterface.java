package com.example.laravel;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

public class WebAppInterface {
    private static final String PREF_NAME = "LaravelUser";

    private final Context context;

    public WebAppInterface(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public void setUserData(String userId, String employeeID, String name){
        Log.d("WEB_APP_DATA", "User ID: " + userId + ", Employee ID: " + employeeID + ", Name: " + name);
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        preferences.edit()
                .putString("userId", userId)
                .putString("employeeID", employeeID)
                .putString("name", name)
                .apply();

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
