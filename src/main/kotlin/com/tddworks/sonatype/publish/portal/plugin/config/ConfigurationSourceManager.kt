package com.tddworks.sonatype.publish.portal.plugin.config

import com.tddworks.sonatype.publish.portal.plugin.defaults.SmartDefaultManager
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import org.gradle.api.Project

/**
 * Manages loading configuration from multiple sources with proper precedence. Supports DSL,
 * properties files, environment variables, and auto-detected values.
 */
class ConfigurationSourceManager(private val project: Project) {
    private val configurationCache = ConcurrentHashMap<String, CachedConfiguration>()
    private val validationErrors = mutableListOf<ConfigurationValidationError>()
    private val sourceDiagnostics = ConfigurationSourceDiagnostics()
    private var cacheHitCount = 0

    /** Default property mappings for standard gradle.properties format */
    private val defaultPropertyMappings =
        mapOf(
            // Credentials
            "SONATYPE_USERNAME" to "credentials.username",
            "SONATYPE_PASSWORD" to "credentials.password",

            // Project Info
            "POM_NAME" to "projectInfo.name",
            "POM_DESCRIPTION" to "projectInfo.description",
            "POM_URL" to "projectInfo.url",
            "POM_SCM_URL" to "projectInfo.scm.url",
            "POM_SCM_CONNECTION" to "projectInfo.scm.connection",
            "POM_SCM_DEV_CONNECTION" to "projectInfo.scm.developerConnection",
            "POM_LICENCE_NAME" to "projectInfo.license.name",
            "POM_LICENCE_URL" to "projectInfo.license.url",
            "POM_LICENCE_DIST" to "projectInfo.license.distribution",
            "POM_DEVELOPER_ID" to "projectInfo.developer.id",
            "POM_DEVELOPER_NAME" to "projectInfo.developer.name",
            "POM_DEVELOPER_EMAIL" to "projectInfo.developer.email",
            "POM_DEVELOPER_ORGANIZATION" to "projectInfo.developer.organization",
            "POM_DEVELOPER_ORGANIZATION_URL" to "projectInfo.developer.organizationUrl",

            // Signing
            "signing.keyId" to "signing.keyId",
            "signing.password" to "signing.password",
            "signing.secretKeyRingFile" to "signing.secretKeyRingFile",

            // Publishing
            "autoPublish" to "publishing.autoPublish",
            "aggregation" to "publishing.aggregation",
        )

    /** Environment variable mappings */
    private val environmentMappings =
        mapOf(
            "SONATYPE_USERNAME" to "credentials.username",
            "SONATYPE_PASSWORD" to "credentials.password",
            "SIGNING_KEY" to "signing.keyId",
            "SIGNING_PASSWORD" to "signing.password",
        )

    /**
     * Loads configuration with proper precedence: DSL > Properties > Environment > Auto-Detected >
     * Smart-Defaults > Defaults
     */
    fun loadConfigurationWithPrecedence(
        dslConfig: CentralPublisherConfig? = null,
        propertiesFile: String? = null,
        enableAutoDetection: Boolean = true,
    ): CentralPublisherConfig {
        sourceDiagnostics.clear()

        // Start with empty defaults
        var config = CentralPublisherConfigBuilder().build()
        val usedSources = mutableSetOf<ConfigurationSource>()

        // Apply auto-detected values if enabled
        if (enableAutoDetection) {
            val autoDetected = loadAutoDetected()
            if (hasNonEmptyValues(autoDetected)) {
                config = config.mergeWith(autoDetected)
                usedSources.add(ConfigurationSource.AUTO_DETECTED)
                sourceDiagnostics.recordSource(ConfigurationSource.AUTO_DETECTED)
            }
        }

        // Apply smart defaults
        val smartDefaultManager = SmartDefaultManager(project)
        val smartDefaults = smartDefaultManager.applySmartDefaults(project, config)
        if (smartDefaults != config) { // Only record if smart defaults actually changed something
            config = smartDefaults
            usedSources.add(ConfigurationSource.SMART_DEFAULTS)
            sourceDiagnostics.recordSource(ConfigurationSource.SMART_DEFAULTS)
        }

        // Apply environment variables
        val envConfig = loadFromEnvironment()
        if (hasNonEmptyValues(envConfig)) {
            config = config.mergeWith(envConfig)
            usedSources.add(ConfigurationSource.ENVIRONMENT)
            sourceDiagnostics.recordSource(ConfigurationSource.ENVIRONMENT)
        }

        // Apply properties file
        propertiesFile?.let {
            val propsConfig = loadFromProperties(it)
            if (hasNonEmptyValues(propsConfig)) {
                config = config.mergeWith(propsConfig)
                usedSources.add(ConfigurationSource.PROPERTIES)
                sourceDiagnostics.recordSource(ConfigurationSource.PROPERTIES)
            }
        }

        // Apply DSL configuration (highest precedence)
        dslConfig?.let {
            if (hasNonEmptyValues(it)) {
                // Record DSL values in diagnostics
                if (!it.credentials.username.isEmpty()) {
                    sourceDiagnostics.recordValue(
                        "credentials.username",
                        it.credentials.username,
                        ConfigurationSource.DSL,
                    )
                }
                if (!it.credentials.password.isEmpty()) {
                    sourceDiagnostics.recordValue(
                        "credentials.password",
                        it.credentials.password,
                        ConfigurationSource.DSL,
                    )
                }
                config = config.mergeWith(it)
                usedSources.add(ConfigurationSource.DSL)
                sourceDiagnostics.recordSource(ConfigurationSource.DSL)
            }
        }

        // Create final config with only the used sources
        return config.copy(metadata = config.metadata.copy(sources = usedSources))
    }

    /** Loads configuration from a DSL config object */
    fun loadConfiguration(dslConfig: CentralPublisherConfig): CentralPublisherConfig {
        sourceDiagnostics.recordSource(ConfigurationSource.DSL)
        return dslConfig
    }

    /** Loads configuration from properties file */
    fun loadFromProperties(
        filePath: String,
        propertyMappings: Map<String, String> = defaultPropertyMappings,
        validateOnLoad: Boolean = false,
    ): CentralPublisherConfig {
        val cacheKey = "props:$filePath"

        // Check cache first
        val file = File(filePath)
        if (file.exists()) {
            val lastModified = Files.getLastModifiedTime(file.toPath())
            configurationCache[cacheKey]?.let { cached ->
                if (cached.lastModified >= lastModified) {
                    cacheHitCount++
                    return cached.config
                }
            }
        }

        val builder = CentralPublisherConfigBuilder().withSource(ConfigurationSource.PROPERTIES)

        if (!file.exists()) {
            return builder.build()
        }

        try {
            val properties = Properties().apply { file.inputStream().use { load(it) } }

            // Convert Properties to Map<String, String>
            val propertiesMap =
                properties.entries.associate { it.key.toString() to it.value.toString() }

            // Apply property mappings using the new method
            val configBuilder = applyPropertiesToBuilder(propertiesMap, propertyMappings)
            val config = configBuilder.build()

            // Validate if requested
            if (validateOnLoad) {
                validateConfiguration(config)
            }

            // Cache the result
            configurationCache[cacheKey] =
                CachedConfiguration(
                    config = config,
                    lastModified = Files.getLastModifiedTime(file.toPath()),
                )

            return config
        } catch (e: Exception) {
            validationErrors.add(
                ConfigurationValidationError(
                    source = ConfigurationSource.PROPERTIES,
                    message = "Failed to load properties from $filePath: ${e.message}",
                    exception = e,
                )
            )
            return builder.build()
        }
    }

    /** Loads configuration from environment variables */
    fun loadFromEnvironment(): CentralPublisherConfig {
        // Convert environment variables to properties map and apply them
        val envProperties = mutableMapOf<String, String>()
        environmentMappings.forEach { (envVar, configPath) ->
            val value = System.getProperty(envVar) ?: System.getenv(envVar)
            if (!value.isNullOrBlank()) {
                envProperties[envVar] = value
                // Note: diagnostics will be recorded in applyPropertiesToBuilder
            }
        }

        if (envProperties.isNotEmpty()) {
            return applyPropertiesToBuilder(
                    envProperties,
                    environmentMappings,
                    ConfigurationSource.ENVIRONMENT,
                )
                .build()
        }

        return CentralPublisherConfigBuilder().withSource(ConfigurationSource.ENVIRONMENT).build()
    }

    /** Loads auto-detected configuration using available detectors */
    private fun loadAutoDetected(): CentralPublisherConfig {
        val detectors =
            listOf(
                com.tddworks.sonatype.publish.portal.plugin.autodetection.GitInfoDetector(),
                com.tddworks.sonatype.publish.portal.plugin.autodetection.ProjectInfoDetector(),
            )

        val manager =
            com.tddworks.sonatype.publish.portal.plugin.autodetection.AutoDetectionManager(
                detectors
            )
        val summary = manager.detectConfiguration(project)

        return summary.config.copy(
            metadata =
                summary.config.metadata.copy(sources = setOf(ConfigurationSource.AUTO_DETECTED))
        )
    }

    /** Applies property values to create a new configuration builder */
    private fun applyPropertiesToBuilder(
        properties: Map<String, String>,
        propertyMappings: Map<String, String>,
        source: ConfigurationSource = ConfigurationSource.PROPERTIES,
    ): CentralPublisherConfigBuilder {
        val builder = CentralPublisherConfigBuilder().withSource(source)

        // Group properties by section to build each section once
        val credentialProps = mutableMapOf<String, String>()
        val projectProps = mutableMapOf<String, String>()
        val scmProps = mutableMapOf<String, String>()
        val licenseProps = mutableMapOf<String, String>()
        val developerProps = mutableMapOf<String, String>()
        val signingProps = mutableMapOf<String, String>()
        val publishingProps = mutableMapOf<String, String>()

        propertyMappings.forEach { (propKey, configPath) ->
            val value = properties[propKey]
            if (!value.isNullOrBlank()) {
                sourceDiagnostics.recordValue(configPath, value, source)

                val parts = configPath.split(".")
                when {
                    configPath.startsWith("credentials.") -> credentialProps[parts[1]] = value
                    configPath.startsWith("projectInfo.") && parts.size == 2 ->
                        projectProps[parts[1]] = value
                    configPath.startsWith("projectInfo.scm.") -> scmProps[parts[2]] = value
                    configPath.startsWith("projectInfo.license.") -> licenseProps[parts[2]] = value
                    configPath.startsWith("projectInfo.developer.") ->
                        developerProps[parts[2]] = value
                    configPath.startsWith("signing.") -> signingProps[parts[1]] = value
                    configPath.startsWith("publishing.") -> publishingProps[parts[1]] = value
                }
            }
        }

        // Apply grouped properties
        if (credentialProps.isNotEmpty()) {
            builder.credentials {
                credentialProps["username"]?.let { username = it }
                credentialProps["password"]?.let { password = it }
            }
        }

        if (
            projectProps.isNotEmpty() ||
                scmProps.isNotEmpty() ||
                licenseProps.isNotEmpty() ||
                developerProps.isNotEmpty()
        ) {
            builder.projectInfo {
                projectProps["name"]?.let { name = it }
                projectProps["description"]?.let { description = it }
                projectProps["url"]?.let { url = it }

                if (scmProps.isNotEmpty()) {
                    scm {
                        scmProps["url"]?.let { url = it }
                        scmProps["connection"]?.let { connection = it }
                        scmProps["developerConnection"]?.let { developerConnection = it }
                    }
                }

                if (licenseProps.isNotEmpty()) {
                    license {
                        licenseProps["name"]?.let { name = it }
                        licenseProps["url"]?.let { url = it }
                        licenseProps["distribution"]?.let { distribution = it }
                    }
                }

                if (developerProps.isNotEmpty()) {
                    developer {
                        developerProps["id"]?.let { id = it }
                        developerProps["name"]?.let { name = it }
                        developerProps["email"]?.let { email = it }
                        developerProps["organization"]?.let { organization = it }
                        developerProps["organizationUrl"]?.let { organizationUrl = it }
                    }
                }
            }
        }

        if (signingProps.isNotEmpty()) {
            builder.signing {
                signingProps["keyId"]?.let { keyId = it }
                signingProps["password"]?.let { password = it }
                signingProps["secretKeyRingFile"]?.let { secretKeyRingFile = it }
            }
        }

        if (publishingProps.isNotEmpty()) {
            builder.publishing {
                publishingProps["autoPublish"]?.let { autoPublish = it.toBoolean() }
                publishingProps["aggregation"]?.let { aggregation = it.toBoolean() }
            }
        }

        return builder
    }

    /** Checks if a configuration has any non-empty values */
    private fun hasNonEmptyValues(config: CentralPublisherConfig): Boolean {
        return !config.credentials.username.isEmpty() ||
            !config.credentials.password.isEmpty() ||
            !config.projectInfo.name.isEmpty() ||
            !config.projectInfo.description.isEmpty() ||
            !config.projectInfo.url.isEmpty() ||
            !config.signing.keyId.isEmpty() ||
            !config.signing.password.isEmpty() ||
            !config.signing.secretKeyRingFile.isEmpty() ||
            config.publishing.autoPublish ||
            config.publishing.aggregation != true || // Default is true, so false means it was set
            config.publishing.dryRun
    }

    /** Applies a configuration to a builder, setting individual non-empty values only */
    private fun applyConfigToBuilder(
        builder: CentralPublisherConfigBuilder,
        config: CentralPublisherConfig,
    ) {
        // Apply credentials individually
        if (!config.credentials.username.isEmpty()) {
            builder.credentials { username = config.credentials.username }
        }
        if (!config.credentials.password.isEmpty()) {
            builder.credentials { password = config.credentials.password }
        }

        // Apply project info individually
        if (!config.projectInfo.name.isEmpty()) {
            builder.projectInfo { name = config.projectInfo.name }
        }
        if (!config.projectInfo.description.isEmpty()) {
            builder.projectInfo { description = config.projectInfo.description }
        }
        if (!config.projectInfo.url.isEmpty()) {
            builder.projectInfo { url = config.projectInfo.url }
        }

        // Apply signing individually
        if (!config.signing.keyId.isEmpty()) {
            builder.signing { keyId = config.signing.keyId }
        }
        if (!config.signing.password.isEmpty()) {
            builder.signing { password = config.signing.password }
        }
        if (!config.signing.secretKeyRingFile.isEmpty()) {
            builder.signing { secretKeyRingFile = config.signing.secretKeyRingFile }
        }

        // Apply publishing (these are boolean/enum values that have defaults)
        if (config.publishing.autoPublish) {
            builder.publishing { autoPublish = config.publishing.autoPublish }
        }
        if (config.publishing.aggregation != true) { // Default is true, so false means it was set
            builder.publishing { aggregation = config.publishing.aggregation }
        }
        if (config.publishing.dryRun) {
            builder.publishing { dryRun = config.publishing.dryRun }
        }
    }

    /** Validates a configuration and records any errors */
    private fun validateConfiguration(config: CentralPublisherConfig) {
        try {
            config.validate()
        } catch (e: ConfigurationException) {
            val errorMessages =
                e.message?.split("\n")?.drop(1)
                    ?: emptyList() // Skip first line "Configuration validation failed:"
            errorMessages.forEach { errorMessage ->
                if (errorMessage.startsWith("- ")) {
                    validationErrors.add(
                        ConfigurationValidationError(
                            source = ConfigurationSource.PROPERTIES,
                            // Remove "- " prefix
                            message = errorMessage.substring(2),
                            exception = e,
                        )
                    )
                }
            }
        }
    }

    /** Returns validation errors encountered during loading */
    fun getValidationErrors(): List<ConfigurationValidationError> = validationErrors.toList()

    /** Returns the number of cache hits */
    fun getCacheHitCount(): Int = cacheHitCount

    /** Returns diagnostics about configuration sources */
    fun getSourceDiagnostics(): ConfigurationSourceDiagnostics = sourceDiagnostics
}

/** Cached configuration with timestamp */
private data class CachedConfiguration(
    val config: CentralPublisherConfig,
    val lastModified: FileTime,
)

/** Configuration validation error */
data class ConfigurationValidationError(
    val source: ConfigurationSource,
    val message: String,
    val exception: Throwable? = null,
)

/** Diagnostics information about configuration sources */
class ConfigurationSourceDiagnostics {
    private val _configurationSources = mutableSetOf<ConfigurationSource>()
    private val _sourceValues =
        mutableMapOf<String, MutableList<Pair<String, ConfigurationSource>>>()

    val configurationSources: Set<ConfigurationSource>
        get() = _configurationSources

    val precedenceOrder =
        listOf(
            ConfigurationSource.DSL,
            ConfigurationSource.PROPERTIES,
            ConfigurationSource.ENVIRONMENT,
            ConfigurationSource.AUTO_DETECTED,
            ConfigurationSource.SMART_DEFAULTS,
            ConfigurationSource.DEFAULTS,
        )

    val sourceValues: Map<String, List<Pair<String, ConfigurationSource>>>
        get() = _sourceValues.mapValues { it.value.toList() }

    fun recordSource(source: ConfigurationSource) {
        _configurationSources.add(source)
    }

    fun recordValue(configPath: String, value: String, source: ConfigurationSource) {
        _sourceValues.getOrPut(configPath) { mutableListOf() }.add(value to source)
    }

    fun finalValue(configPath: String): String? {
        return sourceValues[configPath]
            ?.sortedBy { (_, source) -> precedenceOrder.indexOf(source) }
            ?.firstOrNull()
            ?.first
    }

    fun clear() {
        _configurationSources.clear()
        _sourceValues.clear()
    }
}
