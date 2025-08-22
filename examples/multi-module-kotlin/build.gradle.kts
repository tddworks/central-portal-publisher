// Imports will be available after plugin is applied

plugins {
    kotlin("jvm") version "2.2.0" apply false
    `maven-publish`
    signing
    id("com.tddworks.central-publisher")
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
    // Note: maven-publish and signing plugins are now applied per-module as needed
    // Central Publisher plugin will auto-configure them when present
    
    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(21)
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