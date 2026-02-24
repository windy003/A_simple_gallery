plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.imagegallery"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.imagegallery"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "2026/2/25-1"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // Coil - 图片加载
    implementation("io.coil-kt:coil:2.5.0")
    implementation("io.coil-kt:coil-video:2.5.0")

    // PhotoView - 手势缩放
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    // Activity Result API
    implementation("androidx.activity:activity-ktx:1.8.2")
}
