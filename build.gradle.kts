plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin)
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "1.2.1"
    id("maven-publish")
    alias(libs.plugins.kover)
}

val pluginDescription = "Plugin that helps you publish to the Central Portal (https://central.sonatype.org/)"

gradlePlugin {
    website = "https://github.com/tddworks/sonatype-portal-publisher"
    vcsUrl = "https://github.com/tddworks/sonatype-portal-publisher"
    plugins {
        create("sonatype-portal-publisher") {
            id = "com.tddworks.sonatype-portal-publisher"
            this.displayName = "sonatype-portal-publisher"
            this.description = pluginDescription
            tags = listOf("sonatype", "publish", "portal", "maven-central", "kmp")
            implementationClass = "com.tddworks.sonatype.publish.portal.plugin.SonatypePortalPublisherPlugin"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("com.squareup.okio:okio:3.8.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    testImplementation(libs.bundles.jvm.test)
    testImplementation("uk.org.webcompere:system-stubs-jupiter:2.1.6")
}

group = "com.tddworks"
version = "0.0.1"

tasks {
    test {
        useJUnitPlatform()
    }
}