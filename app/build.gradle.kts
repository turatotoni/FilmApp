import java.util.Properties
import java.io.FileInputStream


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.kapt")
}

val apikeyProperties = Properties().apply { //testiram da li radi kad je apikey u apikey.properties
    load(rootProject.file("apikey.properties").inputStream())
}

android {
    //BuildConfig je proradio kad sam dodao ovo i prebacio BUildCOnfigFields u BUildTypes
    buildFeatures {
        buildConfig = true
        dataBinding = true
    }

    namespace = "com.example.filmapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.filmapp"
        minSdk = 28
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
            buildConfigField("String", "TMDB_API_KEY", "\"${apikeyProperties["TMDB_API_KEY"]}\"")
        }
        debug {
            buildConfigField("String", "TMDB_API_KEY", "\"${apikeyProperties["TMDB_API_KEY"]}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage-ktx:21.0.2")
    // Retrofit za API pozive
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    // Glide za učitavanje slika
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Logging za Retrofit (opcionalno)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Potrebni dependencii za viewmodelscope
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    //bottom nav
    implementation("com.google.android.material:material:1.12.0")
    //refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

}