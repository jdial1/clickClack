plugins {
    alias(libs.plugins.android.application)
    // No kotlin.android plugin: AGP 9 has built-in Kotlin support and rejects it.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services) apply false
}

/**
 * Applied only when the Firebase config is actually present.
 *
 * The google-services plugin hard-fails the build if google-services.json is
 * missing, and this project is checked in without one -- it is per-project and
 * carries a real OAuth client id. Guarding it here means the app builds, runs
 * and is developed against FakeAuthRepository with no Firebase account at all,
 * and starts using the real thing the moment the file is dropped in.
 */
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.ampersand"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ampersand"
        minSdk = 26          // java.time without desugaring
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.identity)

    // Renders the @Preview composables in Android Studio.
    debugImplementation(libs.compose.ui.tooling)
}
