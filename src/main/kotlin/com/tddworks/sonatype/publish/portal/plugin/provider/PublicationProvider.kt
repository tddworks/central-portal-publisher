package com.tddworks.sonatype.publish.portal.plugin.provider

import com.tddworks.sonatype.publish.portal.plugin.configureIfExists
import com.tddworks.sonatype.publish.portal.plugin.configureMavenPublication
import com.tddworks.sonatype.publish.portal.plugin.publishingExtension
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*

interface PublicationProvider {
    fun preparePublication(project: Project)
}

class KotlinMultiplatformPublicationProvider : PublicationProvider {
    override fun preparePublication(project: Project) {
        // Kotlin Multiplatform
        project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            project.configureMavenPublication()
        }
    }
}

class JvmPublicationProvider : PublicationProvider {
    override fun preparePublication(project: Project) {

        // central.sonatype.com - Sources must be provided but not found in entries
        project.plugins.withId("java") {
            project.configureIfExists<JavaPluginExtension> {
                withSourcesJar()
            }
        }


        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            val javadocJar by project.tasks.registering(Jar::class) {
                archiveClassifier.set("javadoc")
                duplicatesStrategy = DuplicatesStrategy.WARN
                // contents are deliberately left empty
            }

            val publishing = project.publishingExtension

            publishing.publications.register<MavenPublication>("maven") {
                from(project.components["java"])
                project.configureMavenPublication()
            }

            // add javadocJar to the maven publication
            publishing.publications.withType<MavenPublication>().configureEach {
                artifact(javadocJar)
            }
        }
    }
}