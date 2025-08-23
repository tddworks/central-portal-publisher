rootProject.name = "multi-module-example"

// Include submodules
include("core")

include("api")

include("client")

include("test-utils") // Non-publishable module for internal testing

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
