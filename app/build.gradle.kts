plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.lendy.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.lendy.app"
        minSdk = 24
        targetSdk = 36
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // room db
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // for managing component such as view model and live data
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // for testing
    testImplementation(libs.mockito.core)
    testImplementation(libs.core.testing)
    androidTestImplementation(libs.core.testing)
    androidTestImplementation(libs.room.testing)

    // chart
    implementation(libs.mpandroidchart)
    implementation(libs.viewpager2)
}