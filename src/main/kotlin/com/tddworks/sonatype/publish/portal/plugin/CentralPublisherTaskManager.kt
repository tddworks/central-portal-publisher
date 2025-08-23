package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.tasks.BundleArtifactsTaskExecutor
import com.tddworks.sonatype.publish.portal.plugin.tasks.PublishToCentralTaskExecutor
import com.tddworks.sonatype.publish.portal.plugin.tasks.ValidatePublishingTaskExecutor
import org.gradle.api.Project

/**
 * Central Publisher Task Manager - Simple and Clear
 *
 * User Mental Model: "Create the publish commands I can run"
 */
class CentralPublisherTaskManager(private val project: Project) {

    companion object {
        const val PLUGIN_GROUP = "Central Publishing"

        // Task names - simple and memorable
        const val TASK_PUBLISH_TO_CENTRAL = "publishToCentral"
        const val TASK_BUNDLE_ARTIFACTS = "bundleArtifacts"
        const val TASK_VALIDATE_PUBLISHING = "validatePublishing"
        const val TASK_SETUP_PUBLISHING = "setupPublishing"
    }

    /** Creates all publishing tasks. Simple and direct. */
    fun createTasks(config: CentralPublisherConfig) {
        if (project.tasks.findByName(TASK_PUBLISH_TO_CENTRAL) != null) {
            return // Tasks already created
        }

        setupLocalRepository()
        createPublishToCentralTask(config)
        createBundleArtifactsTask(config)
        createValidatePublishingTask(config)
        createSetupTask()
    }

    /** Creates only the setup task (for projects without configuration). */
    fun createSetupTask() {
        if (project.tasks.findByName(TASK_SETUP_PUBLISHING) == null) {
            createSetupPublishingTask()
        }
    }

    private fun createPublishToCentralTask(config: CentralPublisherConfig) {
        project.tasks.register(TASK_PUBLISH_TO_CENTRAL) {
            group = PLUGIN_GROUP
            description = "🚀 Publish your artifacts to Maven Central (creates bundle and uploads)"
            dependsOn(TASK_BUNDLE_ARTIFACTS)

            doLast {
                val executor = PublishToCentralTaskExecutor(project, config)
                executor.execute()
            }
        }
    }

    private fun createBundleArtifactsTask(config: CentralPublisherConfig) {
        project.tasks.register(TASK_BUNDLE_ARTIFACTS) {
            group = PLUGIN_GROUP
            description = "📦 Prepare your artifacts for publishing (signs, validates, bundles)"

            // Depend on publishing all artifacts to LocalRepo
            setupBundleTaskDependencies()

            doLast {
                val executor = BundleArtifactsTaskExecutor(project, config)
                executor.execute()
            }
        }
    }

    private fun createValidatePublishingTask(config: CentralPublisherConfig) {
        project.tasks.register(TASK_VALIDATE_PUBLISHING) {
            group = PLUGIN_GROUP
            description = "✅ Check if your project is ready to publish (no upload, safe to run)"

            doLast {
                val executor = ValidatePublishingTaskExecutor(project, config)
                executor.execute()
            }
        }
    }

    private fun createSetupPublishingTask() {
        project.tasks.register(TASK_SETUP_PUBLISHING) {
            group = PLUGIN_GROUP
            description = "🧙 Set up your project for Maven Central publishing (interactive guide)"

            doLast {
                try {
                    project.logger.quiet("🧙 Starting setup wizard...")

                    val wizard =
                        com.tddworks.sonatype.publish.portal.plugin.wizard.RefactoredSetupWizard(
                            project
                        )
                    val result = wizard.runComplete()

                    if (result.isComplete) {
                        project.logger.quiet("✅ Setup wizard completed successfully!")
                        project.logger.quiet("📝 Generated files:")
                        result.filesGenerated.forEach { file -> project.logger.quiet("   - $file") }
                        project.logger.quiet("💡 Next steps:")
                        project.logger.quiet("   1. Review the generated configuration")
                        project.logger.quiet(
                            "   2. Run './gradlew validatePublishing' to check your setup"
                        )
                        project.logger.quiet(
                            "   3. Run './gradlew publishToCentral' when ready to publish"
                        )
                    } else {
                        project.logger.warn("⚠️ Setup was not completed successfully")
                    }
                } catch (e: Exception) {
                    project.logger.error("❌ Setup wizard failed: ${e.message}")
                    throw e
                }
            }
        }
    }

    private fun setupLocalRepository() {
        // Configure root project repository
        if (project.plugins.hasPlugin("maven-publish")) {
            project.extensions
                .getByType(org.gradle.api.publish.PublishingExtension::class.java)
                .apply {
                    repositories {
                        maven {
                            name = "LocalRepo"
                            url = project.uri("build/maven-repo")
                        }
                    }
                }
        }
    }

    /** Bundle task depends on all artifacts being published to LocalRepo first. */
    private fun org.gradle.api.Task.setupBundleTaskDependencies() {
        // Root project: depend on its publish task if it has publications
        if (project.plugins.hasPlugin("maven-publish")) {
            dependsOn("publishAllPublicationsToLocalRepoRepository")
            project.logger.quiet("📦 Bundle will wait for root project publishing")
        }

        // All subprojects: depend on their publish tasks
        project.subprojects.forEach { subproject ->
            if (subproject.plugins.hasPlugin("maven-publish")) {
                dependsOn("${subproject.path}:publishAllPublicationsToLocalRepoRepository")
                project.logger.quiet("📦 Bundle will wait for ${subproject.name} publishing")
            }
        }

        // Ensure signing tasks run first (Maven Central requires signatures)
        setupSigningDependencies()
    }

    /** Make sure signing happens before publishing. */
    private fun org.gradle.api.Task.setupSigningDependencies() {
        // Root project signing
        if (project.plugins.hasPlugin("signing")) {
            val rootSigningTasks =
                project.tasks.matching { task ->
                    task.name.startsWith("sign") && task.name.endsWith("Publication")
                }
            if (rootSigningTasks.isNotEmpty()) {
                dependsOn(rootSigningTasks)
                project.logger.quiet("🔐 Bundle will wait for root project signing")
            }
        }

        // Subproject signing
        project.subprojects.forEach { subproject ->
            if (subproject.plugins.hasPlugin("signing")) {
                val signingTasks =
                    subproject.tasks.matching { task ->
                        task.name.startsWith("sign") && task.name.endsWith("Publication")
                    }
                if (signingTasks.isNotEmpty()) {
                    dependsOn(signingTasks)
                    project.logger.quiet("🔐 Bundle will wait for ${subproject.name} signing")
                }
            }
        }
    }
}
