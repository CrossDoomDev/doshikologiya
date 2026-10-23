plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "ru.doshikologiya.nativepreview"
    compileSdk = 35
    defaultConfig {
        // Production package ID must match the existing RuStore installation.
        applicationId = "ru.doshikologiya.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 100
        versionName = "1.1.0"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.maxHeapSize = "2g" }
    }
    buildFeatures { compose = true; buildConfig = true }
    sourceSets.getByName("main").assets.srcDirs("../../data", "../../images")
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.03.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-core")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    // Native SDK, not a WebView or mock banner. Keep Compose and core versions in sync.
    implementation("com.yandex.android:mobileads:8.5.0")
    implementation("com.yandex.android:mobileads-compose:8.5.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
