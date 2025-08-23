package com.tddworks.sonatype.publish.portal.plugin.publication

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import org.gradle.api.Project

/**
 * Strategy interface for configuring publications based on specific Gradle plugin types.
 *
 * This interface follows the Strategy pattern and Open/Closed Principle:
 * - Open for extension: New plugin types can be added by implementing this interface
 * - Closed for modification: Core plugin logic never needs to change
 *
 * Each implementation handles configuration for a specific project type (Kotlin JVM, Java Library,
 * etc.)
 */
interface PluginConfigurationStrategy {

    /**
     * Determines if this strategy can handle the given project based on applied plugins.
     *
     * @param project The Gradle project to check
     * @return true if this strategy should handle the project, false otherwise
     */
    fun canHandle(project: Project): Boolean

    /**
     * Configures publications and publishing setup for the project.
     *
     * This method should:
     * - Apply necessary Gradle plugins (maven-publish, signing, etc.)
     * - Configure publications with appropriate artifacts (sources, javadoc)
     * - Set up any plugin-specific publishing configuration
     *
     * @param project The Gradle project to configure
     * @param config The Central Publisher configuration
     * @param showMessages whether to show configuration messages
     */
    fun configure(project: Project, config: CentralPublisherConfig, showMessages: Boolean = true)

    /**
     * Returns a human-readable identifier for the plugin type this strategy handles.
     *
     * @return Plugin type identifier (e.g., "kotlin-jvm", "java-library", "kotlin-multiplatform")
     */
    fun getPluginType(): String

    /**
     * Returns the priority of this strategy when multiple strategies can handle a project. Higher
     * numbers = higher priority.
     *
     * Default priority is 0. Override for strategies that should take precedence. Example: Kotlin
     * Multiplatform should have higher priority than Kotlin JVM.
     *
     * @return Priority value (higher = more priority)
     */
    fun getPriority(): Int = 0
}
