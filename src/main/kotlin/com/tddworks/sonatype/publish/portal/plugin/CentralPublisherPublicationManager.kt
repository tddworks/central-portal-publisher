package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.publication.ConfigurationResult
import com.tddworks.sonatype.publish.portal.plugin.publication.PluginConfigurationRegistry
import com.tddworks.sonatype.publish.portal.plugin.publication.PublicationProviderRegistry
import org.gradle.api.Project

/**
 * Manages publication configuration for Central Publisher plugin.
 *
 * Responsibilities:
 * - Configure publications based on detected project types using strategy pattern
 * - Leverage existing OCP-compliant plugin detection system
 * - Provide fallback configuration for unsupported project types
 *
 * This class encapsulates the publication logic that was previously in the main plugin, following
 * the developer mental model: "Configure the artifacts to publish"
 *
 * Note: Local repository setup and task creation is handled by CentralPublisherTaskManager
 */
class CentralPublisherPublicationManager(private val project: Project) {

    /**
     * Configures publications based on the project's applied plugins.
     *
     * Developer mental model: "Set up what gets published based on my project type"
     *
     * @param config the resolved configuration to use for publication setup
     * @param showMessages whether to show configuration messages
     * @return configuration result indicating success/failure and detected plugin type
     */
    fun configurePublications(
        config: CentralPublisherConfig,
        showMessages: Boolean = true,
    ): ConfigurationResult {
        // Use the existing OCP-compliant plugin detection system
        val pluginConfigurator = PluginConfigurationRegistry.createWithStandardStrategies()
        val configurationResult =
            pluginConfigurator.configureBasedOnAppliedPlugins(project, config, showMessages)

        if (showMessages) {
            if (configurationResult.isConfigured) {
                project.logger.quiet(
                    "✅ Auto-configured for ${configurationResult.detectedPluginType} project"
                )
            } else {
                project.logger.warn("⚠️ ${configurationResult.reason}")
                project.logger.warn("   Using fallback publication configuration")
            }
        }

        if (!configurationResult.isConfigured) {
            // Fallback to existing system for compatibility
            val publicationRegistry = PublicationProviderRegistry()
            publicationRegistry.configurePublications(project, config)
        }

        return configurationResult
    }
}
