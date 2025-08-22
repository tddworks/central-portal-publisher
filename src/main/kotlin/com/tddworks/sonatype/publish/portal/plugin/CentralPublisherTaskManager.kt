package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.tasks.ValidatePublishingTaskExecutor
import com.tddworks.sonatype.publish.portal.plugin.tasks.BundleArtifactsTaskExecutor
import com.tddworks.sonatype.publish.portal.plugin.tasks.PublishToCentralTaskExecutor
import org.gradle.api.Project

/**
 * Manages task creation and lifecycle for Central Publisher plugin.
 * 
 * Responsibilities:
 * - Create all publishing-related tasks
 * - Set up task dependencies and lifecycle
 * - Configure task properties and behavior
 * 
 * This class encapsulates the task management logic that was previously in the main plugin,
 * following the developer mental model: "Create the publish commands"
 */
class CentralPublisherTaskManager(
    private val project: Project
) {
    
    companion object {
        const val PLUGIN_GROUP = "Central Publishing"
        
        // Task names - simple and memorable
        const val TASK_PUBLISH_TO_CENTRAL = "publishToCentral"
        const val TASK_BUNDLE_ARTIFACTS = "bundleArtifacts" 
        const val TASK_VALIDATE_PUBLISHING = "validatePublishing"
        const val TASK_SETUP_PUBLISHING = "setupPublishing"
    }
    
    /**
     * Creates all publishing tasks for the project.
     * 
     * Developer mental model: "Set up the commands I can run to publish"
     * 
     * @param config the resolved configuration to use for task setup
     */
    fun createPublishingTasks(config: CentralPublisherConfig) {
        // Avoid creating duplicate tasks
        if (project.tasks.findByName(TASK_PUBLISH_TO_CENTRAL) != null) {
            return
        }
        
        setupLocalRepository()
        createBundleArtifactsTask(config)
        createPublishToCentralTask(config)
        createValidatePublishingTask(config)
        createSetupPublishingTask()
    }
    
    private fun createPublishToCentralTask(config: CentralPublisherConfig) {
        project.tasks.register(TASK_PUBLISH_TO_CENTRAL) {
            group = PLUGIN_GROUP
            description = "🚀 Publish your artifacts to Maven Central (creates bundle and uploads)"
            
            // Make sure bundle is created first
            dependsOn(TASK_BUNDLE_ARTIFACTS)
            
            doLast {
                val executor = PublishToCentralTaskExecutor(project, config)
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
    
    private fun createBundleArtifactsTask(config: CentralPublisherConfig) {
        project.tasks.register(TASK_BUNDLE_ARTIFACTS) {
            group = PLUGIN_GROUP
            description = "📦 Prepare your artifacts for publishing (signs, validates, bundles)"
            
            // Simple, clear dependencies - bundle depends on publishing all artifacts to LocalRepo
            setupBundleTaskDependencies()
            
            doLast {
                val executor = BundleArtifactsTaskExecutor(project, config)
                executor.execute()
            }
        }
    }
    
    /**
     * Sets up clear, simple dependencies for the bundle task.
     * Developer mental model: "Bundle needs all artifacts published to LocalRepo first"
     */
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
        
        // Signing tasks will run automatically as part of publishing tasks above
        // No need to explicitly depend on signing tasks - publishing handles it
    }
    
    private fun createSetupPublishingTask() {
        project.tasks.register(TASK_SETUP_PUBLISHING) {
            group = PLUGIN_GROUP
            description = "🧙 Set up your project for Maven Central publishing (interactive guide)"
            
            doLast {
                try {
                    project.logger.quiet("🧙 Starting setup wizard...")
                    
                    val wizard = com.tddworks.sonatype.publish.portal.plugin.wizard.RefactoredSetupWizard(project)
                    val result = wizard.runComplete()
                    
                    if (result.isComplete) {
                        project.logger.quiet("✅ Setup wizard completed successfully!")
                        project.logger.quiet("📝 Generated files:")
                        result.filesGenerated.forEach { file ->
                            project.logger.quiet("   - $file")
                        }
                        project.logger.quiet("💡 Next steps:")
                        project.logger.quiet("   1. Review the generated configuration")
                        project.logger.quiet("   2. Run './gradlew validatePublishing' to check your setup")
                        project.logger.quiet("   3. Run './gradlew publishToCentral' when ready to publish")
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
        // Configure repositories for both signed and unsigned artifacts
        // Note: maven-publish plugin is already applied by publication strategies
        // Note: Subprojects are configured in CentralPublisherPlugin.configureSubprojects()
        
        // Configure root project repository
        if (project.plugins.hasPlugin("maven-publish")) {
            project.extensions.getByType(org.gradle.api.publish.PublishingExtension::class.java).apply {
                repositories {
                    // Repository for signed artifacts (preferred by Maven Central)
                    maven {
                        name = "LocalRepo"
                        url = project.uri("build/maven-repo")
                    }
                }
            }
        }
    }
    
    /**
     * Creates only the setup task (for when no configuration is provided).
     */
    fun createSetupTask() {
        if (project.tasks.findByName(TASK_SETUP_PUBLISHING) == null) {
            createSetupPublishingTask()
        }
    }
}