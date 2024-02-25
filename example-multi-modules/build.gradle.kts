plugins {
    id("com.tddworks.sonatype-portal-publisher") version "0.0.1"
}

sonatypePortalPublisher {
    authentication {
        username = "some-username"
        password = "some-password"
    }
    settings {
        autoPublish = false
        aggregation = true
    }
}

version = "0.0.1"

group = "com.tddworks"