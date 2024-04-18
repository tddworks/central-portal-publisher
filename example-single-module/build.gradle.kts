plugins {
    alias(libs.plugins.kotlin)
    id("com.tddworks.central-portal-publisher") version "0.0.2"
    `maven-publish`
    signing
}

sonatypePortalPublisher {
    authentication {
        username = "some-username"
        password = "some-password"
    }
    settings {
        autoPublish = false
    }
}

version = "0.0.1"

group = "com.tddworks"