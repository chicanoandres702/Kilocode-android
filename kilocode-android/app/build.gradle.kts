plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.kilocode.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kilocode.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.1.1"

        val defaultServerUrl = System.getenv("SERVER_URL") ?: "http://18.191.142.105:3000"
        val sharedSecret = System.getenv("KILO_SHARED_SECRET") ?: ""
        val serverUrlDomain = System.getenv("SERVER_URL_DOMAIN") ?: "10.0.2.2"
        manifestPlaceholders["serverUrlDomain"] = serverUrlDomain

        buildConfigField("String", "DEFAULT_SERVER_URL", "\"$defaultServerUrl\"")
        buildConfigField("String", "KILO_SHARED_SECRET", "\"$sharedSecret\"")
    }

    signingConfigs {
        create("release") {
            val keystoreFile = file("../keystore/release.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "kilocode123"
                keyAlias = System.getenv("KEY_ALIAS") ?: "kilocode"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "kilocode123"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val keystoreFile = file("../keystore/release.keystore")
            signingConfig = if (keystoreFile.exists()) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
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
        buildConfig = true
    }

    lint {
        disable.add("NullSafeMutableLiveData")
        abortOnError = false
        checkReleaseBuilds = true
    }
}

dependencies {
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    // Core
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.activity:activity-compose:1.10.1")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2025.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.9")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON
    implementation("com.google.code.gson:gson:2.11.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // Markdown
    implementation("com.github.jeziellago:compose-markdown:0.5.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.12.0")
}
