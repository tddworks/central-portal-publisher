plugins {
    kotlin("multiplatform") version "2.2.0"
    `maven-publish`
    signing
    id("com.tddworks.central-publisher")
}

group = "com.tddworks.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
    
    // JVM target
    jvm()
    
    // JavaScript target
    js(IR) {
        browser()
        nodejs()
    }
    
    // Native targets
    linuxX64()
    macosX64()
    macosArm64()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            }
        }
        
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }
    }
}

centralPublisher {
    credentials {
        username = project.findProperty("SONATYPE_USERNAME") as String? ?: ""
        password = project.findProperty("SONATYPE_PASSWORD") as String? ?: ""
    }
    
    projectInfo {
        name = "kmp-example"
        description = "A simple Kotlin Multiplatform library example for testing Maven Central publishing"
        url = "https://github.com/tddworks/central-portal-publisher"
        
        license {
            name = "Apache License 2.0"
            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            distribution = "repo"
        }
        
        developer {
            id = "tddworks"
            name = "Developer Name"
            email = "developer@example.com"
        }
        
        scm {
            url = "https://github.com/tddworks/central-portal-publisher"
            connection = "scm:git:git://github.com/tddworks/central-portal-publisher.git"
            developerConnection = "scm:git:ssh://github.com/tddworks/central-portal-publisher.git"
        }
    }
    
    publishing {
        autoPublish = false // Let user manually promote from staging
        dryRun = false
    }
    
    signing {
        // Will be auto-detected from gradle.properties
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}