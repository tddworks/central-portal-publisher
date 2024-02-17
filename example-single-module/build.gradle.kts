plugins {
    id("com.tddworks.sonatype-portal-publisher") version "0.0.4"
}

sonatypePortalPublisher {
    authentication {
        username = "some-username"
        password = "some-password"
    }

    autoPublish = true

    modules {
        project(":gpt3")
        project(":gpt3-gradle-plugin")
    }
}