package com.tddworks.sonatype.publish.portal.plugin.defaults

import com.tddworks.sonatype.publish.portal.plugin.config.*
import org.gradle.api.Project

/**
 * Interface for providing intelligent default values for configuration.
 * 
 * Smart defaults are applied when neither DSL configuration nor auto-detection
 * can provide a value. They are context-aware and adapt based on project type,
 * structure, and other environmental factors.
 */
interface SmartDefaultProvider {
    
    /**
     * The name of this default provider for logging and debugging.
     */
    val name: String
    
    /**
     * Priority of this provider. Higher values take precedence.
     * 
     * Standard priorities:
     * - 100: Framework-specific defaults (Android, Spring Boot, etc.)
     * - 50: Language-specific defaults (Kotlin, Java, etc.) 
     * - 10: Generic defaults
     */
    val priority: Int
    
    /**
     * Determines if this provider can provide defaults for the given project.
     */
    fun canProvideDefaults(project: Project): Boolean
    
    /**
     * Provides default configuration values for the project.
     * 
     * Only returns values that this provider can confidently default.
     * Returns partial configuration - other providers may fill in missing values.
     * 
     * @param project The Gradle project
     * @param existingConfig Current configuration state (from DSL, properties, auto-detection)
     * @return Partial configuration with default values
     */
    fun provideDefaults(project: Project, existingConfig: CentralPublisherConfig): CentralPublisherConfig
}

/**
 * Manages multiple smart default providers and applies them in priority order.
 */
class SmartDefaultManager(private val providers: List<SmartDefaultProvider> = emptyList()) {
    
    /**
     * Creates a manager with standard default providers.
     */
    constructor(project: Project) : this(
        listOf(
            GenericProjectDefaultProvider()
        ).filter { it.canProvideDefaults(project) }
            .sortedByDescending { it.priority }
    )
    
    /**
     * Applies smart defaults to the configuration.
     * 
     * Providers are applied in priority order (highest first).
     * Each provider can override values set by lower-priority providers.
     * 
     * @param project The Gradle project
     * @param baseConfig Current configuration from DSL, properties, and auto-detection
     * @return Configuration enhanced with smart defaults
     */
    fun applySmartDefaults(project: Project, baseConfig: CentralPublisherConfig): CentralPublisherConfig {
        // Apply providers in reverse priority order (lowest priority first)
        // This way, higher priority providers can override lower priority values
        return providers.reversed().fold(baseConfig) { config, provider ->
            if (provider.canProvideDefaults(project)) {
                val defaults = provider.provideDefaults(project, config)
                mergeConfigurations(config, defaults)
            } else {
                config
            }
        }
    }
    
    /**
     * Gets information about which providers are active for this project.
     */
    fun getActiveProviders(project: Project): List<SmartDefaultProvider> {
        return providers.filter { it.canProvideDefaults(project) }
    }
    
    private fun mergeConfigurations(
        base: CentralPublisherConfig, 
        defaults: CentralPublisherConfig
    ): CentralPublisherConfig {
        return base.copy(
            credentials = mergeCredentials(base.credentials, defaults.credentials),
            projectInfo = mergeProjectInfo(base.projectInfo, defaults.projectInfo),
            signing = mergeSigning(base.signing, defaults.signing),
            publishing = mergePublishing(base.publishing, defaults.publishing)
        )
    }
    
    private fun mergeCredentials(
        base: CredentialsConfig,
        defaults: CredentialsConfig
    ): CredentialsConfig {
        return base.copy(
            username = base.username.ifBlank { defaults.username },
            password = base.password.ifBlank { defaults.password }
        )
    }
    
    private fun mergeProjectInfo(
        base: ProjectInfoConfig,
        defaults: ProjectInfoConfig
    ): ProjectInfoConfig {
        return base.copy(
            name = base.name.ifBlank { defaults.name },
            description = base.description.ifBlank { defaults.description },
            url = base.url.ifBlank { defaults.url },
            scm = mergeScm(base.scm, defaults.scm),
            license = mergeLicense(base.license, defaults.license),
            developers = if (base.developers.isEmpty()) defaults.developers else base.developers
        )
    }
    
    private fun mergeScm(
        base: ScmConfig,
        defaults: ScmConfig
    ): ScmConfig {
        return base.copy(
            url = base.url.ifBlank { defaults.url },
            connection = base.connection.ifBlank { defaults.connection },
            developerConnection = base.developerConnection.ifBlank { defaults.developerConnection }
        )
    }
    
    private fun mergeLicense(
        base: LicenseConfig,
        defaults: LicenseConfig
    ): LicenseConfig {
        return base.copy(
            name = base.name.ifBlank { defaults.name },
            url = base.url.ifBlank { defaults.url },
            distribution = base.distribution.ifBlank { defaults.distribution }
        )
    }
    
    private fun mergeSigning(
        base: SigningConfig,
        defaults: SigningConfig
    ): SigningConfig {
        return base.copy(
            keyId = base.keyId.ifBlank { defaults.keyId },
            password = base.password.ifBlank { defaults.password },
            secretKeyRingFile = base.secretKeyRingFile.ifBlank { defaults.secretKeyRingFile }
        )
    }
    
    private fun mergePublishing(
        base: PublishingConfig,
        defaults: PublishingConfig
    ): PublishingConfig {
        return base.copy(
            // For boolean values, use base value unless it's the default false
            autoPublish = base.autoPublish || defaults.autoPublish,
            aggregation = if (base.aggregation != defaults.aggregation) base.aggregation else defaults.aggregation,
            dryRun = base.dryRun || defaults.dryRun
        )
    }
}