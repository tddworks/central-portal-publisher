// Import will be available after plugin is applied

plugins {
    kotlin("jvm") version "2.2.0"
    `maven-publish`
    signing
    id("com.tddworks.central-publisher")
}

group = "com.tddworks.example"

version = "1.0.0"

repositories { mavenCentral() }

dependencies {
    implementation(kotlin("stdlib"))

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.test { useJUnitPlatform() }

kotlin { jvmToolchain(17) }
