rootProject.name = "kmp-example"

pluginManagement {
    repositories {
        mavenLocal() // For testing local plugin builds
        gradlePluginPortal()
        mavenCentral()
    }
}

// For testing with local plugin development
includeBuild("../..")