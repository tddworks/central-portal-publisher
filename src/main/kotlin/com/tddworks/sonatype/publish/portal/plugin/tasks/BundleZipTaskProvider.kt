package com.tddworks.sonatype.publish.portal.plugin.tasks

import com.tddworks.sonatype.publish.portal.plugin.ZIP_CONFIGURATION_CONSUMER
import com.tddworks.sonatype.publish.portal.plugin.layoutBuildDirectory
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip


object BundleZipTaskProvider {
    private const val SONATYPE_ZIP_DIR = "sonatype/zip"
    private const val BUNDLE_ZIP_SUFFIX = "-bundle.zip"
    fun zipTaskProvider(
        project: Project,
        capitalized: String,
        dependsOnTask: TaskProvider<Task>,
        sonatypeDestinationPath: Provider<Directory>,
    ): TaskProvider<Zip> =
        project.tasks.register(taskName(capitalized), Zip::class.java) {
            dependsOn(dependsOnTask)
            from(sonatypeDestinationPath)
            destinationDirectory.set(project.layoutBuildDirectory.dir(SONATYPE_ZIP_DIR))
            archiveFileName.set("$capitalized$BUNDLE_ZIP_SUFFIX")
        }

    private fun taskName(capitalized: String) = "zip${capitalized}Publication"


    fun zipAllPublicationsProvider(
        project: Project,
    ): TaskProvider<Zip> =
        project.tasks.register("zipAllPublications", Zip::class.java) {
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
            archiveFileName.set("publicationsAggregated$BUNDLE_ZIP_SUFFIX")
        }
}