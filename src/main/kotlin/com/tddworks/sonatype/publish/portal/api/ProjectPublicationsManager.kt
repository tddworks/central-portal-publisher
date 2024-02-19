package com.tddworks.sonatype.publish.portal.api

import com.tddworks.sonatype.publish.portal.plugin.ZIP_CONFIGURATION_PRODUCER
import com.tddworks.sonatype.publish.portal.plugin.publishingExtension
import com.tddworks.sonatype.publish.portal.plugin.tasks.BundleZipTaskProvider
import com.tddworks.sonatype.publish.portal.plugin.createZipConfigurationConsumer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.configurationcache.extensions.capitalized

interface ProjectPublicationsManager {
    fun addPublication(project: Project, zipAllPublications: TaskProvider<Task>)
}


class DefaultProjectPublicationsManager : ProjectPublicationsManager {
    override fun addPublication(
        project: Project,
        zipAllPublications: TaskProvider<Task>,
    ) {
        project.pluginManager.withPlugin("maven-publish") {
            // create a ZIP_CONFIGURATION_PRODUCER configuration for project
            project.createZipConfigurationConsumer

            val publishing = project.publishingExtension
            val projectPath = project.path

            publishing.publications.configureEach {
                // publications are the different artifacts that can be published
                // e.g. maven, kotlinMultiplatform, etc.
                // https://kotlinlang.org/docs/multiplatform-publish-lib.html#host-requirements
                // This kotlinMultiplatform publication includes metadata artifacts and references the other publications as its variants.

                val sonatypeDestinationPath = project.layout.buildDirectory.dir("sonatype/${name}-bundle")

                println("Sonatype Portal Publisher plugin found project path: $projectPath")

                val capitalized = name.capitalized()

                val publishingRepoName = "sonatype$capitalized"

                // Add the Sonatype repository to the publishing block
                // for issue -  Task with name 'publishMavenPublicationToSonatypeMavenRepository' not found in project ':example-single-module'.
                // each publication has a task to publish it to a repository
                createRepository(publishing, publishingRepoName, project, sonatypeDestinationPath)

                project.logger.quiet(
                    """
                            Sonatype Portal Publisher plugin found project path: $projectPath
                            Sonatype Portal Publisher plugin found publication name: $name
                            Sonatype Portal Publisher plugin found capitalized publication name: $capitalized
                            Sonatype Portal Publisher plugin found sonatypeDestinationPath: ${sonatypeDestinationPath.get().asFile.path}
                            Sonatype Portal Publisher plugin found repoName: $publishingRepoName
                            Sonatype Portal Publisher plugin found maven url: ${project.uri(sonatypeDestinationPath)}
                        """.trimIndent()
                )

                val publication = publishing.publications.findByName(name)

                if (publication == null) {
                    val candidates = publishing.publications.map { it.name }
                    error("Sonatype Portal Publisher plugin cannot find publication '$name'. Candidates are: '${candidates.joinToString()}'")
                }

                val publishToSonatypeTaskProvider =
                    project.tasks.named("publish${capitalized}PublicationTo${publishingRepoName.capitalized()}Repository")

                publishToSonatypeTaskProvider.configure {
                    doFirst {
                        sonatypeDestinationPath.get().asFile.apply {
                            deleteRecursively()
                            mkdirs()
                        }
                    }
                }

                val zipTaskProvider = BundleZipTaskProvider.zipTaskProvider(
                    project,
                    capitalized,
                    publishToSonatypeTaskProvider,
                    sonatypeDestinationPath
                )

//                val publishTaskProvider = BundlePublishTaskProvider.publishTaskProvider(
//                    project,
//                    capitalized,
//                    zipTaskProvider,
//                    authentication,
//                    autoPublish
//                )
//
//                // Add the publishing task to the publishAllPublicationsToCentralPortal task
//                publishAllPublicationsToSonatypePortal.configure {
//                    dependsOn((publishTaskProvider))
//                }

                project.artifacts.add(ZIP_CONFIGURATION_PRODUCER, zipTaskProvider)
            }
        }
    }

    private fun createRepository(
        publishing: PublishingExtension,
        publishingRepoName: String,
        project: Project,
        sonatypeDestinationPath: Provider<Directory>,
    ) {
        publishing.apply {
            repositories.apply {
                maven {
                    name = publishingRepoName
                    url = project.uri(sonatypeDestinationPath)
                }
            }
        }
    }
}