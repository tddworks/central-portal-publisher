package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.plugin.dsl.CentralPublisherExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Simplified Central Portal Publisher plugin following developer mental models.
 *
 * Developer Mental Model:
 * 1. "I apply the plugin" → Plugin registers extension and prepares for configuration
 * 2. "I configure what I need" → Extension collects configuration via type-safe DSL
 * 3. "I run publish" → Plugin uses managers to coordinate complex operations
 * 4. "If something goes wrong, I get clear feedback" → Actionable error messages
 *
 * Architecture:
 * - CentralPublisherConfigurationManager: "Figure out what to publish"
 * - CentralPublisherTaskManager: "Create the publish commands"
 * - CentralPublisherPublicationManager: "Configure the artifacts"
 *
 * This simplified plugin orchestrates these managers while keeping the apply() method under 20
 * lines to match developer expectations of simplicity.
 */
class CentralPublisherPlugin : Plugin<Project> {

    companion object {
        const val EXTENSION_NAME = "centralPublisher"

        // Central Publisher task names
        private val CENTRAL_PUBLISHER_TASKS =
            setOf("publishToCentral", "bundleArtifacts", "validatePublishing", "setupPublishing")
    }

    override fun apply(project: Project) {
        // Always register the type-safe DSL extension (developer mental model: "I can now
        // configure")
        val extension =
            project.extensions.create(
                EXTENSION_NAME,
                CentralPublisherExtension::class.java,
                project,
            )

        // Defer activation decision until after evaluation when we can check if configured
        project.afterEvaluate {
            val requestedTasks = project.gradle.startParameter.taskNames
            val isPublishingTaskRequested =
                requestedTasks.any { taskName ->
                    val cleanTaskName = taskName.substringAfterLast(":")
                    CENTRAL_PUBLISHER_TASKS.contains(cleanTaskName) ||
                        CENTRAL_PUBLISHER_TASKS.any { ourTask -> taskName.contains(ourTask) }
                }
            val isTasksCommand = requestedTasks.contains("tasks")
            val isTestMode =
                System.getProperty("central.publisher.test.mode") == "true" ||
                    project.extensions.extraProperties.has("testingPublishingTask")

            when {
                // Case 1: Test mode - always do full configuration with messages for testing
                isTestMode -> {
                    project.logger.quiet(
                        "Applying Central Publisher plugin to project: ${project.path}"
                    )
                    configureForPublishing(project, extension, showMessages = true)
                }

                // Case 2: User is actually running publishing tasks - do full configuration with
                // messages
                isPublishingTaskRequested -> {
                    project.logger.quiet(
                        "Applying Central Publisher plugin to project: ${project.path}"
                    )
                    configureForPublishing(project, extension, showMessages = true)
                }

                // Case 3: User wants to see available tasks, or has explicit configuration - create
                // tasks silently
                isTasksCommand || extension.hasExplicitConfiguration() -> {
                    configureForPublishing(project, extension, showMessages = false)
                }

                // Case 4: No configuration, just provide setup task for discovery
                else -> {
                    val taskManager = CentralPublisherTaskManager(project)
                    taskManager.createSetupTask()
                }
            }
        }
    }

    /**
     * Configure publishing using our focused managers. This method orchestrates the managers while
     * maintaining clear separation of concerns.
     */
    private fun configureForPublishing(
        project: Project,
        extension: CentralPublisherExtension,
        showMessages: Boolean = true,
    ) {
        val configurationManager = CentralPublisherConfigurationManager(project, extension)

        // Check if developer wants to publish (mental model: "Should I set up publishing?")
        if (!configurationManager.shouldSetupPublishing()) {
            if (showMessages) {
                project.logger.quiet(
                    "🔧 No explicit configuration detected - use './gradlew setupPublishing' to configure interactively"
                )
                project.logger.quiet(
                    "   Or add centralPublisher {} block to your build.gradle file"
                )
            }

            // Always create setup wizard task even without configuration
            val taskManager = CentralPublisherTaskManager(project)
            taskManager.createSetupTask()
            return
        }

        // Resolve and validate configuration (mental model: "What should I publish?")
        val config = configurationManager.resolveConfiguration()
        val validationResult = configurationManager.validateConfiguration()

        if (showMessages) {
            if (!validationResult.isValid) {
                project.logger.error("❌ Configuration validation failed:")
                project.logger.error(validationResult.formatReport())
                project.logger.warn(
                    "💡 Fix the errors above, then run './gradlew validatePublishing' to check your fixes"
                )
            } else if (validationResult.warningCount > 0) {
                project.logger.warn("⚠️ Configuration warnings:")
                project.logger.warn(validationResult.formatReport())
                project.logger.quiet(
                    "💡 Warnings won't prevent publishing, but consider addressing them"
                )
            } else {
                project.logger.quiet("✅ Central Publisher configuration validated successfully")
            }
        }

        // Configure publications (mental model: "Set up what gets published")
        val publicationManager = CentralPublisherPublicationManager(project)
        val publicationResult = publicationManager.configurePublications(config, showMessages)

        if (showMessages && !publicationResult.isConfigured) {
            project.logger.warn("⚠️ ${publicationResult.reason}")
            project.logger.quiet(
                "💡 Apply java-library or kotlin plugins to enable automatic publication setup"
            )
        }

        // Create tasks (mental model: "Give me commands to run")
        val taskManager = CentralPublisherTaskManager(project)
        taskManager.createPublishingTasks(config)

        // Configure subprojects using the same pattern
        configureSubprojects(project, config, showMessages)

        if (showMessages) {
            project.logger.quiet("🔧 Central Publisher configured successfully")
        }
    }

    /** Configure subprojects using the same manager pattern. */
    private fun configureSubprojects(
        project: Project,
        config: com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig,
        showMessages: Boolean = true,
    ) {
        project.subprojects.forEach { subproject ->
            subproject.afterEvaluate {
                if (subproject.plugins.hasPlugin("maven-publish")) {
                    val publicationManager = CentralPublisherPublicationManager(subproject)
                    val result = publicationManager.configurePublications(config, showMessages)

                    if (showMessages) {
                        if (result.isConfigured) {
                            subproject.logger.quiet(
                                "✅ Subproject ${subproject.path}: Auto-configured for ${result.detectedPluginType}"
                            )
                        } else {
                            subproject.logger.warn(
                                "⚠️ Subproject ${subproject.path}: ${result.reason}"
                            )
                        }
                    }

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

                    if (showMessages) {
                        subproject.logger.quiet(
                            "📦 Configured ${subproject.path} to publish to root project repository"
                        )
                    }
                }
            }
        }
    }
}
