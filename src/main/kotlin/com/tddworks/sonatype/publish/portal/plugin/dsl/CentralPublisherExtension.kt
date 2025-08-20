package com.tddworks.sonatype.publish.portal.plugin.dsl

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfigBuilder
import org.gradle.api.Project
import org.gradle.api.provider.Property

/**
 * Gradle extension that provides a type-safe DSL for configuring Maven Central publishing.
 * 
 * Usage in build.gradle.kts:
 * ```
 * centralPublisher {
 *     credentials {
 *         username = "your-username"
 *         password = "your-token"  
 *     }
 *     
 *     projectInfo {
 *         name = "my-project"
 *         description = "A great library"
 *         url = "https://github.com/user/repo"
 *     }
 * }
 * ```
 */
open class CentralPublisherExtension(private val project: Project) {
    
    private val configBuilder = CentralPublisherConfigBuilder()
    
    /**
     * Configures credentials for Maven Central publishing
     */
    fun credentials(configure: CredentialsDSL.() -> Unit) {
        val credentialsDSL = CredentialsDSL()
        credentialsDSL.configure()
        
        configBuilder.credentials {
            if (credentialsDSL._username.isNotBlank()) username = credentialsDSL._username
            if (credentialsDSL._password.isNotBlank()) password = credentialsDSL._password
        }
    }
    
    /**
     * Configures project information
     */
    fun projectInfo(configure: ProjectInfoDSL.() -> Unit) {
        val projectInfoDSL = ProjectInfoDSL()
        projectInfoDSL.configure()
        
        configBuilder.projectInfo {
            if (projectInfoDSL._name.isNotBlank()) name = projectInfoDSL._name
            if (projectInfoDSL._description.isNotBlank()) description = projectInfoDSL._description
            if (projectInfoDSL._url.isNotBlank()) url = projectInfoDSL._url
            
            // Apply SCM configuration
            projectInfoDSL._scmConfig?.let { scmDSL ->
                scm {
                    if (scmDSL._url.isNotBlank()) url = scmDSL._url
                    if (scmDSL._connection.isNotBlank()) connection = scmDSL._connection
                    if (scmDSL._developerConnection.isNotBlank()) developerConnection = scmDSL._developerConnection
                }
            }
            
            // Apply License configuration
            projectInfoDSL._licenseConfig?.let { licenseDSL ->
                license {
                    if (licenseDSL._name.isNotBlank()) name = licenseDSL._name
                    if (licenseDSL._url.isNotBlank()) url = licenseDSL._url
                    if (licenseDSL._distribution.isNotBlank()) distribution = licenseDSL._distribution
                }
            }
            
            // Apply Developer configuration
            projectInfoDSL._developerConfig?.let { developerDSL ->
                developer {
                    if (developerDSL._id.isNotBlank()) id = developerDSL._id
                    if (developerDSL._name.isNotBlank()) name = developerDSL._name
                    if (developerDSL._email.isNotBlank()) email = developerDSL._email
                    if (developerDSL._organization.isNotBlank()) organization = developerDSL._organization
                    if (developerDSL._organizationUrl.isNotBlank()) organizationUrl = developerDSL._organizationUrl
                }
            }
        }
    }
    
    /**
     * Configures GPG signing settings
     */
    fun signing(configure: SigningDSL.() -> Unit) {
        val signingDSL = SigningDSL()
        signingDSL.configure()
        
        configBuilder.signing {
            if (signingDSL._keyId.isNotBlank()) keyId = signingDSL._keyId
            if (signingDSL._password.isNotBlank()) password = signingDSL._password
            if (signingDSL._secretKeyRingFile.isNotBlank()) secretKeyRingFile = signingDSL._secretKeyRingFile
        }
    }
    
    /**
     * Configures publishing settings
     */
    fun publishing(configure: PublishingDSL.() -> Unit) {
        val publishingDSL = PublishingDSL()
        publishingDSL.configure()
        
        configBuilder.publishing {
            if (publishingDSL._autoPublish != null) autoPublish = publishingDSL._autoPublish!!
            if (publishingDSL._aggregation != null) aggregation = publishingDSL._aggregation!!
            if (publishingDSL._dryRun != null) dryRun = publishingDSL._dryRun!!
        }
    }
    
    /**
     * Builds the final configuration
     */
    fun build(): CentralPublisherConfig {
        return configBuilder.build()
    }
}

/**
 * DSL for credentials configuration
 */
class CredentialsDSL {
    internal var _username: String = ""
    internal var _password: String = ""
    
    var username: String
        get() = _username
        set(value) { _username = value }
    
    var password: String
        get() = _password
        set(value) { _password = value }
}

/**
 * DSL for project info configuration
 */
class ProjectInfoDSL {
    internal var _name: String = ""
    internal var _description: String = ""
    internal var _url: String = ""
    internal var _scmConfig: ScmDSL? = null
    internal var _licenseConfig: LicenseDSL? = null
    internal var _developerConfig: DeveloperDSL? = null
    
    var name: String
        get() = _name
        set(value) { _name = value }
    
    var description: String
        get() = _description
        set(value) { _description = value }
    
    var url: String
        get() = _url
        set(value) { _url = value }
    
    fun scm(configure: ScmDSL.() -> Unit) {
        val scmDSL = ScmDSL()
        scmDSL.configure()
        _scmConfig = scmDSL
    }
    
    fun license(configure: LicenseDSL.() -> Unit) {
        val licenseDSL = LicenseDSL()
        licenseDSL.configure()
        _licenseConfig = licenseDSL
    }
    
    fun developer(configure: DeveloperDSL.() -> Unit) {
        val developerDSL = DeveloperDSL()
        developerDSL.configure()
        _developerConfig = developerDSL
    }
}

/**
 * DSL for SCM configuration
 */
class ScmDSL {
    internal var _url: String = ""
    internal var _connection: String = ""
    internal var _developerConnection: String = ""
    
    var url: String
        get() = _url
        set(value) { _url = value }
    
    var connection: String
        get() = _connection
        set(value) { _connection = value }
    
    var developerConnection: String
        get() = _developerConnection
        set(value) { _developerConnection = value }
}

/**
 * DSL for license configuration  
 */
class LicenseDSL {
    internal var _name: String = ""
    internal var _url: String = ""
    internal var _distribution: String = ""
    
    var name: String
        get() = _name
        set(value) { _name = value }
    
    var url: String
        get() = _url
        set(value) { _url = value }
    
    var distribution: String
        get() = _distribution
        set(value) { _distribution = value }
}

/**
 * DSL for developer configuration
 */
class DeveloperDSL {
    internal var _id: String = ""
    internal var _name: String = ""
    internal var _email: String = ""
    internal var _organization: String = ""
    internal var _organizationUrl: String = ""
    
    var id: String
        get() = _id
        set(value) { _id = value }
    
    var name: String
        get() = _name
        set(value) { _name = value }
    
    var email: String
        get() = _email
        set(value) { _email = value }
    
    var organization: String
        get() = _organization
        set(value) { _organization = value }
    
    var organizationUrl: String
        get() = _organizationUrl
        set(value) { _organizationUrl = value }
}

/**
 * DSL for signing configuration
 */
class SigningDSL {
    internal var _keyId: String = ""
    internal var _password: String = ""
    internal var _secretKeyRingFile: String = ""
    
    var keyId: String
        get() = _keyId
        set(value) { _keyId = value }
    
    var password: String
        get() = _password
        set(value) { _password = value }
    
    var secretKeyRingFile: String
        get() = _secretKeyRingFile
        set(value) { _secretKeyRingFile = value }
}

/**
 * DSL for publishing configuration
 */
class PublishingDSL {
    internal var _autoPublish: Boolean? = null
    internal var _aggregation: Boolean? = null
    internal var _dryRun: Boolean? = null
    
    var autoPublish: Boolean
        get() = _autoPublish ?: false
        set(value) { _autoPublish = value }
    
    var aggregation: Boolean
        get() = _aggregation ?: true
        set(value) { _aggregation = value }
    
    var dryRun: Boolean
        get() = _dryRun ?: false
        set(value) { _dryRun = value }
}