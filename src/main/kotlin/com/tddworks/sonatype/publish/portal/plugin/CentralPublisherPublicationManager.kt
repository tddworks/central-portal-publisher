package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.publication.ConfigurationResult
import com.tddworks.sonatype.publish.portal.plugin.publication.PluginConfigurationRegistry
import com.tddworks.sonatype.publish.portal.plugin.publication.PublicationProviderRegistry
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.configure

/**
 * Manages publication configuration for Central Publisher plugin.
 * 
 * Responsibilities:
 * - Configure publications based on detected project types
 * - Set up local repositories for deployment bundle generation
 * - Create publication-related tasks
 * - Leverage existing strategy pattern architecture
 * 
 * This class encapsulates the publication logic that was previously in the main plugin,
 * following the developer mental model: "Configure the artifacts to publish"
 */
class CentralPublisherPublicationManager(
    private val project: Project
) {
    
    companion object {
        const val PLUGIN_GROUP = "Central Publishing"
    }
    
    /**
     * Configures publications based on the project's applied plugins.
     * 
     * Developer mental model: "Set up what gets published based on my project type"
     * 
     * @param config the resolved configuration to use for publication setup
     * @return configuration result indicating success/failure and detected plugin type
     */
    fun configurePublications(config: CentralPublisherConfig): ConfigurationResult {
        // Use the existing OCP-compliant plugin detection system
        val pluginConfigurator = PluginConfigurationRegistry.createWithStandardStrategies()
        val configurationResult = pluginConfigurator.configureBasedOnAppliedPlugins(project, config)
        
        if (configurationResult.isConfigured) {
            project.logger.quiet("✅ Auto-configured for ${configurationResult.detectedPluginType} project")
            
            // Set up local repository and tasks if maven-publish is available
            setupLocalRepositoryAndTasks()
        } else {
            project.logger.warn("⚠️ ${configurationResult.reason}")
            project.logger.warn("   Using fallback publication configuration")
            
            // Fallback to existing system for compatibility
            val publicationRegistry = PublicationProviderRegistry()
            publicationRegistry.configurePublications(project, config)
        }
        
        return configurationResult
    }
    
    private fun setupLocalRepositoryAndTasks() {
        // Only configure task dependencies - LocalRepo repository is set up by TaskManager
        if (project.plugins.hasPlugin("maven-publish")) {
            // Set up task dependencies for publishToLocalRepo task if it exists
            project.afterEvaluate {
                val publishToLocalRepo = project.tasks.findByName("publishToLocalRepo")
                if (publishToLocalRepo != null) {
                    val publishTasks = project.tasks.matching { task ->
                        task.name.matches(Regex("publish.+Publication[s]?ToLocalRepoRepository"))
                    }
                    
                    // Also ensure signing tasks run if signing is configured
                    val signingTasks = project.tasks.matching { task ->
                        task.name.startsWith("sign") && task.name.endsWith("Publication")
                    }
                    
                    publishToLocalRepo.dependsOn(publishTasks + signingTasks)
                }
            }
        }
    }
}