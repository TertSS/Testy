import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Optional release signing: if keystore.properties exists (or KEYSTORE_* env vars are set),
// the release build is signed with it. On GitHub Actions a keystore can be provided as a
// base64 secret — see .github/workflows/android-release.yml. Otherwise an unsigned release
// APK is produced (assembleRelease still works).
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) FileInputStream(keystorePropsFile).use { load(it) }
}

fun ksProp(key: String): String? =
    keystoreProps.getProperty(key) ?: System.getenv(key.uppercase().replace(".", "_"))

android {
    namespace = "dev.readtracker.lite"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.readtracker.lite"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        resourceConfig += "ru"
    }

    signingConfigs {
        create("releaseConfig") {
            val storePath = ksProp("store_file")
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = ksProp("store_password")
                keyAlias = ksProp("key_alias")
                keyPassword = ksProp("key_password")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false // keep it simple & robust for direct APK distribution
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (ksProp("store_file") != null) {
                signingConfig = signingConfigs.getByName("releaseConfig")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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
        viewBinding = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.fragment:fragment-ktx:1.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("io.coil-kt:coil:2.6.0")
}
