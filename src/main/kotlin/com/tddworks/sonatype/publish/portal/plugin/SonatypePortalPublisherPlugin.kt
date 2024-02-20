package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.api.Authentication
import com.tddworks.sonatype.publish.portal.api.DeploymentBundleManager
import com.tddworks.sonatype.publish.portal.api.Settings
import com.tddworks.sonatype.publish.portal.plugin.tasks.BundleZipTaskProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType

/**
 * Sonatype Portal Publisher plugin.
 * This plugin is used to publish artifacts to Sonatype Portal.
 * It is a wrapper around the Sonatype Portal Publisher API.
 * It is used to publish artifacts to Sonatype Portal.
 * 1. get configuration from the extension
 * 2. create a task to publish all publications to Sonatype Portal
 */
class SonatypePortalPublisherPlugin : Plugin<Project> {
    private var zipConfiguration: Configuration? = null

    override fun apply(project: Project): Unit = with(project) {
        logger.quiet("Applying Sonatype Portal Publisher plugin to project: $path")
        extensions.create<SonatypePortalPublisherExtension>(EXTENSION_NAME)

        zipConfiguration = project.createZipConfigurationConsumer

        // Configure the extension after the project has been evaluated
        afterEvaluate {
            configurePublisher()
        }


//        if (zipConfiguration != null) {
//            configureAggregation(project)
//        }
    }

    private fun Project.configurePublisher() {
        logger.quiet("Configuring Sonatype Portal Publisher plugin for project: $path")
        val extension = extensions.getByType<SonatypePortalPublisherExtension>()

        // Early-out with a warning if user hasn't added required config yet, to ensure project still syncs
        val authentication = extension.getAuthentication()

        val settings = extension.getSettings()

        if (settings?.autoPublish == true && (authentication?.password.isNullOrBlank() || authentication?.username.isNullOrBlank())) {
            logger.info("Sonatype Portal Publisher plugin applied to project: $path and autoPublish is enabled, but no authentication found. Skipping publishing.")
            return
        }

        logger.quiet(
            """
            Sonatype Portal Publisher plugin applied to project: $path
            Extension name: ${extension::class.simpleName}
            autoPublish: ${settings?.autoPublish}
            aggregation: ${settings?.aggregation}
            authentication: ${extension.getAuthentication()}
        """.trimIndent()
        )

        enableZipAggregationPublicationsTaskIfNecessary(extension.getSettings()?.aggregation)

        // Create a task to publish all publications to Sonatype Portal
        val publishAllPublicationsToSonatypePortalRepository =
            enablePublishAggregationPublicationsTaskIfNecessary(extension.getSettings()?.aggregation)

        // Create a task to publish to Sonatype Portal
        project.allprojects.forEach { pj ->
            addProjectAsRootProjectDependencyIfNecessary(extension.getSettings()?.aggregation, pj)
            registerProjectPublications(
                pj,
                authentication,
                settings,
                publishAllPublicationsToSonatypePortalRepository,
            )
        }

    }

    private fun Project.enablePublishAggregationPublicationsTaskIfNecessary(isAggregation: Boolean?): TaskProvider<Task>? {
        return if (isAggregation == true) {
            logger.quiet("Enabling publishAggregationPublicationsToSonatypePortalRepository task for project: $path")
            project.tasks.register("publishAggregationPublicationsToSonatypePortalRepository")
        } else {
            null
        }
    }


    private fun Project.addProjectAsRootProjectDependencyIfNecessary(isAggregation: Boolean?, pj: Project) {
        if (isAggregation == true) {
            logger.quiet("Adding project: ${pj.path} as a dependency to the root project: $path")
            // add the root project as a dependency project to the ZIP_CONFIGURATION_CONSUMER configuration
            project.dependencies.add(ZIP_CONFIGURATION_CONSUMER, project.dependencies.project(mapOf("path" to pj.path)))
        }
    }

    private fun Project.enableZipAggregationPublicationsTaskIfNecessary(aggregation: Boolean?) {
        if (aggregation == true) {
            BundleZipTaskProvider.zipAggregationPublicationsProvider(this)
        }
    }

    private fun registerProjectPublications(
        pj: Project,
        authentication: Authentication?,
        settings: Settings?,
        publishAllPublicationsToSonatypePortalRepository: TaskProvider<Task>?,
    ) {
        pj.pluginManager.withPlugin("maven-publish") {

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
                publishAllPublicationsToSonatypePortalRepository,
                pj.path,
                pj.publishingExtension,
            )
        }
    }
}