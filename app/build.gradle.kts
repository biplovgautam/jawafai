import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.example.jawafai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.jawafai"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${localProperties.getProperty("cloudinary.cloudName")}\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"${localProperties.getProperty("cloudinary.apiKey")}\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET", "\"${localProperties.getProperty("cloudinary.apiSecret")}\"")
        buildConfigField("String", "GROQ_API_KEY", "\"${localProperties.getProperty("groq.apiKey")}\"")
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
        buildConfig = true
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    // AndroidX Core and UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.compose.runtime:runtime-livedata:1.5.4") // Add this line
    implementation("androidx.compose.material:material-icons-extended")

    // Pull-to-refresh for Compose
    implementation("androidx.compose.material:material:1.5.4")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0")

    // Pull-to-refresh for Material 3
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.foundation:foundation:1.5.4")

    // Animation libraries for smooth transitions
    implementation("androidx.compose.animation:animation:1.5.4")
    implementation("androidx.compose.animation:animation-core:1.5.4")
    implementation("androidx.compose.animation:animation-graphics:1.5.4")

    // Lifecycle components for ProcessLifecycleOwner
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    implementation("androidx.lifecycle:lifecycle-common:2.7.0")

    // Lottie Animation for Compose
    implementation("com.airbnb.android:lottie-compose:6.1.0")

    // Coil for image loading in Compose
    implementation("io.coil-kt:coil-compose:2.4.0")

    // Firebase - Use BoM to manage versions
    implementation(platform(libs.firebase.bom))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Glide for image loading in notifications
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Cloudinary
    implementation("com.cloudinary:cloudinary-android:2.4.0")

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // OkHttp for API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("androidx.appcompat:appcompat:1.6.1")
}