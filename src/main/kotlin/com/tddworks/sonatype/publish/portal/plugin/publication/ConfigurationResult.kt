package com.tddworks.sonatype.publish.portal.plugin.publication

/**
 * Result of plugin-based configuration attempt.
 *
 * This class encapsulates the outcome of trying to configure publications based on detected Gradle
 * plugins, providing clear feedback about what happened.
 */
data class ConfigurationResult(
    /** Whether the project was successfully configured for publishing. */
    val isConfigured: Boolean,

    /**
     * The type of plugin that was detected and configured (e.g., "kotlin-jvm", "java-library").
     * Null if no compatible plugin was found.
     */
    val detectedPluginType: String?,

    /**
     * Human-readable reason for the configuration result. Useful for logging and debugging when
     * configuration fails.
     */
    val reason: String,

    /** The strategy that was used for configuration. Null if no strategy was applied. */
    val appliedStrategy: PluginConfigurationStrategy? = null,
) {
    companion object {
        /** Creates a successful configuration result. */
        fun success(
            detectedPluginType: String,
            appliedStrategy: PluginConfigurationStrategy,
        ): ConfigurationResult {
            return ConfigurationResult(
                isConfigured = true,
                detectedPluginType = detectedPluginType,
                reason = "Successfully configured publications for $detectedPluginType project",
                appliedStrategy = appliedStrategy,
            )
        }

        /** Creates a failed configuration result. */
        fun failure(reason: String): ConfigurationResult {
            return ConfigurationResult(
                isConfigured = false,
                detectedPluginType = null,
                reason = reason,
            )
        }

        /** Creates a result for when no compatible plugins are found. */
        fun noCompatiblePlugin(projectPath: String): ConfigurationResult {
            return failure("No compatible plugin found in project $projectPath for publishing")
        }
    }
}
