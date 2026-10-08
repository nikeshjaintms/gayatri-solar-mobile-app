package com.example.laravel;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
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

    // To hold the callback when requesting location permissions dynamically
    private String geoOrigin;
    private GeolocationPermissions.Callback geoCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 1. MainActivity started
        Log.d("LOCATION_DEBUG", "MainActivity started");

        // Initialize WebView
        webView = findViewById(R.id.webView);
        setupWebView();
        
        // 2. WebView initialized
        Log.d("LOCATION_DEBUG", "WebView initialized");

        // Request notification permission up front if needed
        requestNotificationPermission();

        // Load Laravel URL
        String webUrl = getString(R.string.web_url);
        // 3. Laravel URL loaded
        Log.d("LOCATION_DEBUG", "Laravel URL loaded: " + webUrl);
        webView.loadUrl(webUrl);
    }

    /**
     * WEBVIEW SETUP
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        Log.d("LOCATION_DEBUG", "Setting up WebView");

        WebSettings settings = webView.getSettings();

        // JavaScript
        settings.setJavaScriptEnabled(true);

        // Local Storage & Database
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // IMPORTANT: Geolocation enabled for WebView
        settings.setGeolocationEnabled(true);

        // Mobile rendering
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        // WebViewClient with page and error logging
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                // 4. WebView page started
                Log.d("WEBVIEW_DEBUG", "WebView page started: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 5. WebView page finished
                Log.d("WEBVIEW_DEBUG", "WebView page finished: " + url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                // 17. Any WebView errors
                String description = error.getDescription().toString();
                Log.e("WEBVIEW_DEBUG", "WebView error: " + description + " for URL: " + request.getUrl());
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                // 17. Any WebView errors
                Log.e("WEBVIEW_DEBUG", "WebView HTTP error code: " + errorResponse.getStatusCode() + " for URL: " + request.getUrl());
            }
        });

        // WebChromeClient for Geolocation permissions prompt
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(
                    String origin,
                    GeolocationPermissions.Callback callback
            ) {
                // 9. WebView geolocation request received
                Log.d("LOCATION_DEBUG", "WebView geolocation request received");
                
                // 10. WebView geolocation origin
                Log.d("LOCATION_DEBUG", "WebView geolocation origin: " + origin);

                boolean fineGranted = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                boolean coarseGranted = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

                // 6. Android location permission status
                Log.d("LOCATION_DEBUG", "Android location permission status - Fine: " + fineGranted + ", Coarse: " + coarseGranted);
                // 7. Fine location permission status
                Log.d("LOCATION_DEBUG", "Fine location permission status: " + fineGranted);
                // 8. Coarse location permission status
                Log.d("LOCATION_DEBUG", "Coarse location permission status: " + coarseGranted);

                if (fineGranted || coarseGranted) {
                    // Allow geolocation permission for the origin
                    callback.invoke(origin, true, false);
                    // 11. WebView geolocation callback invoked
                    Log.d("LOCATION_DEBUG", "WebView geolocation callback invoked (Allowed)");
                } else {
                    // Save callback and origin to invoke after user grants permission
                    geoOrigin = origin;
                    geoCallback = callback;
                    Log.d("LOCATION_DEBUG", "Android permission missing. Requesting permission now...");
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE
                    );
                }
            }
        });

        // Laravel Cookies / Authentication
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // Laravel -> Android JavaScript Bridge
        webAppInterface = new WebAppInterface(this);
        webView.addJavascriptInterface(webAppInterface, "Android");
    }

    /**
     * NOTIFICATION PERMISSION (Android 13+)
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
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            }
        }
    }

    /**
     * PERMISSION RESULT
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
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

            if (fineGranted || coarseGranted) {
                Log.d("LOCATION_DEBUG", "Location permission granted by user.");
                if (geoCallback != null) {
                    geoCallback.invoke(geoOrigin, true, false);
                    Log.d("LOCATION_DEBUG", "WebView geolocation callback invoked (Allowed after user prompt)");
                    geoCallback = null;
                    geoOrigin = null;
                }
            } else {
                // 18. Any permission errors
                Log.e("LOCATION_DEBUG", "Location permission denied by user.");
                if (geoCallback != null) {
                    geoCallback.invoke(geoOrigin, false, false);
                    Log.e("LOCATION_DEBUG", "WebView geolocation callback invoked (Denied)");
                    geoCallback = null;
                    geoOrigin = null;
                }
            }
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.setWebViewClient(null);
            webView.setWebChromeClient(null);
            webView.removeJavascriptInterface("Android");
            webView.destroy();
        }
        super.onDestroy();
    }
}
