rootProject.name = "single-module-example"

pluginManagement {
    repositories {
        mavenLocal() // For testing local plugin builds
        gradlePluginPortal()
        mavenCentral()
    }
}

// For testing with local plugin development
// Uncomment the line below when testing with the plugin source code
includeBuild("../..")
