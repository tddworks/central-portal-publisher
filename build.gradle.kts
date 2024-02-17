plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin)
    id("java-gradle-plugin")
    id("maven-publish")
    id("signing")

}

val pluginDescription = "Plugin that helps you publish to the Central Portal (https://central.sonatype.org/)"

gradlePlugin {
    plugins {
        create("sonatype-portal-publisher") {
            id = "com.tddworks.sonatype-portal-publisher"
            implementationClass = "com.tddworks.sonatype.publish.portal.plugin.SonatypePortalPublisherPlugin"
            this.description = pluginDescription
            this.displayName = "sonatype-portal-publisher"
        }
    }
}

group = "com.tddworks"
version = "0.0.1"

tasks {
    test {
        useJUnitPlatform()
    }
}