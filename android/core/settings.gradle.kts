pluginManagement {
    repositories {
        // مرآة Google الرسمية لـ Maven Central (أقل عرضة لحدود الطلبات 429)
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        // مرآة Google الرسمية لـ Maven Central (أقل عرضة لحدود الطلبات 429)
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        mavenCentral()
    }
}

rootProject.name = "core"
