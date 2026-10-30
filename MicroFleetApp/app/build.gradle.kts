plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.microfleetapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.microfleetapp"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// No dependencies on purpose: the app uses only the Android framework, so it builds offline
// once Gradle and the Android plugin are installed.
