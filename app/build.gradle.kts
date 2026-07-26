plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.rusty.aurora"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rusty.aurora"
        // API 26: the floor where NotificationListenerService and the
        // BatteryManager APIs Aurora relies on are stable, and where
        // adaptive launcher icons are supported natively (no legacy
        // fallback assets needed). Aurora only ever runs on the author's
        // own Galaxy A13, so there's no reason to support older devices.
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // kotlinx.serialization: compile-time-safe JSON encoding for the
    // dashboard model, no reflection - a good fit for a small, stable
    // set of API response types.
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // NanoHTTPD: a single-purpose, dependency-free embedded HTTP server.
    // Aurora only needs to answer a handful of GET routes on the LAN, so
    // a full framework (Ktor server, etc.) would be a lot of surface
    // area for no real benefit here.
    implementation(libs.nanohttpd)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
