plugins {
    alias(libs.plugins.kotlin)
    id("com.tddworks.sonatype-portal-publisher") version "0.0.1"
    `maven-publish`
    signing
}

sonatypePortalPublisher {
    authentication {
        username = "some-username"
        password = "some-password"
    }
    autoPublish = true
}

kotlin {

}

version = "0.0.1"

group = "com.tddworks"

signing {
    // check gradle.properties for the key
    sign(publishing.publications)
}

publishing {

    publications {

        // Kotlin Multiplatform
        plugins.withId(libs.plugins.kotlinMultiplatform.get().pluginId) {
            val javadocJar by
            tasks.registering(Jar::class) {
                archiveClassifier = "javadoc"
                duplicatesStrategy = DuplicatesStrategy.WARN
                // contents are deliberately left empty
            }

            withType<MavenPublication>().configureEach {
                artifact(javadocJar)
                configurePom()
            }
        }

        // Kotlin JVM
        plugins.withId("org.jetbrains.kotlin.jvm") {

            val javadocJar by
            tasks.registering(Jar::class) {
                archiveClassifier = "javadoc"
                duplicatesStrategy = DuplicatesStrategy.WARN
                // contents are deliberately left empty
            }


            register<MavenPublication>("maven") {
                from(components["java"])
                configurePom()
            }

            // Add an executable artifact if exists
            withType<MavenPublication>().configureEach {
                artifact(javadocJar)
                // val execJar = tasks.findByName("buildExecutable") as? ReallyExecJar
                // if (execJar != null) {
                //   artifact(execJar.execJarFile)
                // }
            }
        }

        // Maven Bom
        plugins.withId("java-platform") {
            register<MavenPublication>("maven") {
                from(components["javaPlatform"])
                configurePom()
            }
        }

        // Gradle version catalog
        plugins.withId("version-catalog") {
            register<MavenPublication>("maven") {
                from(components["versionCatalog"])
                configurePom()
            }
        }

        // Add Dokka html doc to all publications
        plugins.withId("org.jetbrains.dokka") {
            val dokkaHtmlJar by
            tasks.registering(Jar::class) {
                from(tasks.named("dokkaHtml"))
                archiveClassifier = "html-docs"
            }

            withType<MavenPublication>().configureEach { artifact(dokkaHtmlJar) }
        }
    }
}

fun MavenPublication.configurePom() {
    val githubRepo = "github.com/tddworks/openai-kotlin"
    pom {
        name = provider { "${project.group}:${project.name}" }
//        description = provider { project.description }
        description = "OpenAI API KMP Client"
        inceptionYear = "2023"
        url = "https://github.com/tddworks/openai-kotlin"

        developers {
            developer {
                name = "tddworks"
                email = "itshan@tddworks.com"
                organization = "tddworks team"
                organizationUrl = "www.tddworks.com"
            }
        }

        licenses {
            license {
                name = "The Apache Software License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }

        scm {
            url = githubRepo
            connection = "scm:git:$githubRepo.git"
            developerConnection = "scm:git:$githubRepo.git"
        }
    }
}

tasks.withType<GenerateModuleMetadata> {
    enabled = false
}