package com.tddworks.sonatype.publish.portal.api

import com.tddworks.sonatype.publish.portal.plugin.SonatypePortalPublisherPlugin.Companion.PUBLISH_AGGREGATION_PUBLICATIONS_TO_SONATYPE_PORTAL_REPOSITORY
import com.tddworks.sonatype.publish.portal.plugin.SonatypePortalPublisherPlugin.Companion.ZIP_ALL_PUBLICATIONS
import com.tddworks.sonatype.publish.portal.plugin.ZIP_CONFIGURATION_PRODUCER
import com.tddworks.sonatype.publish.portal.plugin.tasks.BundlePublishTaskProvider
import com.tddworks.sonatype.publish.portal.plugin.tasks.BundleZipTaskProvider
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.*

import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.provideDelegate

/**
 * 1. build all subprojects
 * 2. publish all publications -  project.rootProject.artifacts.add("zipline", zipTaskProvider)
 * 3. aggregate all publications from ziplineConfiguration artifacts
 *    project.tasks.register("zipAggregationPublication", Zip::class.java) {
 *                 from(ziplineConfiguration.artifacts.map {
 *                     println("ziplineConfiguration.artifacts: $it")
 *                     project.zipTree(it.file)
 *                 })
 *
 *                 println("Sonatype Portal Publisher plugin found project path: $path")
 *
 *                 destinationDirectory.set(project.layout.buildDirectory.dir("sonatype/zip"))
 *                 archiveFileName.set("publicationAggregated.zip")
 *             }
 */
class DeploymentBundleManager {

    fun publishProjectPublications(
        project: Project,
        authentication: Authentication?,
        autoPublish: Boolean?,
        projectPath: String,
        publishing: PublishingExtension,
    ) {

        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            val javadocJar by project.tasks.registering(Jar::class) {
                archiveClassifier.set("javadoc")
                duplicatesStrategy = DuplicatesStrategy.WARN
                // contents are deliberately left empty
            }


            publishing.publications.register<MavenPublication>("maven") {
                from(project.components["java"])
                configurePom()
            }
//         // Add an executable artifact if exists
            publishing.publications.withType<MavenPublication>().configureEach {
                artifact(javadocJar)
                // val execJar = tasks.findByName("buildExecutable") as? ReallyExecJar
                // if (execJar != null) {
                //   artifact(execJar.execJarFile)
                // }
            }
        }

        // Configure each publication
        publishing.publications.configureEach {
            // publications are the different artifacts that can be published
            // e.g. maven, kotlinMultiplatform, etc.
            // https://kotlinlang.org/docs/multiplatform-publish-lib.html#host-requirements
            // This kotlinMultiplatform publication includes metadata artifacts and references the other publications as its variants.

            val sonatypeDestinationPath = project.layout.buildDirectory.dir("sonatype/${name}-bundle")

            println("Sonatype Portal Publisher plugin found project path: $projectPath")

            // Capitalize the publication name
            // e.g. maven -> Maven
            // e.g. kotlinMultiplatform -> KotlinMultiplatform
            val capitalized = name.capitalized()

            // Add the Sonatype repository to the publishing block
            // each publication has a task to publish it to a repository

            // This step will create publishMavenPublicationToMavenRepository task
            publishing.apply {
                repositories.apply {
                    // default task name will be - publishMavenPublicationToMavenRepository
                    // because the publication name could be different, e.g. maven, kotlinMultiplatform, etc.
                    // so we need rename the task to ${capitalized}
                    // here we use maven as the repository type and save it to the build folder
                    // save to example-multi-modules/module-b/build/sonatype/maven-bundle/
                    maven {
                        // avoid central.sonatype.com - pom not found issue
                        // Bundle has content that does NOT have a .pom file
//                        metadataSources {
//                            mavenPom()
//                            artifact()
//                            // Indicates that this repository may not contain metadata files,
//                            ignoreGradleMetadataRedirection()
//                        }
                        name = capitalized
                        url = project.uri(sonatypeDestinationPath)

                    }

                }
            }

            val publication = publishing.publications.findByName(name)

            if (publication == null) {
                val candidates = publishing.publications.map { it.name }
                error("Sonatype Portal Publisher plugin cannot find publication '$name'. Candidates are: '${candidates.joinToString()}'")
            }

            // reuse the task to publish the publication to the repository
            val publishToTask = project.tasks.named("publish${capitalized}PublicationToMavenRepository")

            // remove the destination path before publishing
            publishToTask.configure {
                doFirst {
                    sonatypeDestinationPath.get().asFile.apply {
                        deleteRecursively()
                        mkdirs()
                    }
                }
            }


            val zipTaskProvider = BundleZipTaskProvider.zipTaskProvider(
                project,
                name,
                publishToTask,
                sonatypeDestinationPath
            )

            // Add the zip task to the zipAllPublications task
            // zipAllPublications will execute all the zip tasks
            // e.g zipMavenPublication, zipKotlinMultiplatformPublication, etc.
            project.rootProject.tasks.named(ZIP_ALL_PUBLICATIONS).configure {
                dependsOn(zipTaskProvider)
            }

            val publishTaskProvider = BundlePublishTaskProvider.publishTaskProvider(
                project,
                name,
                zipTaskProvider,
                authentication,
                autoPublish
            )

            // Add the publishing task to the publishAggregationPublicationsToSonatypePortalRepository task
            project.rootProject.tasks.named(PUBLISH_AGGREGATION_PUBLICATIONS_TO_SONATYPE_PORTAL_REPOSITORY).configure {
                group = "publishing"
                dependsOn(publishTaskProvider)
            }


            project.artifacts.add(ZIP_CONFIGURATION_PRODUCER, zipTaskProvider)
        }
    }
}

fun MavenPublication.configurePom() {
    val githubRepo = "github.com/tddworks/openai-kotlin"
    pom {
        name = "test"
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