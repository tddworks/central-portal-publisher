plugins {
    kotlin("jvm")
    `maven-publish` // Opt-in to publishing for this module
}

dependencies {
    api(project(":core")) // API module exposes core functionality
    implementation(kotlin("stdlib"))
}

// Central Publisher plugin auto-configures publishing based on root project settings