plugins {
    kotlin("jvm")
    `maven-publish` // Opt-in to publishing for this module
}

dependencies {
    implementation(kotlin("stdlib"))
}

// Core module is the foundation - no dependencies on other modules
// Central Publisher plugin auto-configures publishing based on root project settings