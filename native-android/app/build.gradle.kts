plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "ru.doshikologiya.nativepreview"
    compileSdk = 35
    defaultConfig {
        // Separate from existing RuStore application: safe side-by-side install.
        applicationId = "ru.doshikologiya.nativepreview"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-native-preview"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            // NOT the production RuStore signing configuration.
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Shared DATA ONLY (no HTML, CSS, JS, Capacitor, or WebView).
    // Android assets: recipes.json, config.json, recipes/*.webp, etc.
    sourceSets.getByName("main").assets.srcDirs("../../data", "../../images")
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.03.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("com.yandex.android:mobileads:8.5.0")
}
