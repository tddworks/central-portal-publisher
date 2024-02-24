package com.tddworks.sonatype.publish.portal.plugin.tasks

import com.tddworks.sonatype.publish.portal.plugin.SonatypePortalPublisherPlugin.Companion.ZIP_AGGREGATION_PUBLICATIONS
import com.tddworks.sonatype.publish.portal.plugin.ZIP_CONFIGURATION_CONSUMER
import com.tddworks.sonatype.publish.portal.plugin.ZIP_CONFIGURATION_PRODUCER
import com.tddworks.sonatype.publish.portal.plugin.layoutBuildDirectory
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.configurationcache.extensions.capitalized
import java.io.File

object BundleZipTaskProvider {
    private const val SONATYPE_ZIP_DIR = "sonatype/zip"
    private const val BUNDLE_ZIP_SUFFIX = "-deployment-bundle.zip"
    fun zipTaskProvider(
        project: Project,
        publicationName: String,
        dependsOnTask: TaskProvider<Task>,
        sonatypeDestinationPath: File,
    ): TaskProvider<Zip> =
        project.tasks.register(taskName(publicationName.capitalized()), Zip::class.java) {
            dependsOn(dependsOnTask)
            from(sonatypeDestinationPath)
            eachFile {
                // central.sonatype.com - Bundle has content that does NOT have a .pom file
                // Exclude maven-metadata files
                if (name.startsWith("maven-metadata")) {
                    exclude()
                }
            }
            destinationDirectory.set(project.layoutBuildDirectory.dir(SONATYPE_ZIP_DIR))
            archiveFileName.set("$publicationName$BUNDLE_ZIP_SUFFIX")
        }

    private fun taskName(capitalized: String) = "zip${capitalized}Publication"


    fun zipAggregationPublicationsProvider(
        project: Project,
    ): TaskProvider<Zip> =
        project.tasks.register(ZIP_AGGREGATION_PUBLICATIONS, Zip::class.java) {
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
            archiveFileName.set("aggregated$BUNDLE_ZIP_SUFFIX")
        }
}