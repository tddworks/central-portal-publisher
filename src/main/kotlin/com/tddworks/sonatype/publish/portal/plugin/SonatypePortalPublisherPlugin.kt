package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.api.AuthenticationBuilder
import com.tddworks.sonatype.publish.portal.api.DeploymentBundleManager
import org.gradle.api.Plugin
import org.gradle.api.Project
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
    override fun apply(project: Project): Unit = with(project) {

        val extension = extensions.create<SonatypePortalPublisherExtension>(EXTENSION_NAME)

        // Set default values for the extension
        extension.autoPublish.convention(false)
        extension.authenticationProp.convention(AuthenticationBuilder().build())
        extension.modulesProp.convention(emptyList())

        // Configure the extension after the project has been evaluated
        afterEvaluate {
            configurePublisher()
        }
    }

    private fun Project.configurePublisher() {
        val extension = extensions.getByType<SonatypePortalPublisherExtension>()

        // Early-out with a warning if user hasn't added required config yet, to ensure project still syncs
        val authentication = extension.authenticationProp.get()

        val autoPublish = extension.autoPublish.get()

        if (extension.autoPublish.get() && (authentication.password.isNullOrBlank() || authentication.username.isNullOrBlank())) {
            logger.info("Sonatype Portal Publisher plugin applied to project: $path and autoPublish is enabled, but no authentication found. Skipping publishing.")
            return
        }

        val modules = extension.modulesProp.get()
        if (modules.isEmpty()) {
            logger.info("Sonatype Portal Publisher plugin applied to project: ${path}. modules configuration is empty. Will only publish the root module.")
        }

        logger.info(
            """
            Sonatype Portal Publisher plugin applied to project: ${path}
            Extension name: ${extension::class.simpleName}
            module: ${extension.modulesProp.get()}
            autoPublish: ${extension.autoPublish.get()}
            authentication: ${extension.authenticationProp.get()}
        """.trimIndent()
        )

        // Create a task to publish all publications to Sonatype Portal
        val publishAllPublicationsToCentralPortal = project.tasks.register("publishAllPublicationsToCentralPortal")

        // Create a task to publish to Sonatype Portal

        if (modules.isEmpty()) {
            project.allprojects.forEach { pj ->
                pj.pluginManager.withPlugin("maven-publish") {
                    DeploymentBundleManager().publishProjectPublications(
                        project,
                        authentication,
                        autoPublish,
                        publishAllPublicationsToCentralPortal,
                        project.path,
                        pj.publishingExtension,
                    )
                }
            }
        } else {
            project.allprojects.forEach { pj ->
                pj.pluginManager.withPlugin("maven-publish") {
                    DeploymentBundleManager().publishProjectPublications(
                        pj,
                        authentication,
                        autoPublish,
                        publishAllPublicationsToCentralPortal,
                        pj.path,
                        pj.publishingExtension,
                    )
                }
            }
        }
    }
}
