plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.quantummessenger"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.quantummessenger"
        minSdk = 26
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    // 1. КРИПТОГРАФИЯ (Ваша работает)
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.77")

    // 2. TOR (ОФИЦИАЛЬНЫЙ СБОРНИК)

    // Бинарные файлы Tor для Android (самые свежие)
    implementation("info.guardianproject:tor-android:0.4.9.5")

    // Тот самый контроллер, который у вас не качался (новое имя)
    implementation("info.guardianproject:jtorctl:0.4.5.7")

    // Простой HTTP-клиент, чтобы проверять соединение
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Корутины
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}