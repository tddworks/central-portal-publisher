plugins {
    kotlin("multiplatform") version "2.2.0"
    `maven-publish`
    signing
    id("com.tddworks.central-publisher")
}

group = "com.tddworks.example"

version = "1.0.0"

repositories { mavenCentral() }

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
            dependencies { implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0") }
        }

        val commonTest by getting { dependencies { implementation(kotlin("test")) } }

        val jvmMain by getting {
            dependencies { implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0") }
        }

        val jvmTest by getting { dependencies { implementation(kotlin("test-junit5")) } }
    }
}

tasks.withType<Test> { useJUnitPlatform() }
