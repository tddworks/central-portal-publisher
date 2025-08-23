plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
    id("java-gradle-plugin")
    alias(libs.plugins.gradle.plugin.publish)
    id("maven-publish")
    alias(libs.plugins.kover)
    alias(libs.plugins.version.catalog.update)
    alias(libs.plugins.spotless)
    alias(libs.plugins.dependency.analysis)
}

gradlePlugin {
    website = "https://github.com/tddworks/central-portal-publisher"
    vcsUrl = "https://github.com/tddworks/central-portal-publisher"
    plugins {
        create("central-portal-publisher") {
            id = "com.tddworks.central-publisher"
            displayName = "Central Portal Publisher"
            description =
                "Gradle plugin for publishing to Maven Central via Sonatype Central Portal"
            tags = listOf("sonatype", "maven-central", "publishing", "central-portal")
            implementationClass =
                "com.tddworks.sonatype.publish.portal.plugin.CentralPublisherPlugin"
        }
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    // HTTP client dependencies
    implementation(libs.bundles.http.client)
    implementation(libs.kotlinx.serialization.json)

    // Kotlin Multiplatform plugin API (for accessing KotlinMultiplatformExtension)
    compileOnly(libs.kotlin.plugin)

    // Testing dependencies
    testImplementation(libs.bundles.jvm.test)
    testImplementation(libs.kotlin.plugin)
    testImplementation(libs.system.stubs.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(kotlin("test"))
}

group = "com.tddworks"

version = "0.2.1-alpha"

repositories { mavenCentral() }

// Spotless code formatting
spotless {
    kotlin {
        target("**/*.kt")
        ktfmt().kotlinlangStyle()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktfmt().kotlinlangStyle()
    }
}

tasks { test { useJUnitPlatform() } }
