plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.akranka10.campusnavigationapp01"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.akranka10.campusnavigationapp01"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    //my line
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation ("com.google.android.gms:play-services-location:21.1.0")
    implementation ("com.google.android.material:material:1.9.0")
    implementation ("com.google.android.libraries.places:places:4.0.0")




    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}



