// Imports will be available after plugin is applied

plugins {
    kotlin("jvm") version "1.9.23" apply false
    `maven-publish`
    signing
    id("com.tddworks.sonatype-portal-publisher") version "0.0.6"
}

allprojects {
    group = "com.example.multimodule"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    
    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(11)
    }
    
    dependencies {
        "implementation"(kotlin("stdlib"))
        "testImplementation"(kotlin("test"))
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.10.0")
        "testImplementation"("org.assertj:assertj-core:3.24.2")
    }
    
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

// Central Portal Publisher Configuration for Multi-Module Project
centralPublisher {
    // Project information - applied to all modules
    projectInfo {
        // Root project info - inherited by submodules
        name = rootProject.name
        description = "A multi-module Kotlin library demonstrating the Central Portal Publisher"
        url = "https://github.com/example/multi-module-kotlin"
        
        scm {
            url = "https://github.com/example/multi-module-kotlin"
            connection = "scm:git:git://github.com/example/multi-module-kotlin.git"
            developerConnection = "scm:git:ssh://github.com/example/multi-module-kotlin.git"
        }
        
        license {
            name = "Apache License 2.0"
            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            distribution = "repo"
        }
        
        developers {
            developer {
                id = "team-lead"
                name = "Team Lead"
                email = "team@example.com"
            }
        }
    }
    
    // Credentials configuration
    credentials {
        username = project.findProperty("SONATYPE_USERNAME")?.toString() ?: ""
        password = project.findProperty("SONATYPE_PASSWORD")?.toString() ?: ""
    }
    
    // Signing configuration
    signing {
        keyId = project.findProperty("SIGNING_KEY_ID")?.toString() ?: ""
        password = project.findProperty("SIGNING_PASSWORD")?.toString() ?: ""
        secretKeyRingFile = project.findProperty("SIGNING_SECRET_KEY_RING_FILE")?.toString() 
            ?: "${System.getProperty("user.home")}/.gnupg/secring.gpg"
    }
    
    // Publishing options
    publishing {
        autoPublish = false
        aggregation = true
        dryRun = false
    }
}

// Multi-module publishing configuration
// The plugin automatically detects publishable modules (those with maven-publish plugin)
// Only core, api, and client modules will be published since test-utils doesn't have maven-publish

// Task to demonstrate module detection
tasks.register("detectModules") {
    group = "Central Publishing"
    description = "Detect and display multi-module project structure"
    doLast {
        println("🔍 Detecting module structure...")
        println("Root project: ${rootProject.name}")
        println("\nSubmodules:")
        subprojects.forEach { subproject ->
            val hasPublishing = subproject.plugins.hasPlugin("maven-publish")
            val status = if (hasPublishing) "✓ Publishable" else "✗ Non-publishable"
            println("  - ${subproject.name}: $status")
        }
    }
}

// Task to validate multi-module publishing configuration
tasks.register("validateMultiModulePublishing") {
    group = "Central Publishing"
    description = "Validate multi-module publishing configuration"
    doLast {
        println("✓ Multi-module publishing configuration:")
        println("  Root: ${rootProject.name} v${project.version}")
        println("  Modules to publish:")
        subprojects.filter { it.plugins.hasPlugin("maven-publish") }.forEach {
            println("    - ${it.name}")
        }
    }
}

// Simplified task for publishing all modules
tasks.register("publishAllModules") {
    group = "Central Publishing"
    description = "Publish all configured modules to Central Portal"
    dependsOn(subprojects.mapNotNull { 
        it.tasks.findByName("publishAllPublicationsToCentralPortal")
    })
}