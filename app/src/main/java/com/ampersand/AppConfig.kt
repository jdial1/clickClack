package com.ampersand

/**
 * Build-time configuration: the one place the app asks what this build has.
 *
 * Everything comes from [BuildConfig], which Gradle fills from google-services.json.
 * A constant, not a resource looked up by name, because the release shrinker strips
 * resources nothing references statically.
 */
object AppConfig {

    /** The OAuth web client id. Empty when the build has no Firebase config. */
    val webClientId: String get() = BuildConfig.WEB_CLIENT_ID

    /**
     * Whether this build talks to Firebase. Without a config every repository is
     * the in-memory fake, so the app builds and runs with no Firebase account.
     */
    val hasFirebase: Boolean get() = webClientId.isNotBlank()

    /** "0.1.123", as shown in Settings. */
    val versionName: String get() = BuildConfig.VERSION_NAME

    /** "0.1.123 (release)", for diagnostics. */
    val version: String get() = "$versionName (${BuildConfig.BUILD_TYPE})"
}
