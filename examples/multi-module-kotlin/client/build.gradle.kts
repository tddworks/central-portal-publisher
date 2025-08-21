plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    api(project(":api")) // Client module provides a high-level interface to the API
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("${rootProject.name}-client")
                description.set("Client library for the multi-module example")
            }
        }
    }
}