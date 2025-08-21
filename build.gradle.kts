plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "1.2.1"
    id("maven-publish")
    alias(libs.plugins.kover)
}

val pluginDescription = "Plugin that helps you publish to the Central Portal (https://central.sonatype.org/)"

gradlePlugin {
    website = "https://github.com/tddworks/central-portal-publisher"
    vcsUrl = "https://github.com/tddworks/central-portal-publisher"
    plugins {
        // New modern plugin with simplified DSL
        create("central-publisher") {
            id = "com.tddworks.central-publisher"
            this.displayName = "Central Publisher"
            this.description = "Modern Maven Central publishing with type-safe DSL and auto-detection"
            tags = listOf("sonatype", "publish", "maven-central", "dsl", "auto-detection")
            implementationClass = "com.tddworks.sonatype.publish.portal.plugin.CentralPublisherPlugin"
        }

        // Legacy plugin for backward compatibility
        create("central-portal-publisher") {
            id = "com.tddworks.central-portal-publisher"
            this.displayName = "central-portal-publisher (legacy)"
            this.description = "$pluginDescription [LEGACY - Consider migrating to com.tddworks.central-publisher]"
            tags = listOf("sonatype", "publish", "portal", "maven-central", "kmp")
            implementationClass = "com.tddworks.sonatype.publish.portal.plugin.SonatypePortalPublisherPlugin"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("com.squareup.okio:okio:3.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation(libs.kotlinx.serialization.json)
    // Kotlin Multiplatform plugin API (for accessing KotlinMultiplatformExtension)
    compileOnly(libs.kotlin.plugin)

    testImplementation(libs.bundles.jvm.test)
    testImplementation("uk.org.webcompere:system-stubs-jupiter:2.1.6")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

group = "com.tddworks"
version = "0.0.6"
repositories {
    mavenCentral()
}

tasks {
    test {
        useJUnitPlatform()
    }
}