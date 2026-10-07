import org.gradle.api.tasks.PathSensitivity
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing is optional: F-Droid builds from source and signs with its own key, so the
// build has to succeed with no keystore present. Drop a keystore.properties next to settings.gradle
// (storeFile, storePassword, keyAlias, keyPassword) to sign locally; it is gitignored.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// A delivered file should say what it is without being opened.
base {
    archivesName = "ganjoor-0.4.0"
}

android {
    namespace = "com.ganjoor.android"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ganjoor.android"
        minSdk = 24
        targetSdk = 37
        // Pre-1.0 while the app is still being shaped. versionCode only ever climbs: F-Droid
        // refuses an update that does not, and one changelog file per code lives in
        // fastlane/metadata/android/*/changelogs/.
        versionCode = 6
        versionName = "0.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }

    // The dependency metadata block is signed with a Google key and is not reproducible, so
    // F-Droid rejects APKs that carry it.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// ReleaseDocsTest reads these, so a change to either has to re-run the tests. Without this
// Gradle sees only Kotlin sources, calls the task up to date, and a README that has fallen
// behind the build sails through green.
tasks.withType<Test>().configureEach {
    inputs.file(rootProject.file("README.md")).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(rootProject.file("releases/README.md")).withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
