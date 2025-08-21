// Import will be available after plugin is applied

plugins {
    kotlin("jvm") version "2.2.0"
    `maven-publish`
    signing
    id("com.tddworks.central-publisher")
}

group = "com.tddworks.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    
    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

// Central Portal Publisher Configuration
// This demonstrates the new simplified DSL with smart defaults
centralPublisher {
    // Project information - many fields auto-detected from git and project
    projectInfo {
        name = project.name // Auto-detected from project
        description = "A simple single-module Kotlin library example"
        url = "https://github.com/tddworks/central-portal-publisher" // Can be auto-detected from git
        
        // SCM information - auto-detected from git if available
        scm {
            url = "https://github.com/tddworks/central-portal-publisher"
            connection = "scm:git:git://github.com/tddworks/central-portal-publisher.git"
            developerConnection = "scm:git:ssh://github.com/tddworks/central-portal-publisher.git"
        }
        
        // License information
        license {
            name = "Apache License 2.0"
            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            distribution = "repo"
        }
        
        // Developer information - can be auto-detected from git config
        developer {
            id = "tddworks"
            name = "Developer Name"
            email = "developer@example.com"
        }
    }
    
    // Credentials - typically from gradle.properties or environment variables
    credentials {
        // These would normally come from gradle.properties:
        // SONATYPE_USERNAME=your-username
        // SONATYPE_PASSWORD=your-password
        username = project.findProperty("SONATYPE_USERNAME")?.toString() ?: ""
        password = project.findProperty("SONATYPE_PASSWORD")?.toString() ?: ""
    }
    
    // Signing configuration
    signing {
        // These would normally come from gradle.properties:
        // SIGNING_KEY_ID=your-key-id
        // SIGNING_PASSWORD=your-signing-password
        // SIGNING_SECRET_KEY_RING_FILE=/path/to/secring.gpg
        keyId = project.findProperty("SIGNING_KEY_ID")?.toString() ?: ""
        password = project.findProperty("SIGNING_PASSWORD")?.toString() ?: ""
        secretKeyRingFile = project.findProperty("SIGNING_SECRET_KEY_RING_FILE")?.toString() 
            ?: "${System.getProperty("user.home")}/.gnupg/secring.gpg"
    }
    
    // Publishing options
    publishing {
        autoPublish = false // Manual approval by default (safe)
        aggregation = true  // Standard default
        dryRun = false     // Set to true to test without actually publishing
    }
}

// ✅ PUBLICATIONS AUTO-CONFIGURED!
// The plugin automatically:
// - Applies maven-publish plugin
// - Creates Maven publication with sources/javadoc JARs  
// - Populates POM metadata from centralPublisher configuration above
// - Configures signing from signing configuration
//
// No manual publication setup required!
//
// Available tasks:
// - './gradlew publishToCentral' - Publish to Maven Central
// - './gradlew bundleArtifacts' - Create deployment bundle
// - './gradlew validatePublishing' - Validate configuration
// - './gradlew setupPublishing' - Interactive setup wizard