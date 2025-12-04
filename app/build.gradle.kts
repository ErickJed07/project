import java.util.Properties

// 1. Load local.properties securely
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.project"
    compileSdk = 35

    // ↓↓↓ ADD THIS ENTIRE BLOCK ↓↓↓
    signingConfigs {
        create("release") {
            // Read signing properties from local.properties
            val keystoreFile = localProperties.getProperty("keystore.file")
            if (keystoreFile != null && rootProject.file(keystoreFile).exists()) {
                storeFile = rootProject.file(keystoreFile)
                storePassword = localProperties.getProperty("keystore.password")
                keyAlias = localProperties.getProperty("key.alias")
                keyPassword = localProperties.getProperty("key.password")
            }
        }
    }
    // ↑↑↑ END OF NEW BLOCK ↑↑↑

    defaultConfig {
        applicationId = "com.example.project"
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // 2. Inject Cloudinary credentials into BuildConfig
        // These values must exist in your local.properties file
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${localProperties.getProperty("CLOUDINARY_CLOUD_NAME")}\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"${localProperties.getProperty("CLOUDINARY_API_KEY")}\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET", "\"${localProperties.getProperty("CLOUDINARY_API_SECRET")}\"")
    }

    // 3. Enable the BuildConfig feature so the generated class is created
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // ↓↓↓ APPLY THE SIGNING CONFIG TO THE RELEASE BUILD ↓↓↓
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

// AndroidX + UI Libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.gridlayout)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)
    implementation(libs.cardview)
    implementation(libs.exifinterface)
    implementation(libs.material.v1120)



    implementation(libs.appcompat)
    implementation(libs.leanback.grid)
    implementation(libs.volley)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)


    implementation (libs.volley)


    // Firebase (using BoM to sync versions)
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.google.firebase.auth)
    implementation(libs.google.firebase.database)
    implementation("com.google.firebase:firebase-storage")
    implementation(libs.firebase.firestore)

    // CameraX Dependencies
    implementation("androidx.camera:camera-camera2:1.4.0-alpha02")
    implementation("androidx.camera:camera-lifecycle:1.4.0-alpha02")
    implementation("androidx.camera:camera-view:1.4.0-alpha02")

    // Image Loading Libraries (Glide or Picasso)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.car.ui.lib)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.squareup.picasso:picasso:2.8")

    implementation("com.cloudinary:cloudinary-android:2.3.1")

    // UI extras
    implementation(libs.dotsindicator)
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation ("com.google.mlkit:image-labeling:17.0.7")
    implementation ("androidx.palette:palette:1.0.0")



}
