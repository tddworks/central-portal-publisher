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
 * This simplified plugin orchestrates these managers while keeping the apply() method
 * under 20 lines to match developer expectations of simplicity.
 */
class CentralPublisherPlugin : Plugin<Project> {
    
    companion object {
        const val EXTENSION_NAME = "centralPublisher"
    }
    
    override fun apply(project: Project) {
        project.logger.quiet("Applying Central Publisher plugin to project: ${project.path}")
        
        // Register the type-safe DSL extension (developer mental model: "I can now configure")
        val extension = project.extensions.create(EXTENSION_NAME, CentralPublisherExtension::class.java, project)
        
        // Defer complex logic until developer has configured what they want
        project.afterEvaluate {
            configureForPublishing(project, extension)
        }
    }
    
    /**
     * Configure publishing using our focused managers.
     * This method orchestrates the managers while maintaining clear separation of concerns.
     */
    private fun configureForPublishing(project: Project, extension: CentralPublisherExtension) {
        val configurationManager = CentralPublisherConfigurationManager(project, extension)
        
        // Check if developer wants to publish (mental model: "Should I set up publishing?")
        if (!configurationManager.shouldSetupPublishing()) {
            project.logger.quiet("🔧 No explicit configuration detected - use './gradlew setupPublishing' to configure interactively")
            project.logger.quiet("   Or add centralPublisher {} block to your build.gradle file")
            
            // Always create setup wizard task even without configuration
            val taskManager = CentralPublisherTaskManager(project)
            taskManager.createSetupTask()
            return
        }
        
        // Resolve and validate configuration (mental model: "What should I publish?")
        val config = configurationManager.resolveConfiguration()
        val validationResult = configurationManager.validateConfiguration()
        
        if (!validationResult.isValid) {
            project.logger.error("❌ Configuration validation failed:")
            project.logger.error(validationResult.formatReport())
            project.logger.warn("💡 Fix the errors above, then run './gradlew validatePublishing' to check your fixes")
        } else if (validationResult.warningCount > 0) {
            project.logger.warn("⚠️ Configuration warnings:")
            project.logger.warn(validationResult.formatReport())
            project.logger.quiet("💡 Warnings won't prevent publishing, but consider addressing them")
        } else {
            project.logger.quiet("✅ Central Publisher configuration validated successfully")
        }
        
        // Configure publications (mental model: "Set up what gets published")
        val publicationManager = CentralPublisherPublicationManager(project)
        val publicationResult = publicationManager.configurePublications(config)
        
        if (!publicationResult.isConfigured) {
            project.logger.warn("⚠️ ${publicationResult.reason}")
            project.logger.quiet("💡 Apply java-library or kotlin plugins to enable automatic publication setup")
        }
        
        // Create tasks (mental model: "Give me commands to run")
        val taskManager = CentralPublisherTaskManager(project)
        taskManager.createPublishingTasks(config)
        
        // Configure subprojects using the same pattern
        configureSubprojects(project, config)
        
        project.logger.quiet("🔧 Central Publisher configured successfully")
    }
    
    /**
     * Configure subprojects using the same manager pattern.
     */
    private fun configureSubprojects(project: Project, config: com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig) {
        project.subprojects.forEach { subproject ->
            subproject.afterEvaluate {
                if (subproject.plugins.hasPlugin("maven-publish")) {
                    val publicationManager = CentralPublisherPublicationManager(subproject)
                    val result = publicationManager.configurePublications(config)
                    
                    if (result.isConfigured) {
                        subproject.logger.quiet("✅ Subproject ${subproject.path}: Auto-configured for ${result.detectedPluginType}")
                    } else {
                        subproject.logger.warn("⚠️ Subproject ${subproject.path}: ${result.reason}")
                    }
                    
                    // Configure subproject to publish to root project's repository
                    subproject.extensions.getByType(org.gradle.api.publish.PublishingExtension::class.java).apply {
                        repositories {
                            maven {
                                name = "LocalRepo"
                                url = project.uri("build/maven-repo") // Root project's repository
                            }
                        }
                    }
                    subproject.logger.quiet("📦 Configured ${subproject.path} to publish to root project repository")
                }
            }
        }
    }
}