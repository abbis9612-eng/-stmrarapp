pluginManagement {
    repositories {
        google()
        // مرآة Google الرسمية لـ Maven Central (أقل عرضة لحدود الطلبات 429)
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // مرآة Google الرسمية لـ Maven Central (أقل عرضة لحدود الطلبات 429)
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        mavenCentral()
    }
}

rootProject.name = "Sanad"

// منطق التطبيق (الحسابات، الأكل، التمارين، المدرب المحلي) بناء Kotlin/JVM مستقل
// يُختبر بدون Android SDK: cd core && ../gradlew test
includeBuild("core")
include(":app")
