plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.placyd.horrah"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.placyd.horrah"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.06699"

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
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation("androidx.work:work-runtime:2.9.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
