import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Apply the Google Services plugin
    id ("com.google.gms.google-services")
    // Firebase Crashlytics
    id("com.google.firebase.crashlytics")
}

// Load secrets from local.properties (NOT committed to git)
val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}
val fcmApiKey: String = localProperties.getProperty("FCM_API_KEY", "")

// Mappls (MapmyIndia) credentials — from https://apis.mappls.com/console/
val mapplsMapSdkKey: String = localProperties.getProperty("MAPPLS_MAP_SDK_KEY", "")
val mapplsRestApiKey: String = localProperties.getProperty("MAPPLS_REST_API_KEY", "")
val mapplsAtlasClientId: String = localProperties.getProperty("MAPPLS_ATLAS_CLIENT_ID", "")
val mapplsAtlasClientSecret: String = localProperties.getProperty("MAPPLS_ATLAS_CLIENT_SECRET", "")

android {
    namespace = "com.example.workman"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.workman"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Mappls credentials exposed to code via BuildConfig (kept out of source control)
        buildConfigField("String", "MAPPLS_MAP_SDK_KEY", "\"$mapplsMapSdkKey\"")
        buildConfigField("String", "MAPPLS_REST_API_KEY", "\"$mapplsRestApiKey\"")
        buildConfigField("String", "MAPPLS_ATLAS_CLIENT_ID", "\"$mapplsAtlasClientId\"")
        buildConfigField("String", "MAPPLS_ATLAS_CLIENT_SECRET", "\"$mapplsAtlasClientSecret\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "FCM_API_KEY", "\"$fcmApiKey\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "FCM_API_KEY", "\"$fcmApiKey\"")
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
    buildFeatures {
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}


dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2023.08.00")) // Use latest BOM version
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Firebase Firestore and Storage
    implementation(platform("com.google.firebase:firebase-bom:33.7.0")) // Use latest BOM version
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-tasks:18.2.0")
    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("com.android.volley:volley:1.2.1")
    implementation("com.google.android.datatransport:transport-api:3.0.0")
    implementation("androidx.activity:activity:1.9.0")
    implementation("com.google.firebase:firebase-database-ktx:21.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.ink:ink-geometry:1.0.0")

    // Testing Libraries
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // Material Design Components
    implementation ("com.google.android.material:material:1.12.0") // Use latest version

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp — used for Cloudinary image uploads (free image hosting, no Firebase Storage needed)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")


    implementation("com.google.android.play:review:2.0.2")
    implementation("com.google.android.play:review-ktx:2.0.2")

    // Mappls (MapmyIndia) Maps SDK — replaces OpenStreetMap/osmdroid.
    // v8.3.0 is the last release that initializes with API keys via MapplsAccountManager
    // (REST/Map SDK key + Atlas client id/secret) and needs NO Gradle plugin or license
    // file. v9 (BoM 2.0.0) dropped key-based init in favour of an OLF file + plugin.
    implementation("com.mappls.sdk:mappls-android-sdk:8.3.0")
}
