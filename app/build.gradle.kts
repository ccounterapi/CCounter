plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

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
        val registrationToken = ((project.findProperty("ccounter.githubRegistrationToken") as String?) ?: "")
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

        buildConfigField("String", "ACCESS_CONTROL_URL", "\"$accessControlUrl\"")
        buildConfigField("String", "GITHUB_REPO_OWNER", "\"$githubRepoOwner\"")
        buildConfigField("String", "GITHUB_REPO_NAME", "\"$githubRepoName\"")
        buildConfigField("String", "GITHUB_REGISTRATION_TOKEN", "\"$registrationToken\"")
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
