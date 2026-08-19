plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.rusty.aurora"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.rusty.aurora"
        // API 26: the floor where NotificationListenerService and the
        // BatteryManager APIs Aurora relies on are stable, and where
        // adaptive launcher icons are supported natively (no legacy
        // fallback assets needed). Aurora only ever runs on the author's
        // own Galaxy A13, so there's no reason to support older devices.
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "4.1"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
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
    implementation(libs.androidx.compose.material.icons.extended)

    // Coil: the single-wallpaper/schedule picker needs to show actual photo
    // thumbnails (from content:// Uris already granted via the Photo
    // Picker) rather than opaque photo IDs - a hand-rolled bitmap loader
    // would just reimplement Coil's caching/lifecycle-awareness worse.
    implementation(libs.coil.compose)

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

    // Plain JUnit4 on the JVM (no Robolectric/instrumentation) - enough to
    // start AuroraHttpServer against fake repositories and hit it over real
    // HTTP, since none of api/ or the repository interfaces touch the
    // Android framework directly.
    testImplementation(libs.junit)
}
