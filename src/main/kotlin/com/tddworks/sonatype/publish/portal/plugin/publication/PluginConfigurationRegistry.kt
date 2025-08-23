package com.tddworks.sonatype.publish.portal.plugin.publication

import com.tddworks.sonatype.publish.portal.plugin.publication.strategies.JavaLibraryConfigurationStrategy
import com.tddworks.sonatype.publish.portal.plugin.publication.strategies.KotlinJvmConfigurationStrategy
import com.tddworks.sonatype.publish.portal.plugin.publication.strategies.KotlinMultiplatformConfigurationStrategy

/**
 * Registry that provides default plugin configuration strategies.
 *
 * This class serves as a factory for creating a configured PluginBasedConfigurator with all the
 * standard strategies pre-registered.
 */
object PluginConfigurationRegistry {

    /**
     * Creates a PluginBasedConfigurator with all standard strategies registered.
     *
     * The strategies are registered in priority order:
     * 1. Kotlin Multiplatform (priority 20) - highest priority, handles KMP projects
     * 2. Kotlin JVM (priority 10) - handles Kotlin JVM projects
     * 3. Java Library (priority 5) - handles Java Library and plain Java projects
     *
     * @return A configured PluginBasedConfigurator ready for use
     */
    fun createWithStandardStrategies(): PluginBasedConfigurator {
        val configurator = PluginBasedConfigurator()

        // Register strategies in any order - they will be sorted by priority
        configurator.registerStrategy(KotlinMultiplatformConfigurationStrategy())
        configurator.registerStrategy(KotlinJvmConfigurationStrategy())
        configurator.registerStrategy(JavaLibraryConfigurationStrategy())

        return configurator
    }

    /**
     * Creates an empty PluginBasedConfigurator for custom configuration.
     *
     * Use this when you want to register only specific strategies or add custom strategies not
     * included in the standard set.
     *
     * @return An empty PluginBasedConfigurator
     */
    fun createEmpty(): PluginBasedConfigurator {
        return PluginBasedConfigurator()
    }
}
