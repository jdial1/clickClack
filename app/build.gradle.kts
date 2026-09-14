import java.time.LocalDateTime
import java.util.Properties
import java.time.format.DateTimeFormatter

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

/**
 * The OAuth **web** client id, read out of google-services.json at build time.
 *
 * It used to be fetched with Resources.getIdentifier, because the resource only
 * exists once a Firebase config is present and R.string would not compile
 * without one. That indirection is invisible to static analysis, so
 * `shrinkResources` stripped the string and the release build failed sign-in
 * with "No default_web_client_id" on a build that had one. A `keep.xml` did not
 * rescue it.
 *
 * Baking it into BuildConfig removes the problem rather than working around it:
 * the value is a constant the shrinker can see, and an absent config yields an
 * empty string rather than a missing symbol.
 */
fun webClientId(): String {
    val config = file("google-services.json")
    if (!config.exists()) return ""
    @Suppress("UNCHECKED_CAST")
    val root = groovy.json.JsonSlurper().parse(config) as Map<String, Any?>
    val client = (root["client"] as? List<*>)?.firstOrNull() as? Map<*, *> ?: return ""
    val clients = client["oauth_client"] as? List<*> ?: return ""
    return clients.filterIsInstance<Map<*, *>>()
        // client_type 3 is the web client. The Android one (type 1) is bound to
        // the signing certificate and fails here with a token-audience error
        // that says nothing useful.
        .firstOrNull { (it["client_type"] as? Number)?.toInt() == 3 }
        ?.get("client_id") as? String
        ?: ""
}

/**
 * The Google Play upload key, read from keystore.properties (gitignored). It is
 * the same upload key as Chronicle Cryptogram; Play allows one upload key for
 * many apps and re-signs each with its own app signing key.
 */
val uploadKey = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

val commitCount: Int = providers.exec { commandLine("git", "rev-list", "--count", "HEAD") }
    .standardOutput.asText.get().trim().toIntOrNull() ?: 1

android {
    namespace = "com.ampersand"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ampersand"
        minSdk = 26          // java.time without desugaring
        targetSdk = 37
        // Counted from the commits, so every build of a newer commit installs
        // as an upgrade and says which one it is. Read through providers.exec
        // so the configuration cache notices a new commit.
        versionCode = commitCount
        versionName = "0.1.$commitCount"
        buildConfigField("String", "WEB_CLIENT_ID", "\"${webClientId()}\"")
    }

    signingConfigs {
        if (uploadKey.getProperty("storeFile") != null) {
            create("upload") {
                storeFile = file(uploadKey.getProperty("storeFile"))
                storePassword = uploadKey.getProperty("storePassword")
                keyAlias = uploadKey.getProperty("keyAlias")
                keyPassword = uploadKey.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 plus resource shrinking. Worth doing before shipping anything:
            // a debug APK of this project is ~19MB and **92% of that is dex**,
            // not art -- Firebase, Firestore, Play Services and Compose, none
            // of it shrunk. The brick pack is under 100KB.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Debug-signed on purpose: this is a build for testing on real
            // phones, and the debug key is the one registered with Firebase.
            // A release key would need its own SHA-1 adding before Google
            // sign-in would work at all.
            signingConfig = signingConfigs.getByName("debug")
        }
        // The store build: release, signed with the upload key.
        //   gradlew bundlePlay  ->  app/build/outputs/bundle/play/clickclack-<version>-play.aab
        // Google sign-in on a Play install needs Play's app signing SHA-1 added to Firebase.
        create("play") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.findByName("upload")
            matchingFallbacks += "release"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // Screenshots only when asked: gradlew testDebugUnitTest -Pshots
            all {
                it.systemProperty("roborazzi.test.record", "true")
                if (project.hasProperty("shots")) it.systemProperty("shots", "true")
            }
        }
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
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.identity)

    // Renders the @Preview composables in Android Studio.
    debugImplementation(libs.compose.ui.tooling)

    // The published scoring keys are transcribed by hand and a sign error in
    // one of them is invisible on screen -- a wrong letter still looks like a
    // letter. That is what there is a JVM test source set for.
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

// Each APK is also copied under a name stamped with the version and build
// time, e.g. clickclack-0.1.0-20260913-1150-release.apk, so a file sent to a
// phone does not land on top of the last one called app-release.apk.
listOf("debug", "release").forEach { type ->
    val cap = type.replaceFirstChar { it.uppercase() }
    val stamp = tasks.register("stamp${cap}Apk") {
        val dir = layout.buildDirectory.dir("outputs/apk/$type")
        val version = android.defaultConfig.versionName
        doLast {
            val out = dir.get().asFile
            val time = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
            out.resolve("app-$type.apk").copyTo(out.resolve("clickclack-$version-$time-$type.apk"), overwrite = true)
        }
    }
    tasks.matching { it.name == "assemble$cap" }.configureEach { finalizedBy(stamp) }
}

val stampBundle = tasks.register("stampPlayBundle") {
    val dir = layout.buildDirectory.dir("outputs/bundle/play")
    val version = android.defaultConfig.versionName
    doLast { dir.get().asFile.let { it.resolve("app-play.aab").copyTo(it.resolve("clickclack-$version-play.aab"), overwrite = true) } }
}
tasks.matching { it.name == "bundlePlay" }.configureEach { finalizedBy(stampBundle) }
