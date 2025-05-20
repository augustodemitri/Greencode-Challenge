import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.greencodechallenge"
    compileSdk = 34

    val properties = Properties()
    properties.load(project.rootProject.file("local.properties").inputStream())

    defaultConfig {
        applicationId = "com.example.greencodechallenge"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        resValue("string", "BASE_URL", properties.getProperty("BASE_URL", ""))
        resValue("string", "API_KEY", properties.getProperty("API_KEY", ""))
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
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    // Retrofit
    implementation(libs.retrofit)
    // Gson
    implementation(libs.converter.gson)
    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    //Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    // ViewModel y LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    // RecyclerView
    implementation(libs.androidx.recyclerview)
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    // Hilt Navigation
    implementation(libs.androidx.hilt.navigation.fragment)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}