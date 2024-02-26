package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.plugin.tasks.SonatypePublishPublicationToMavenRepositoryTaskFactory
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.extra
import java.io.File

interface ZipPublicationTaskFactory {
    fun createZipTask(
        project: Project,
        publicationName: String,
        dependsOnTask: TaskProvider<Task>,
    ): TaskProvider<Zip>

    fun createZipAggregationPublicationsTask(
        project: Project,
    ): TaskProvider<Zip>
}


class SonatypeZipPublicationTaskFactory : ZipPublicationTaskFactory {

    override fun createZipTask(
        project: Project,
        publicationName: String,
        dependsOnTask: TaskProvider<Task>,
    ): TaskProvider<Zip> {

        // get the sonatype build repository directory for task e.g publishMavenPublicationToMavenRepository
        val sonatypeBuildRepositoryDirectory =
            dependsOnTask.get().extra.get(SonatypePublishPublicationToMavenRepositoryTaskFactory.SONATYPE_BUILD_REPOSITORY_DIRECTORY) as File

        println("creating zip task for project ${project.name} with publicationName: $publicationName")

        val zipTaskProvider = project.tasks.register(taskName(publicationName.capitalized()), Zip::class.java) {
            dependsOn(dependsOnTask)
            from(sonatypeBuildRepositoryDirectory)
            eachFile {
                // central.sonatype.com - Bundle has content that does NOT have a .pom file
                // Exclude maven-metadata files
                if (name.startsWith("maven-metadata")) {
                    exclude()
                }
            }
            destinationDirectory.set(project.layoutBuildDirectory.dir(SONATYPE_ZIP_DIR))
            archiveFileName.set("$publicationName${BUNDLE_ZIP_SUFFIX}")
        }

        // Add the zip task to the ZIP_CONFIGURATION_PRODUCER configuration
        // which can be consumed by root projects
        project.artifacts.add(ZIP_CONFIGURATION_PRODUCER, zipTaskProvider)

        return zipTaskProvider
    }

    override fun createZipAggregationPublicationsTask(project: Project): TaskProvider<Zip> {
        return project.tasks.register(SonatypePortalPublisherPlugin.ZIP_AGGREGATION_PUBLICATIONS, Zip::class.java) {
            from(project.configurations.getByName(ZIP_CONFIGURATION_CONSUMER).elements?.map { bundle ->
                logger.quiet("Sonatype Portal Publisher plugin found publish bundle: $bundle")
                check(bundle.isNotEmpty()) {
                    "No bundle found for project: $path"
                }
                bundle.map {
                    project.zipTree(it)
                }
            })

            destinationDirectory.set(project.layout.buildDirectory.dir(SONATYPE_ZIP_DIR))
            archiveFileName.set("aggregated${BUNDLE_ZIP_SUFFIX}")
        }
    }

    private fun taskName(capitalized: String) = "zip${capitalized}Publication"

    companion object {
        private const val SONATYPE_ZIP_DIR = "sonatype/zip"
        private const val BUNDLE_ZIP_SUFFIX = "-deployment-bundle.zip"
    }
}