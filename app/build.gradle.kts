plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace="com.chirathi.voicebridge"
    compileSdk=34

    defaultConfig {
        applicationId="com.chirathi.voicebridge"
        minSdk=24
        targetSdk=34
        versionCode=1
        versionName="1.0"

        testInstrumentationRunner="androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled=false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility=JavaVersion.VERSION_1_8
        targetCompatibility=JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget="1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.firebase:firebase-firestore:26.0.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}