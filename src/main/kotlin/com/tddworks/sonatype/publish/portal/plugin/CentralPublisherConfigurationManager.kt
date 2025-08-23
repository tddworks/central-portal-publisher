package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.dsl.CentralPublisherExtension
import com.tddworks.sonatype.publish.portal.plugin.validation.ValidationEngine
import com.tddworks.sonatype.publish.portal.plugin.validation.ValidationResult
import org.gradle.api.Project

/**
 * Manages configuration resolution and validation for Central Publisher plugin.
 *
 * Responsibilities:
 * - Determine if publishing setup should be performed
 * - Resolve configuration from multiple sources (DSL, properties, auto-detection)
 * - Validate final configuration
 *
 * This class encapsulates the configuration logic that was previously in the main plugin, following
 * the developer mental model: "Figure out what to publish"
 */
class CentralPublisherConfigurationManager(
    private val project: Project,
    private val extension: CentralPublisherExtension,
) {

    /**
     * Determines if the plugin should set up publishing based on available configuration.
     *
     * Developer mental model: "Should I try to publish or wait for more config?"
     *
     * @return true if publishing should be set up, false if more configuration is needed
     */
    fun shouldSetupPublishing(): Boolean {
        return extension.hasExplicitConfiguration()
    }

    /**
     * Resolves the final configuration from all available sources.
     *
     * Developer mental model: "Give me the complete configuration to use"
     *
     * @return resolved configuration ready for validation and use
     */
    fun resolveConfiguration(): CentralPublisherConfig {
        return extension.build()
    }

    /**
     * Validates the resolved configuration and provides actionable feedback.
     *
     * Developer mental model: "Tell me what's wrong and how to fix it"
     *
     * @return validation result with errors, warnings, and suggestions
     */
    fun validateConfiguration(): ValidationResult {
        val config = resolveConfiguration()
        val validationEngine = ValidationEngine()
        return validationEngine.validate(config)
    }
}
