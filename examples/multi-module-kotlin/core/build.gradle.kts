plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation(kotlin("stdlib"))
}

// Core module is the foundation - no dependencies on other modules
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            // Module-specific POM customization
            pom {
                name.set("${rootProject.name}-core")
                description.set("Core functionality for the multi-module example")
            }
        }
    }
}