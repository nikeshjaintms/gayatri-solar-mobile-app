package com.example.laravel;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1002;

    private WebView webView;

    private WebAppInterface webAppInterface;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // -------------------------------------------------
        // Initialize WebView
        // -------------------------------------------------

        webView = findViewById(R.id.webView);

        setupWebView();

        // -------------------------------------------------
        // Load Laravel Website
        // -------------------------------------------------

        webView.loadUrl(
                getString(R.string.web_url)
        );

        // -------------------------------------------------
        // Request permissions
        // -------------------------------------------------

        requestRequiredPermissions();
    }


    /**
     * -----------------------------------------------------
     * WEBVIEW SETUP
     * -----------------------------------------------------
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {

        WebSettings settings = webView.getSettings();

        // JavaScript
        settings.setJavaScriptEnabled(true);

        // Local Storage
        settings.setDomStorageEnabled(true);

        // Database
        settings.setDatabaseEnabled(true);

        // Mobile rendering
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);


        // -------------------------------------------------
        // Normal WebView navigation
        // -------------------------------------------------

        webView.setWebViewClient(
                new WebViewClient()
        );


        // -------------------------------------------------
        // IMPORTANT:
        // Allow Laravel JavaScript geolocation
        // -------------------------------------------------

        webView.setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public void onGeolocationPermissionsShowPrompt(
                            String origin,
                            GeolocationPermissions.Callback callback
                    ) {

                        /*
                         * Allow the website to use the location
                         * after Android location permission
                         * has been granted.
                         */

                        callback.invoke(
                                origin,
                                true,
                                false
                        );
                    }
                }
        );


        // -------------------------------------------------
        // Laravel Cookies / Authentication
        // -------------------------------------------------

        CookieManager cookieManager =
                CookieManager.getInstance();

        cookieManager.setAcceptCookie(true);

        cookieManager.setAcceptThirdPartyCookies(
                webView,
                true
        );


        // -------------------------------------------------
        // Laravel -> Android JavaScript Bridge
        // -------------------------------------------------

        webAppInterface =
                new WebAppInterface(this);

        webView.addJavascriptInterface(
                webAppInterface,
                "Android"
        );
    }


    /**
     * -----------------------------------------------------
     * PERMISSION FLOW
     * -----------------------------------------------------
     */
    private void requestRequiredPermissions() {

        requestLocationPermission();
    }


    /**
     * -----------------------------------------------------
     * LOCATION PERMISSION
     * -----------------------------------------------------
     */
    private void requestLocationPermission() {

        boolean fineGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;


        boolean coarseGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;


        // For precise location we want FINE location.
        if (!fineGranted) {

            ActivityCompat.requestPermissions(
                    this,

                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },

                    LOCATION_PERMISSION_REQUEST_CODE
            );

        } else {

            // Location already granted
            requestNotificationPermission();
        }
    }


    /**
     * -----------------------------------------------------
     * NOTIFICATION PERMISSION
     * Android 13+
     * -----------------------------------------------------
     */
    private void requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            boolean notificationGranted =
                    ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED;


            if (!notificationGranted) {

                ActivityCompat.requestPermissions(
                        this,

                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },

                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );

                return;
            }
        }


        // Permission already granted
        permissionsCompleted();
    }


    /**
     * -----------------------------------------------------
     * ALL REQUIRED PERMISSIONS COMPLETED
     * -----------------------------------------------------
     */
    private void permissionsCompleted() {

        /*
         * WebView location is now allowed.
         *
         * Later, when we implement native background
         * tracking, we will start LocationService here.
         *
         * DO NOT start it yet.
         */
    }


    /**
     * -----------------------------------------------------
     * PERMISSION RESULT
     * -----------------------------------------------------
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );


        // -------------------------------------------------
        // LOCATION
        // -------------------------------------------------

        if (requestCode ==
                LOCATION_PERMISSION_REQUEST_CODE) {


            boolean fineGranted =
                    ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED;


            if (fineGranted) {

                // Location permission successful
                requestNotificationPermission();

            } else {

                // Location permission denied

                // WebView geolocation will not work
            }


            return;
        }


        // -------------------------------------------------
        // NOTIFICATION
        // -------------------------------------------------

        if (requestCode ==
                NOTIFICATION_PERMISSION_REQUEST_CODE) {

            /*
             * Notification permission is not required
             * for WebView geolocation.
             *
             * Even if user denies notification,
             * we can continue.
             */

            permissionsCompleted();

            return;
        }
    }


    /**
     * -----------------------------------------------------
     * BACK BUTTON
     * -----------------------------------------------------
     */
    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {

        if (webView != null &&
                webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }


    /**
     * -----------------------------------------------------
     * DESTROY WEBVIEW
     * -----------------------------------------------------
     */
    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.stopLoading();

            webView.setWebViewClient(null);

            webView.setWebChromeClient(null);

            webView.removeJavascriptInterface(
                    "Android"
            );

            webView.destroy();
        }

        super.onDestroy();
    }
}