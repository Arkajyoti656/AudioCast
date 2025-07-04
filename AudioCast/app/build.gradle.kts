import org.jetbrains.kotlin.storage.CacheResetOnProcessCanceled.enabled

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)


}

android {
    namespace = "com.audiocast.app"
    compileSdk = 35

        defaultConfig {
            applicationId = "com.audiocast.app"
            minSdk = 31
            targetSdk = 35
            versionCode = 1
            versionName = "1.0"
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
        kotlinOptions {
            jvmTarget = "11"
        }
        buildFeatures {
            compose = true
            viewBinding=true
        }
    }

    dependencies {

        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.ui)
        implementation(libs.androidx.ui.graphics)
        implementation(libs.androidx.ui.tooling.preview)
        implementation(libs.androidx.material3)
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.ui.test.junit4)
        debugImplementation(libs.androidx.ui.tooling)
        debugImplementation(libs.androidx.ui.test.manifest)
        // Jetpack Compose
        implementation(libs.androidx.activity.compose)
        implementation ("androidx.navigation:navigation-compose:2.7.7")
        implementation("com.google.firebase:firebase-auth-ktx")
        implementation("com.google.firebase:firebase-firestore-ktx")
        implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
        implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
        implementation("com.google.firebase:firebase-analytics")
        // Firebase

        implementation (libs.firebase.database.ktx)

        implementation("com.google.firebase:firebase-auth")
        implementation("com.google.firebase:firebase-firestore")

        // WebRTC
        implementation("com.mesibo.api:webrtc:1.0.5")

        // ExoPlayer
        //implementation("com.google.android.exoplayer:exoplayer:2.19.1")
        implementation("androidx.media3:media3-exoplayer:1.7.1")

        // EncryptedSharedPreferences
        implementation("androidx.security:security-crypto:1.1.0-alpha06")
        implementation("androidx.recyclerview:recyclerview:1.3.2")
        // For lifecycle components
        implementation("androidx.lifecycle:lifecycle-process:2.6.2")
        implementation("androidx.lifecycle:lifecycle-common-java8:2.6.2")

// For coroutines
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

        implementation("androidx.appcompat:appcompat:1.6.1") // or latest
    }
