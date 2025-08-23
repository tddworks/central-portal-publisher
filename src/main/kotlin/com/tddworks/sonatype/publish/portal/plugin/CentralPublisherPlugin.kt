package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.plugin.dsl.CentralPublisherExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Central Publisher Plugin - Simple and Clear
 *
 * User Mental Models:
 * 1. "I apply the plugin" → Plugin registers extension
 * 2. "I configure what I need" → Extension collects configuration
 * 3. "I run publish" → Plugin creates tasks and publishes
 * 4. "If something goes wrong, I get clear feedback" → Actionable error messages
 */
class CentralPublisherPlugin : Plugin<Project> {

    companion object {
        const val EXTENSION_NAME = "centralPublisher"
        private val CENTRAL_PUBLISHER_TASKS =
            setOf("publishToCentral", "bundleArtifacts", "validatePublishing", "setupPublishing")
    }

    override fun apply(project: Project) {
        // Always register the type-safe DSL extension
        val extension =
            project.extensions.create(
                EXTENSION_NAME,
                CentralPublisherExtension::class.java,
                project,
            )

        // Configure after project evaluation
        project.afterEvaluate {
            if (extension.hasExplicitConfiguration()) {
                configurePublishing(project, extension)
            } else {
                // Just create setup task for projects without configuration
                val taskManager = CentralPublisherTaskManager(project)
                taskManager.createSetupTask()
            }
        }
    }

    /** Single unified configuration path. Simple and predictable. */
    private fun configurePublishing(project: Project, extension: CentralPublisherExtension) {
        val config = extension.build()

        // Configure publications silently (so tasks are created)
        val publicationManager = CentralPublisherPublicationManager(project)
        publicationManager.configurePublications(config, showMessages = false)

        // Configure subprojects silently
        configureSubprojects(project, config, showMessages = false)

        // Create tasks
        val taskManager = CentralPublisherTaskManager(project)
        taskManager.createTasks(config)

        // Show messages when publishing tasks run
        project.gradle.taskGraph.whenReady {
            val willPublish =
                allTasks.any { task ->
                    CENTRAL_PUBLISHER_TASKS.contains(task.name) ||
                        (task.name.startsWith("publish") &&
                            (task.name.contains("LocalRepo") ||
                                task.name.contains("MavenLocal") ||
                                task.name == "publish"))
                }

            if (willPublish) {
                project.logger.quiet("🔧 Central Publisher ready for publishing")
            }
        }
    }

    /** Configure subprojects using the same pattern. */
    private fun configureSubprojects(
        project: Project,
        config: com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig,
        showMessages: Boolean = false,
    ) {
        project.subprojects.forEach { subproject ->
            subproject.afterEvaluate {
                if (subproject.plugins.hasPlugin("maven-publish")) {
                    val publicationManager = CentralPublisherPublicationManager(subproject)
                    publicationManager.configurePublications(config, showMessages)

                    // Configure subproject to publish to root project's repository
                    subproject.extensions
                        .getByType(org.gradle.api.publish.PublishingExtension::class.java)
                        .apply {
                            repositories {
                                maven {
                                    name = "LocalRepo"
                                    url =
                                        project.uri("build/maven-repo") // Root project's repository
                                }
                            }
                        }
                }
            }
        }
    }
}
