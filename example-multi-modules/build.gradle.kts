plugins {
    id("com.tddworks.central-portal-publisher") version "0.0.2"
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