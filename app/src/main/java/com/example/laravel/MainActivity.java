package com.example.laravel;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;



import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;



import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1002;

    private static final int BACKGROUND_LOCATION_REQUEST = 1003;
    private WebView webView;

    private WebAppInterface webAppInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        // initializie WebView
        webView = findViewById(R.id.webView);

        setupWebView();

        webView.loadUrl(getString(R.string.web_url));

        //Ask Location Permission
         requestRequiredPermissions();

    }

    /**
     * Configure WebView
     */
    private void setupWebView() {

        WebSettings settings = webView.getSettings();

        // Enable JavaScript
        settings.setJavaScriptEnabled(true);

        // Enable Local Storage
        settings.setDomStorageEnabled(true);

        // Enable Database
        settings.setDatabaseEnabled(true);

        // Better mobile rendering
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        // Open website inside WebView
        webView.setWebViewClient(new WebViewClient());

        // Enable cookies for Laravel sessions
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);


        /*
         * ---------------------------------------------------------
         * Laravel -> Android JavaScript Bridge
         * ---------------------------------------------------------
         *
         * Laravel can call:
         *
         * Android.setUserData(
         *     userId,
         *     employeeId,
         *     name
         * );
         *
         * The Android object is provided by WebAppInterface.
         */

        webAppInterface = new WebAppInterface(this);

        webView.addJavascriptInterface(
                webAppInterface,
                "Android"
        );
    }

    private void requestRequiredPermissions() {
            // First request location
        requestLocationPermission();

    }

    private void requestLocationPermission() {
        // this is for FINE LOCATION
        boolean fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // this is for COARSE LOCATION
        boolean coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted || !coarseGranted) {
            ActivityCompat.requestPermissions(
                    this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
        else {
            // Foreground Location Already Granted
             continuePermissionFlow();
        }
    }


    private void continuePermissionFlow(){
         requestNotificationPermission();

        //Background Location
        // we Request this separately because Android requires
        // background location to be handled separately

        requestBackgroundLocationPermission();
    }

    // Android 13 +

    private void requestNotificationPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean notificationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            if (!notificationGranted) {
                ActivityCompat.requestPermissions(
                        this, new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            }
        }

    }

    private void requestBackgroundLocationPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            boolean backgroundGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
                if (!backgroundGranted) {
                    ActivityCompat.requestPermissions(
                            this, new String[]{
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            },
                            BACKGROUND_LOCATION_REQUEST
                    );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,@NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean locationGranted = false;
            for(int result : grantResults) {
                if(result == PackageManager.PERMISSION_GRANTED) {
                    locationGranted = true;
                    break;
                }
            }
            if(locationGranted) {
                continuePermissionFlow();
            }
            else {
                // Denied
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            boolean notificationGranted = false;
            for(int result : grantResults) {
                if(result == PackageManager.PERMISSION_GRANTED) {
                    notificationGranted = true;
                    break;
                }
            }

            if(notificationGranted) {
                continuePermissionFlow();
            }
            else {
                // Denied
            }
            
        } else if (requestCode == BACKGROUND_LOCATION_REQUEST) {
            boolean backgroundGranted = false;
            for(int result : grantResults) {
                if(result == PackageManager.PERMISSION_GRANTED) {
                    backgroundGranted = true;
                    break;
                }
            }

            if(backgroundGranted) {
                continuePermissionFlow();
            }
            else {
                // Denied
            }
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed(){
        if (webView != null && webView.canGoBack()){
            webView.goBack();
        }
        else {
            super.onBackPressed();
        }
    }
    @Override
    public void onDestroy(){

        if (webView != null){
            webView.stopLoading();
            webView.setWebViewClient(null);
            webView.destroy();
        }

        super.onDestroy();
    }



}