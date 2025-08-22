plugins {
    kotlin("jvm")
    `maven-publish` // Opt-in to publishing for this module
}

dependencies {
    api(project(":api")) // Client module provides a high-level interface to the API
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

// Central Publisher plugin auto-configures publishing based on root project settings