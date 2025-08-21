plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    api(project(":core")) // API module exposes core functionality
    implementation(kotlin("stdlib"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("${rootProject.name}-api")
                description.set("API interfaces for the multi-module example")
            }
        }
    }
}