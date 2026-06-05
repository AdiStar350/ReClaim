import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

val secretsProperties = Properties()
val secretsFile = rootProject.file("secrets.properties")
if (secretsFile.exists()) {
    secretsFile.inputStream().use { secretsProperties.load(it) }
}

val googleMapsApiKey =
    secretsProperties.getProperty("GOOGLE_MAPS_API_KEY")
        ?: localProperties.getProperty("MAPS_API_KEY")
        ?: System.getenv("MAPS_API_KEY")
        ?: ""

android {
    namespace = "com.example.reclaim"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.reclaim"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = googleMapsApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.fragment)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.swiperefreshlayout)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Material Design 3
    implementation(libs.material)

    // Retrofit (REST API)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // Glide (Image Loading)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // Google Maps & Location
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // Firebase Storage
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.storage)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
