plugins {
    kotlin("jvm")
    // NOTE: No maven-publish plugin - this module is not published
}

dependencies {
    implementation(project(":core"))
    implementation(project(":api"))
    implementation(kotlin("stdlib"))
    implementation("org.junit.jupiter:junit-jupiter:5.10.0")
    implementation("org.assertj:assertj-core:3.24.2")
    implementation("io.mockk:mockk:1.13.8")
}

// This module provides test utilities for other modules
// It is NOT published to Maven Central