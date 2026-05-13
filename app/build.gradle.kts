import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.quizapp_nafis"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.quizapp_nafis"
        minSdk = 24
        targetSdk = 34
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

// Load secrets AFTER android block, before dependencies
val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties()
if (secretsFile.exists()) {
    secrets.load(FileInputStream(secretsFile))
}

// Create BuildConfig fields here
android.defaultConfig.apply {
    buildConfigField("String", "GROQ_API_KEY", "\"${secrets.getProperty("GROQ_API_KEY", "")}\"")
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.google.maps)
    implementation(libs.google.location)
    implementation(libs.glide)
    implementation(libs.okhttp)
}