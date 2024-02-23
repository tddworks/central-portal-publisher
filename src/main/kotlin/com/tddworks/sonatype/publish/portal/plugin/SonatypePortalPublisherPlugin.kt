package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.api.Authentication
import com.tddworks.sonatype.publish.portal.api.DeploymentBundleManager
import com.tddworks.sonatype.publish.portal.api.SonatypePublisherSettings
import com.tddworks.sonatype.publish.portal.plugin.tasks.BundlePublishTaskProvider
import com.tddworks.sonatype.publish.portal.plugin.tasks.BundleZipTaskProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.*

/**
 * Sonatype Portal Publisher plugin.
 * This plugin is used to publish artifacts to Sonatype Portal.
 * It is a wrapper around the Sonatype Portal Publisher API.
 * It is used to publish artifacts to Sonatype Portal.
 * 1. get configuration from the extension
 * 2. create a task to publish all publications to Sonatype Portal
 */
class SonatypePortalPublisherPlugin : Plugin<Project> {
    companion object {
        const val PUBLISH_ALL_PUBLICATIONS_TO_SONATYPE_PORTAL_REPOSITORY =
            "publishAllPublicationsToSonatypePortalRepository"
        const val PUBLISH_AGGREGATION_PUBLICATIONS_TO_SONATYPE_PORTAL_REPOSITORY =
            "publishAggregationPublicationsToSonatypePortalRepository"
        const val ZIP_AGGREGATION_PUBLICATIONS = "zipAggregationPublications"
        const val ZIP_ALL_PUBLICATIONS = "zipAllPublications"
    }

    override fun apply(project: Project) {
        with(project) {
            logger.quiet("Applying Sonatype Portal Publisher plugin to project: $path")
            extensions.create<SonatypePortalPublisherExtension>(EXTENSION_NAME)

            afterEvaluate {
                configurePublisher()
            }
        }
    }

    private fun Project.configurePublisher() {
        // create a ZIP_CONFIGURATION_PRODUCER configuration for project
        createZipConfigurationConsumer

        logger.quiet("Configuring Sonatype Portal Publisher plugin for project: $path")
        val extension = extensions.getByType<SonatypePortalPublisherExtension>()
        val authentication = extension.getAuthentication()
        val settings = extension.getSettings()

        if (settings?.autoPublish == true && (authentication?.password.isNullOrBlank() || authentication?.username.isNullOrBlank())) {
            logger.info("Sonatype Portal Publisher plugin applied to project: $path and autoPublish is enabled, but no authentication found. Skipping publishing.")
            return
        }

        loggingExtensionInfo(extension, settings)


        // Create a task to zip all publications
        val zipAllPublications = project.tasks.register(ZIP_ALL_PUBLICATIONS)

        // Create a task to publish all publications to Sonatype Portal
        project.tasks.register(PUBLISH_ALL_PUBLICATIONS_TO_SONATYPE_PORTAL_REPOSITORY) {
            group = "publishing"
            // publish all publications depends on zipAllPublications
            dependsOn(zipAllPublications)
        }

        val zipProvider = enableZipAggregationPublicationsTaskIfNecessary(extension.getSettings()?.aggregation)


        enablePublishAggregationPublicationsTaskIfNecessary(
            extension.getSettings()?.aggregation,
            zipProvider,
            authentication,
            settings?.autoPublish
        )

//        val clearTempRepoDir by tasks.registering {
//            val dir = project.layout.buildDirectory.dir("sonatype/${name}-bundle").get().asFile
//            doFirst {
//                dir.deleteRecursively()
//            }
//        }

        // Create a task to publish to Sonatype Portal
        project.allprojects.forEach { pj ->
            addProjectAsRootProjectDependencyIfNecessary(extension.getSettings()?.aggregation, pj)



            pj.pluginManager.withPlugin("maven-publish") {

//                configure<PublishingExtension> {
//                    repositories {
//                        maven {
//                            name = ""
//                            url = uri("")
//                        }
//                    }
//                }
//
//                tasks.withType<PublishToMavenRepository>().configureEach {
//                    if (name.endsWith("To${tempRepoName.capitalized()}Repository")) {
//                        dependsOn(clearTempRepoDir)
//                    }
//                }

                // should move to the zip register task
                // create a ZIP_CONFIGURATION_PRODUCER configuration for each project
                pj.createZipConfigurationProducer
//
//            DefaultProjectPublicationsManager().addPublication(
//                pj,
//                publishAllPublicationsToSonatypePortalRepository,
//                zipAllPublications
//            )

                DeploymentBundleManager().publishProjectPublications(
                    pj,
                    authentication,
                    settings?.autoPublish,
                    pj.path,
                    pj.publishingExtension
                )
            }
        }
    }

    private fun Project.enablePublishAggregationPublicationsTaskIfNecessary(
        isAggregation: Boolean?,
        zipProvider: TaskProvider<Zip>?,
        authentication: Authentication? = null,
        autoPublish: Boolean? = null,
    ) {
        if (isAggregation == true) {
            logger.quiet("Enabling publishAggregationPublicationsToSonatypePortalRepository task for project: $path")

            BundlePublishTaskProvider.publishAggTaskProvider(
                project,
                zipProvider!!,
                authentication,
                autoPublish
            )
        }
    }

    private fun Project.addProjectAsRootProjectDependencyIfNecessary(isAggregation: Boolean?, pj: Project) {
        if (isAggregation == true) {
            logger.quiet("Adding project: ${pj.path} as a dependency to the root project: $path")
            // add the root project as a dependency project to the ZIP_CONFIGURATION_CONSUMER configuration
            project.dependencies.add(ZIP_CONFIGURATION_CONSUMER, project.dependencies.project(mapOf("path" to pj.path)))
        }
    }

    private fun Project.enableZipAggregationPublicationsTaskIfNecessary(aggregation: Boolean?): TaskProvider<Zip>? {
        if (aggregation == true) {
            createZipConfigurationProducer
            return BundleZipTaskProvider.zipAggregationPublicationsProvider(this)
        }
        return null
    }

    private fun Project.loggingExtensionInfo(
        extension: SonatypePortalPublisherExtension,
        settings: SonatypePublisherSettings?,
    ) {
        logger.quiet(
            """
            Sonatype Portal Publisher plugin applied to project: $path
            Extension name: ${extension::class.simpleName}
            autoPublish: ${settings?.autoPublish}
            aggregation: ${settings?.aggregation}
            authentication: ${extension.getAuthentication()}
        """.trimIndent()
        )
    }
}
