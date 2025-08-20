package com.tddworks.sonatype.publish.portal.plugin.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Comprehensive configuration model for the Central Portal Publisher plugin.
 * Supports validation, serialization, merging, and source tracking.
 */
@Serializable
data class CentralPublisherConfig(
    val credentials: CredentialsConfig = CredentialsConfig(),
    val projectInfo: ProjectInfoConfig = ProjectInfoConfig(),
    val signing: SigningConfig = SigningConfig(),
    val publishing: PublishingConfig = PublishingConfig(),
    val validation: ValidationConfig = ValidationConfig(),
    val autoDetection: AutoDetectionConfig = AutoDetectionConfig(),
    val metadata: ConfigurationMetadata = ConfigurationMetadata()
) {
    fun validate() {
        val errors = mutableListOf<String>()
        
        // Validate credentials
        if (credentials.username.isBlank()) {
            errors.add("Username cannot be empty")
        }
        if (credentials.password.isBlank()) {
            errors.add("Password cannot be empty")
        }
        
        // Validate project info
        if (projectInfo.name.isBlank()) {
            errors.add("Project name cannot be empty")
        }
        
        // Validate URLs
        if (projectInfo.url.isNotBlank() && !projectInfo.url.startsWith("http")) {
            errors.add("Project URL must be a valid HTTP/HTTPS URL")
        }
        
        if (errors.isNotEmpty()) {
            throw ConfigurationException("Configuration validation failed:\n" + 
                errors.joinToString("\n") { "- $it" })
        }
    }
    
    fun serialize(): String {
        return Json.encodeToString(this)
    }
    
    fun mergeWith(other: CentralPublisherConfig): CentralPublisherConfig {
        return CentralPublisherConfig(
            credentials = credentials.mergeWith(other.credentials),
            projectInfo = projectInfo.mergeWith(other.projectInfo),
            signing = signing.mergeWith(other.signing),
            publishing = publishing.mergeWith(other.publishing),
            validation = validation.mergeWith(other.validation),
            autoDetection = autoDetection.mergeWith(other.autoDetection),
            metadata = metadata.copy(
                sources = metadata.sources + other.metadata.sources,
                lastModified = Instant.now().toString()
            )
        )
    }
    
    companion object {
        fun deserialize(json: String): CentralPublisherConfig {
            return Json.decodeFromString(json)
        }
    }
}

@Serializable
data class CredentialsConfig(
    val username: String = "",
    val password: String = "",
    val loadFromEnvironment: Boolean = true
) {
    fun mergeWith(other: CredentialsConfig): CredentialsConfig {
        return CredentialsConfig(
            username = if (other.username.isNotBlank()) other.username else username,
            password = if (other.password.isNotBlank()) other.password else password,
            loadFromEnvironment = other.loadFromEnvironment
        )
    }
}

@Serializable
data class ProjectInfoConfig(
    val name: String = "",
    val description: String = "",
    val url: String = "",
    val scm: ScmConfig = ScmConfig(),
    val license: LicenseConfig = LicenseConfig(),
    val developers: List<DeveloperConfig> = emptyList(),
    val issueManagement: IssueManagementConfig = IssueManagementConfig()
) {
    fun mergeWith(other: ProjectInfoConfig): ProjectInfoConfig {
        return ProjectInfoConfig(
            name = if (other.name.isNotBlank()) other.name else name,
            description = if (other.description.isNotBlank()) other.description else description,
            url = if (other.url.isNotBlank()) other.url else url,
            scm = scm.mergeWith(other.scm),
            license = license.mergeWith(other.license),
            developers = if (other.developers.isNotEmpty()) other.developers else developers,
            issueManagement = issueManagement.mergeWith(other.issueManagement)
        )
    }
}

@Serializable
data class ScmConfig(
    val url: String = "",
    val connection: String = "",
    val developerConnection: String = ""
) {
    fun mergeWith(other: ScmConfig): ScmConfig {
        return ScmConfig(
            url = if (other.url.isNotBlank()) other.url else url,
            connection = if (other.connection.isNotBlank()) other.connection else connection,
            developerConnection = if (other.developerConnection.isNotBlank()) other.developerConnection else developerConnection
        )
    }
}

@Serializable
data class LicenseConfig(
    val name: String = "",
    val url: String = "",
    val distribution: String = "repo"
) {
    fun mergeWith(other: LicenseConfig): LicenseConfig {
        return LicenseConfig(
            name = if (other.name.isNotBlank()) other.name else name,
            url = if (other.url.isNotBlank()) other.url else url,
            distribution = if (other.distribution != "repo") other.distribution else distribution
        )
    }
}

@Serializable
data class DeveloperConfig(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val organization: String = "",
    val organizationUrl: String = ""
)

@Serializable
data class IssueManagementConfig(
    val system: String = "",
    val url: String = ""
) {
    fun mergeWith(other: IssueManagementConfig): IssueManagementConfig {
        return IssueManagementConfig(
            system = if (other.system.isNotBlank()) other.system else system,
            url = if (other.url.isNotBlank()) other.url else url
        )
    }
}

@Serializable
data class SigningConfig(
    val keyId: String = "",
    val password: String = "",
    val secretKeyRingFile: String = "",
    val useGpgAgent: Boolean = true,
    val autoDetect: Boolean = true
) {
    fun mergeWith(other: SigningConfig): SigningConfig {
        return SigningConfig(
            keyId = if (other.keyId.isNotBlank()) other.keyId else keyId,
            password = if (other.password.isNotBlank()) other.password else password,
            secretKeyRingFile = if (other.secretKeyRingFile.isNotBlank()) other.secretKeyRingFile else secretKeyRingFile,
            useGpgAgent = other.useGpgAgent,
            autoDetect = other.autoDetect
        )
    }
}

@Serializable
data class PublishingConfig(
    val autoPublish: Boolean = false,
    val dryRun: Boolean = false,
    val aggregation: Boolean = true,
    val publications: List<String> = emptyList(),
    val excludeModules: List<String> = emptyList()
) {
    fun mergeWith(other: PublishingConfig): PublishingConfig {
        return PublishingConfig(
            autoPublish = other.autoPublish,
            dryRun = other.dryRun,
            aggregation = other.aggregation,
            publications = if (other.publications.isNotEmpty()) other.publications else publications,
            excludeModules = if (other.excludeModules.isNotEmpty()) other.excludeModules else excludeModules
        )
    }
}

@Serializable
data class ValidationConfig(
    val enabled: Boolean = true,
    val strictMode: Boolean = false,
    val skipOnError: Boolean = false
) {
    fun mergeWith(other: ValidationConfig): ValidationConfig {
        return ValidationConfig(
            enabled = other.enabled,
            strictMode = other.strictMode,
            skipOnError = other.skipOnError
        )
    }
}

@Serializable
data class AutoDetectionConfig(
    val projectInfo: Boolean = true,
    val credentials: Boolean = true,
    val signing: Boolean = true,
    val gitInfo: Boolean = true
) {
    fun mergeWith(other: AutoDetectionConfig): AutoDetectionConfig {
        return AutoDetectionConfig(
            projectInfo = other.projectInfo,
            credentials = other.credentials,
            signing = other.signing,
            gitInfo = other.gitInfo
        )
    }
}

@Serializable
data class ConfigurationMetadata(
    val version: String = "1.0.0",
    val sources: Set<ConfigurationSource> = emptySet(),
    val lastModified: String = Instant.now().toString()
)

@Serializable
enum class ConfigurationSource {
    DSL,
    PROPERTIES,
    ENVIRONMENT,
    AUTO_DETECTED,
    SMART_DEFAULTS,
    DEFAULTS
}

/**
 * Builder for creating CentralPublisherConfig instances with a fluent DSL.
 */
class CentralPublisherConfigBuilder {
    private var credentials: CredentialsConfig = CredentialsConfig()
    private var projectInfo: ProjectInfoConfig = ProjectInfoConfig()
    private var signing: SigningConfig = SigningConfig()
    private var publishing: PublishingConfig = PublishingConfig()
    private var validation: ValidationConfig = ValidationConfig()
    private var autoDetection: AutoDetectionConfig = AutoDetectionConfig()
    private var sources: MutableSet<ConfigurationSource> = mutableSetOf()
    
    fun credentials(block: CredentialsConfigBuilder.() -> Unit): CentralPublisherConfigBuilder {
        credentials = CredentialsConfigBuilder().apply(block).build()
        return this
    }
    
    fun projectInfo(block: ProjectInfoConfigBuilder.() -> Unit): CentralPublisherConfigBuilder {
        projectInfo = ProjectInfoConfigBuilder().apply(block).build()
        return this
    }
    
    fun signing(block: SigningConfigBuilder.() -> Unit): CentralPublisherConfigBuilder {
        signing = SigningConfigBuilder().apply(block).build()
        return this
    }
    
    fun publishing(block: PublishingConfigBuilder.() -> Unit): CentralPublisherConfigBuilder {
        publishing = PublishingConfigBuilder().apply(block).build()
        return this
    }
    
    fun validation(block: ValidationConfigBuilder.() -> Unit): CentralPublisherConfigBuilder {
        validation = ValidationConfigBuilder().apply(block).build()
        return this
    }
    
    fun autoDetection(block: AutoDetectionConfigBuilder.() -> Unit): CentralPublisherConfigBuilder {
        autoDetection = AutoDetectionConfigBuilder().apply(block).build()
        return this
    }
    
    fun withSource(source: ConfigurationSource): CentralPublisherConfigBuilder {
        sources.add(source)
        return this
    }
    
    fun withSources(sourceList: List<ConfigurationSource>): CentralPublisherConfigBuilder {
        sources.addAll(sourceList)
        return this
    }
    
    fun build(): CentralPublisherConfig {
        return CentralPublisherConfig(
            credentials = credentials,
            projectInfo = projectInfo,
            signing = signing,
            publishing = publishing,
            validation = validation,
            autoDetection = autoDetection,
            metadata = ConfigurationMetadata(sources = sources)
        )
    }
}

/**
 * Builder classes for each configuration section
 */
class CredentialsConfigBuilder {
    var username: String = ""
    var password: String = ""
    var loadFromEnvironment: Boolean = true
    
    fun build() = CredentialsConfig(username, password, loadFromEnvironment)
}

class ProjectInfoConfigBuilder {
    var name: String = ""
    var description: String = ""
    var url: String = ""
    private var scm: ScmConfig = ScmConfig()
    private var license: LicenseConfig = LicenseConfig()
    private var developers: MutableList<DeveloperConfig> = mutableListOf()
    private var issueManagement: IssueManagementConfig = IssueManagementConfig()
    
    fun scm(block: ScmConfigBuilder.() -> Unit) {
        scm = ScmConfigBuilder().apply(block).build()
    }
    
    fun license(block: LicenseConfigBuilder.() -> Unit) {
        license = LicenseConfigBuilder().apply(block).build()
    }
    
    fun developer(block: DeveloperConfigBuilder.() -> Unit) {
        developers.add(DeveloperConfigBuilder().apply(block).build())
    }
    
    fun issueManagement(block: IssueManagementConfigBuilder.() -> Unit) {
        issueManagement = IssueManagementConfigBuilder().apply(block).build()
    }
    
    fun build() = ProjectInfoConfig(name, description, url, scm, license, developers, issueManagement)
}

class ScmConfigBuilder {
    var url: String = ""
    var connection: String = ""
    var developerConnection: String = ""
    
    fun build() = ScmConfig(url, connection, developerConnection)
}

class LicenseConfigBuilder {
    var name: String = ""
    var url: String = ""
    var distribution: String = "repo"
    
    fun build() = LicenseConfig(name, url, distribution)
}

class DeveloperConfigBuilder {
    var id: String = ""
    var name: String = ""
    var email: String = ""
    var organization: String = ""
    var organizationUrl: String = ""
    
    fun build() = DeveloperConfig(id, name, email, organization, organizationUrl)
}

class IssueManagementConfigBuilder {
    var system: String = ""
    var url: String = ""
    
    fun build() = IssueManagementConfig(system, url)
}

class SigningConfigBuilder {
    var keyId: String = ""
    var password: String = ""
    var secretKeyRingFile: String = ""
    var useGpgAgent: Boolean = true
    var autoDetect: Boolean = true
    
    fun build() = SigningConfig(keyId, password, secretKeyRingFile, useGpgAgent, autoDetect)
}

class PublishingConfigBuilder {
    var autoPublish: Boolean = false
    var dryRun: Boolean = false
    var aggregation: Boolean = true
    var publications: List<String> = emptyList()
    var excludeModules: List<String> = emptyList()
    
    fun build() = PublishingConfig(autoPublish, dryRun, aggregation, publications, excludeModules)
}

class ValidationConfigBuilder {
    var enabled: Boolean = true
    var strictMode: Boolean = false
    var skipOnError: Boolean = false
    
    fun build() = ValidationConfig(enabled, strictMode, skipOnError)
}

class AutoDetectionConfigBuilder {
    var projectInfo: Boolean = true
    var credentials: Boolean = true
    var signing: Boolean = true
    var gitInfo: Boolean = true
    
    fun build() = AutoDetectionConfig(projectInfo, credentials, signing, gitInfo)
}

/**
 * Exception thrown when configuration validation fails.
 */
class ConfigurationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)