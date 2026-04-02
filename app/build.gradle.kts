import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { stream -> load(stream) }
    }
}

fun resolveConfigValue(
    key: String,
    localProps: Properties,
    envKey: String? = null,
): String {
    return (
        (project.findProperty(key) as String?)
            ?: localProps.getProperty(key)
            ?: envKey?.let { System.getenv(it) }
            ?: ""
        ).trim()
}

val releaseStoreFilePath = resolveConfigValue(
    key = "ccounter.release.storeFile",
    localProps = localProps,
    envKey = "CCOUNTER_RELEASE_STORE_FILE",
)
val releaseStorePassword = resolveConfigValue(
    key = "ccounter.release.storePassword",
    localProps = localProps,
    envKey = "CCOUNTER_RELEASE_STORE_PASSWORD",
)
val releaseKeyAlias = resolveConfigValue(
    key = "ccounter.release.keyAlias",
    localProps = localProps,
    envKey = "CCOUNTER_RELEASE_KEY_ALIAS",
)
val releaseKeyPassword = resolveConfigValue(
    key = "ccounter.release.keyPassword",
    localProps = localProps,
    envKey = "CCOUNTER_RELEASE_KEY_PASSWORD",
)
val hasReleaseSigning = releaseStoreFilePath.isNotBlank() &&
    releaseStorePassword.isNotBlank() &&
    releaseKeyAlias.isNotBlank() &&
    releaseKeyPassword.isNotBlank()

android {
    namespace = "com.example.ccounter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ccounter"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val accessControlUrl = "https://raw.githubusercontent.com/ccounterapi/CCounter/main/admin/devices.json"
        val githubRepoOwner = "ccounterapi"
        val githubRepoName = "CCounter"
        val registrationToken = (
            (project.findProperty("ccounter.githubRegistrationToken") as String?)
                ?: localProps.getProperty("ccounter.githubRegistrationToken")
                ?: System.getenv("CCOUNTER_GITHUB_REGISTRATION_TOKEN")
                ?: ""
            )
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

        buildConfigField("String", "ACCESS_CONTROL_URL", "\"$accessControlUrl\"")
        buildConfigField("String", "GITHUB_REPO_OWNER", "\"$githubRepoOwner\"")
        buildConfigField("String", "GITHUB_REPO_NAME", "\"$githubRepoName\"")
        buildConfigField("String", "GITHUB_REGISTRATION_TOKEN", "\"$registrationToken\"")
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(releaseStoreFilePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.material)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
