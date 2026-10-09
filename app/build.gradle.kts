plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.laravel"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.laravel"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    
    // Dependencies for Background Location & API Sync
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("androidx.work:work-runtime:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}