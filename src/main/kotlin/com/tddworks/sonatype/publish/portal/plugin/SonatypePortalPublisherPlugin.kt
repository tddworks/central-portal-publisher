package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.api.SonatypePublisherSettings
import com.tddworks.sonatype.publish.portal.plugin.provider.JvmPublicationProvider
import com.tddworks.sonatype.publish.portal.plugin.provider.SonatypePortalPublishingTaskManager
import com.tddworks.sonatype.publish.portal.plugin.tasks.SonatypeDevelopmentBundlePublishTaskFactory
import com.tddworks.sonatype.publish.portal.plugin.tasks.SonatypePublishPublicationToMavenRepositoryTaskFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
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

    lateinit var sonatypePortalPublishingTaskManager: SonatypePortalPublishingTaskManager

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

            sonatypePortalPublishingTaskManager = SonatypePortalPublishingTaskManager(
                publishPublicationToMavenRepositoryTaskFactory = SonatypePublishPublicationToMavenRepositoryTaskFactory(
                    publishingBuildRepositoryManager = SonatypePortalPublishingBuildRepositoryManager()
                ),
                zipPublicationTaskFactory = SonatypeZipPublicationTaskFactory(),
                developmentBundlePublishTaskFactory = SonatypeDevelopmentBundlePublishTaskFactory(),
                publicationProvider = JvmPublicationProvider()
            )

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
        val authentication = extension.getAuthentication(this)
        val settings = extension.getSettings()

        if (settings?.autoPublish == true && (authentication?.password.isNullOrBlank() || authentication?.username.isNullOrBlank())) {
            logger.info("Sonatype Portal Publisher plugin applied to project: $path and autoPublish is enabled, but no authentication found. Skipping publishing.")
            return
        }

        sonatypePortalPublishingTaskManager.apply {
            this.autoPublish = settings?.autoPublish
            this.authentication = authentication
            this.settings = settings
        }

        loggingExtensionInfo(extension, settings)


        sonatypePortalPublishingTaskManager.registerPublishingTasks(this)
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
            authentication: ${extension.getAuthentication(this)}
        """.trimIndent()
        )
    }
}
