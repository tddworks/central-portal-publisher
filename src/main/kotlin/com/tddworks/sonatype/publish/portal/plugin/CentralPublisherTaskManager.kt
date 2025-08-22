package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
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
        
        createPublishToCentralTask(config)
        createValidatePublishingTask(config)
        createBundleArtifactsTask(config)
        createSetupPublishingTask()
    }
    
    private fun createPublishToCentralTask(config: CentralPublisherConfig) {
        project.tasks.register(TASK_PUBLISH_TO_CENTRAL) {
            group = PLUGIN_GROUP
            description = "Publishes all artifacts to Maven Central"
            
            // Make sure bundle is created first
            dependsOn(TASK_BUNDLE_ARTIFACTS)
            
            doLast {
                // Task execution logic will be implemented later
                project.logger.quiet("🚀 Publishing to Maven Central...")
            }
        }
    }
    
    private fun createValidatePublishingTask(config: CentralPublisherConfig) {
        project.tasks.register(TASK_VALIDATE_PUBLISHING) {
            group = PLUGIN_GROUP
            description = "Validates publishing configuration without publishing"
            
            doLast {
                // Task execution logic will be implemented later
                project.logger.quiet("✅ Validating publishing configuration...")
            }
        }
    }
    
    private fun createBundleArtifactsTask(config: CentralPublisherConfig) {
        project.tasks.register(TASK_BUNDLE_ARTIFACTS) {
            group = PLUGIN_GROUP
            description = "Creates deployment bundle for Maven Central"
            
            doLast {
                // Task execution logic will be implemented later
                project.logger.quiet("📦 Creating deployment bundle...")
            }
        }
    }
    
    private fun createSetupPublishingTask() {
        project.tasks.register(TASK_SETUP_PUBLISHING) {
            group = PLUGIN_GROUP
            description = "Interactive setup wizard for Maven Central publishing"
            
            doLast {
                // Task execution logic will be implemented later
                project.logger.quiet("🧙 Starting setup wizard...")
            }
        }
    }
}