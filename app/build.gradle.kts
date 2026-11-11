plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.rpghqconspiration"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.rpghqconspiration"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
}