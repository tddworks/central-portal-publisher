package com.tddworks.sonatype.publish.portal.plugin.publication

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import org.gradle.api.Project

/**
 * Orchestrates plugin detection and configuration using registered strategies.
 *
 * This class implements the Chain of Responsibility pattern combined with Strategy pattern:
 * - Maintains a registry of configuration strategies
 * - Iterates through strategies in priority order to find one that can handle the project
 * - Applies the first matching strategy
 *
 * This design follows the Open/Closed Principle:
 * - Open for extension: New plugin types can be added by registering new strategies
 * - Closed for modification: Core logic never needs to change
 */
class PluginBasedConfigurator {

    private val strategies = mutableListOf<PluginConfigurationStrategy>()

    /**
     * Registers a new configuration strategy.
     *
     * Strategies are automatically sorted by priority (highest first) when accessed.
     *
     * @param strategy The strategy to register
     */
    fun registerStrategy(strategy: PluginConfigurationStrategy) {
        strategies.add(strategy)
        // Keep strategies sorted by priority (highest first)
        strategies.sortByDescending { it.getPriority() }
    }

    /**
     * Configures publications based on applied Gradle plugins.
     *
     * This method:
     * 1. Iterates through registered strategies in priority order
     * 2. Finds the first strategy that can handle the project
     * 3. Applies that strategy's configuration
     * 4. Returns the result of the configuration attempt
     *
     * @param project The Gradle project to configure
     * @param config The Central Publisher configuration
     * @return Configuration result indicating success/failure and details
     */
    fun configureBasedOnAppliedPlugins(
        project: Project,
        config: CentralPublisherConfig,
    ): ConfigurationResult {

        // Find the first strategy that can handle this project
        val applicableStrategy = strategies.firstOrNull { strategy -> strategy.canHandle(project) }

        return if (applicableStrategy != null) {
            try {
                applicableStrategy.configure(project, config)
                ConfigurationResult.success(
                    detectedPluginType = applicableStrategy.getPluginType(),
                    appliedStrategy = applicableStrategy,
                )
            } catch (e: Exception) {
                ConfigurationResult.failure(
                    "Failed to configure ${applicableStrategy.getPluginType()} project: ${e.message}"
                )
            }
        } else {
            ConfigurationResult.noCompatiblePlugin(project.path)
        }
    }

    /** Returns all registered strategies for testing and introspection. */
    fun getRegisteredStrategies(): List<PluginConfigurationStrategy> {
        return strategies.toList()
    }

    /** Clears all registered strategies. Useful for testing. */
    fun clearStrategies() {
        strategies.clear()
    }
}
