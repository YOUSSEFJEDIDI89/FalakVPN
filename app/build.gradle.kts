plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Signing is optional: when no keystore is present (for example on CI) the
// release build is produced unsigned instead of failing the whole pipeline.
// Provide FALAK_KEYSTORE / FALAK_STORE_PASSWORD / FALAK_KEY_ALIAS /
// FALAK_KEY_PASSWORD (or edit the defaults below) to sign a build.
val keystorePath = System.getenv("FALAK_KEYSTORE") ?: "../falak-release.jks"
val signStorePass = System.getenv("FALAK_STORE_PASSWORD") ?: "falakvpn2026"
val signKeyAlias = System.getenv("FALAK_KEY_ALIAS") ?: "falak"
val signKeyPass = System.getenv("FALAK_KEY_PASSWORD") ?: signStorePass
val keystoreFile = file(keystorePath)

android {
    namespace = "app.falak.vpn"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.falak.vpn"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (keystoreFile.exists()) {
            create("release") {
                storeFile = keystoreFile
                storePassword = signStorePass
                keyAlias = signKeyAlias
                keyPassword = signKeyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
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
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.ui:ui-graphics:1.6.8")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.webkit:webkit:1.11.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
}
