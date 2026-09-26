plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val runNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "app.sanad.coach"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.sanad.coach"
        minSdk = 26
        targetSdk = 36
        // رقم البناء من GitHub Actions حتى يتثبت كل إصدار فوق اللي قبله
        versionCode = runNumber
        versionName = "2.0.$runNumber"
        // المدرب السحابي: عنوان السيرفر ومفتاح التطبيق من أسرار CI (فارغ = المدرب المحلي أو مفتاح المستخدم)
        buildConfigField("String", "SANAD_API_URL", "\"${System.getenv("SANAD_API_URL").orEmpty()}\"")
        buildConfigField("String", "SANAD_APP_KEY", "\"${System.getenv("SANAD_APP_KEY").orEmpty()}\"")
    }

    signingConfigs {
        // مفتاح تجريبي ثابت؛ للنشر على Google Play تُمرَّر أسرار GitHub (ANDROID_KEYSTORE_*)
        create("release") {
            storeFile = file(System.getenv("ANDROID_KEYSTORE_PATH") ?: "sanad-dev.keystore")
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")?.ifEmpty { null } ?: "android"
            keyAlias = System.getenv("ANDROID_KEY_ALIAS")?.ifEmpty { null } ?: "sanad"
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD")?.ifEmpty { null } ?: "android"
        }
    }

    buildTypes {
        getByName("release") {
            // R8 مطفأ حتى نغطيه بلقطات على نسخة الإصدار نفسها
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation("app.sanad:core:1.0")
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.serialization.json)
    implementation(libs.coroutines.android)
    implementation(libs.okhttp)
    debugImplementation(libs.compose.ui.tooling)
}
