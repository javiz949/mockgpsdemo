plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.mockgpsdemo.hook"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mockgpsdemo.hook"

        // LSPatch itself targets Android 9+.
        minSdk = 28
        targetSdk = 34

        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // IMPORTANT: compileOnly prevents XposedBridge classes from being packaged
    // inside the module APK. LSPatch supplies them at runtime.
    compileOnly("de.robv.android.xposed:api:82")
}
