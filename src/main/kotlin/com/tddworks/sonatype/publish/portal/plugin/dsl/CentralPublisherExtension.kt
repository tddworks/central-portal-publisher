package com.tddworks.sonatype.publish.portal.plugin.dsl

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfigBuilder
import com.tddworks.sonatype.publish.portal.plugin.config.ConfigurationSourceManager
import org.gradle.api.Project
import org.gradle.api.provider.Property

/**
 * DSL marker annotation to prevent scope pollution and provide better IDE support.
 * This ensures that DSL blocks cannot access methods from outer scopes accidentally.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class CentralPublisherDsl

/**
 * Gradle extension that provides a type-safe DSL for configuring Maven Central publishing.
 * 
 * This extension enables zero-configuration publishing to Maven Central with automatic 
 * detection of project information from git repositories and build files.
 * 
 * ## Usage in build.gradle.kts:
 * ```kotlin
 * plugins {
 *     id("com.tddworks.central-publisher") version "0.0.5"
 * }
 * 
 * centralPublisher {
 *     credentials {
 *         username = "your-username"  // Required: Sonatype username
 *         password = "your-token"     // Required: Sonatype token
 *     }
 *     
 *     projectInfo {
 *         name = "my-project"           // Auto-detected from project
 *         description = "A great lib"  // Auto-detected from README
 *         url = "https://github.com/user/repo"  // Auto-detected from git
 *         
 *         scm {
 *             url = "https://github.com/user/repo"
 *             connection = "scm:git:https://github.com/user/repo.git"
 *         }
 *         
 *         license {
 *             name = "Apache License 2.0"
 *             url = "https://www.apache.org/licenses/LICENSE-2.0"
 *         }
 *         
 *         developer {
 *             name = "Developer Name"
 *             email = "dev@example.com"
 *         }
 *     }
 *     
 *     signing {
 *         keyId = "12345678"                    // GPG key ID
 *         password = "signing-password"        // GPG key password
 *         secretKeyRingFile = "~/.gnupg/secring.gpg"
 *     }
 *     
 *     publishing {
 *         autoPublish = true    // Auto-publish after upload (default: false)
 *         dryRun = false       // Test mode without actual publishing
 *     }
 * }
 * ```
 * 
 * ## Key Features:
 * - **Auto-detection**: Automatically detects project info from git and build files
 * - **Type-safe**: Compile-time validation with full IDE support
 * - **Zero-configuration**: Minimal setup required - just credentials
 * - **Multi-source**: Supports DSL, properties files, and environment variables
 * - **Validation**: Comprehensive validation with actionable error messages
 * 
 * ## Configuration Sources (in order of precedence):
 * 1. DSL configuration (this block)
 * 2. gradle.properties file
 * 3. Environment variables
 * 4. Auto-detected values (git, build files, README)
 * 5. Smart defaults
 * 
 * @since 0.0.5
 * @see com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
 */
@CentralPublisherDsl
open class CentralPublisherExtension(private val project: Project) {
    
    private val configBuilder = CentralPublisherConfigBuilder()
    private var hasExplicitConfigurationFlag = false
    
    /**
     * Configures credentials for Maven Central publishing.
     * 
     * @param configure Configuration block for setting username and password
     * 
     * Example:
     * ```kotlin
     * credentials {
     *     username = "your-sonatype-username"
     *     password = "your-sonatype-token"
     * }
     * ```
     * 
     * @see CredentialsDSL
     */
    fun credentials(configure: CredentialsDSL.() -> Unit) {
        hasExplicitConfigurationFlag = true
        val credentialsDSL = CredentialsDSL()
        credentialsDSL.configure()
        
        configBuilder.credentials {
            if (credentialsDSL._username.isNotBlank()) username = credentialsDSL._username
            if (credentialsDSL._password.isNotBlank()) password = credentialsDSL._password
        }
    }
    
    /**
     * Configures project metadata required for Maven Central publishing.
     * 
     * This block supports auto-detection from git repositories and build files.
     * Explicitly configured values take precedence over auto-detected ones.
     * 
     * @param configure Configuration block for setting project metadata
     * 
     * Example:
     * ```kotlin
     * projectInfo {
     *     name = "my-awesome-library"
     *     description = "An awesome library for doing awesome things"
     *     url = "https://github.com/user/awesome-library"
     *     
     *     scm {
     *         url = "https://github.com/user/awesome-library"
     *         connection = "scm:git:https://github.com/user/awesome-library.git"
     *     }
     *     
     *     license {
     *         name = "Apache License 2.0"
     *         url = "https://www.apache.org/licenses/LICENSE-2.0"
     *     }
     *     
     *     developer {
     *         name = "Your Name"
     *         email = "your.email@example.com"
     *     }
     * }
     * ```
     * 
     * @see ProjectInfoDSL
     */
    fun projectInfo(configure: ProjectInfoDSL.() -> Unit) {
        hasExplicitConfigurationFlag = true
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
     * Configures GPG signing settings for artifact signing.
     * 
     * Maven Central requires all artifacts to be signed with GPG.
     * 
     * @param configure Configuration block for setting GPG signing parameters
     * 
     * Example:
     * ```kotlin
     * signing {
     *     keyId = "12345678"  // Last 8 characters of GPG key ID
     *     password = "your-gpg-passphrase"
     *     secretKeyRingFile = "~/.gnupg/secring.gpg"
     * }
     * ```
     * 
     * @see SigningDSL
     */
    fun signing(configure: SigningDSL.() -> Unit) {
        hasExplicitConfigurationFlag = true
        val signingDSL = SigningDSL()
        signingDSL.configure()
        
        configBuilder.signing {
            if (signingDSL._keyId.isNotBlank()) keyId = signingDSL._keyId
            if (signingDSL._password.isNotBlank()) password = signingDSL._password
            if (signingDSL._secretKeyRingFile.isNotBlank()) secretKeyRingFile = signingDSL._secretKeyRingFile
        }
    }
    
    /**
     * Configures publishing behavior and options.
     * 
     * @param configure Configuration block for setting publishing options
     * 
     * Example:
     * ```kotlin
     * publishing {
     *     autoPublish = true   // Automatically publish after upload
     *     aggregation = true   // Use aggregated publishing (recommended)
     *     dryRun = false      // Set to true for testing without actual publishing
     * }
     * ```
     * 
     * @see PublishingDSL
     */
    fun publishing(configure: PublishingDSL.() -> Unit) {
        hasExplicitConfigurationFlag = true
        val publishingDSL = PublishingDSL()
        publishingDSL.configure()
        
        configBuilder.publishing {
            if (publishingDSL._autoPublish != null) autoPublish = publishingDSL._autoPublish!!
            if (publishingDSL._aggregation != null) aggregation = publishingDSL._aggregation!!
            if (publishingDSL._dryRun != null) dryRun = publishingDSL._dryRun!!
        }
    }
    
    /**
     * Returns whether the user has explicitly configured any part of the plugin.
     * Used to determine if validation errors should be shown during configuration phase.
     */
    fun hasExplicitConfiguration(): Boolean = hasExplicitConfigurationFlag
    
    /**
     * Builds the final configuration with auto-detection integration
     */
    fun build(): CentralPublisherConfig {
        // First, build the DSL configuration
        val dslConfig = configBuilder.build()
        
        // Then use ConfigurationSourceManager to merge with auto-detection and other sources
        val sourceManager = ConfigurationSourceManager(project)
        
        return sourceManager.loadConfigurationWithPrecedence(
            dslConfig = dslConfig,
            enableAutoDetection = true
        )
    }
}

/**
 * DSL for configuring Maven Central credentials.
 * 
 * Provides type-safe configuration for Sonatype username and password/token.
 * 
 * @since 0.0.5
 */
@CentralPublisherDsl
class CredentialsDSL {
    internal var _username: String = ""
    internal var _password: String = ""
    
    /**
     * Sonatype username for Maven Central publishing.
     * 
     * This should be your JIRA username from issues.sonatype.org.
     */
    var username: String
        get() = _username
        set(value) { _username = value }
    
    /**
     * Sonatype password or token for Maven Central publishing.
     * 
     * It's recommended to use a token instead of your actual password.
     * You can generate tokens in your Sonatype account settings.
     */
    var password: String
        get() = _password
        set(value) { _password = value }
}

/**
 * DSL for configuring project metadata.
 * 
 * Supports auto-detection from git repositories, build files, and README files.
 * Explicit configuration takes precedence over auto-detected values.
 * 
 * @since 0.0.5
 */
@CentralPublisherDsl
class ProjectInfoDSL {
    internal var _name: String = ""
    internal var _description: String = ""
    internal var _url: String = ""
    internal var _scmConfig: ScmDSL? = null
    internal var _licenseConfig: LicenseDSL? = null
    internal var _developerConfig: DeveloperDSL? = null
    
    /**
     * Project name for Maven Central publishing.
     * 
     * If not specified, will be auto-detected from:
     * - Project name in build files
     * - Git repository name
     * - Directory name
     */
    var name: String
        get() = _name
        set(value) { _name = value }
    
    /**
     * Project description for Maven Central publishing.
     * 
     * If not specified, will be auto-detected from:
     * - README files
     * - Project description in build files
     */
    var description: String
        get() = _description
        set(value) { _description = value }
    
    /**
     * Project URL for Maven Central publishing.
     * 
     * If not specified, will be auto-detected from:
     * - Git remote URL
     * - GitHub/GitLab repository URL
     */
    var url: String
        get() = _url
        set(value) { _url = value }
    
    /**
     * Configures Source Control Management (SCM) information.
     * 
     * @param configure Configuration block for SCM settings
     * @see ScmDSL
     */
    fun scm(configure: ScmDSL.() -> Unit) {
        val scmDSL = ScmDSL()
        scmDSL.configure()
        _scmConfig = scmDSL
    }
    
    /**
     * Configures license information.
     * 
     * @param configure Configuration block for license settings
     * @see LicenseDSL
     */
    fun license(configure: LicenseDSL.() -> Unit) {
        val licenseDSL = LicenseDSL()
        licenseDSL.configure()
        _licenseConfig = licenseDSL
    }
    
    /**
     * Configures developer information.
     * 
     * @param configure Configuration block for developer settings
     * @see DeveloperDSL
     */
    fun developer(configure: DeveloperDSL.() -> Unit) {
        val developerDSL = DeveloperDSL()
        developerDSL.configure()
        _developerConfig = developerDSL
    }
}

/**
 * DSL for configuring Source Control Management (SCM) information.
 * 
 * Provides type-safe configuration for repository URLs and connections.
 * Supports auto-detection from git remotes when not explicitly configured.
 * 
 * @since 0.0.5
 */
@CentralPublisherDsl
class ScmDSL {
    internal var _url: String = ""
    internal var _connection: String = ""
    internal var _developerConnection: String = ""
    
    /**
     * The URL of the project's browsable repository.
     * 
     * Example: "https://github.com/user/project"
     */
    var url: String
        get() = _url
        set(value) { _url = value }
    
    /**
     * The source control connection URL for anonymous access.
     * 
     * Example: "scm:git:https://github.com/user/project.git"
     */
    var connection: String
        get() = _connection
        set(value) { _connection = value }
    
    /**
     * The source control connection URL for developer access.
     * 
     * Example: "scm:git:ssh://git@github.com/user/project.git"
     */
    var developerConnection: String
        get() = _developerConnection
        set(value) { _developerConnection = value }
}

/**
 * DSL for configuring project license information.
 * 
 * Provides type-safe configuration for license name, URL, and distribution method.
 * 
 * @since 0.0.5
 */
@CentralPublisherDsl
class LicenseDSL {
    internal var _name: String = ""
    internal var _url: String = ""
    internal var _distribution: String = ""
    
    /**
     * The name of the license.
     * 
     * Examples: "Apache License 2.0", "MIT License", "GPL-3.0"
     */
    var name: String
        get() = _name
        set(value) { _name = value }
    
    /**
     * The URL where the license text can be found.
     * 
     * Example: "https://www.apache.org/licenses/LICENSE-2.0.txt"
     */
    var url: String
        get() = _url
        set(value) { _url = value }
    
    /**
     * The distribution method for the license.
     * 
     * Typically "repo" for repository distribution or "manual" for manual distribution.
     */
    var distribution: String
        get() = _distribution
        set(value) { _distribution = value }
}

/**
 * DSL for configuring developer information.
 * 
 * Provides type-safe configuration for developer/maintainer details.
 * 
 * @since 0.0.5
 */
@CentralPublisherDsl
class DeveloperDSL {
    internal var _id: String = ""
    internal var _name: String = ""
    internal var _email: String = ""
    internal var _organization: String = ""
    internal var _organizationUrl: String = ""
    
    /**
     * A unique identifier for the developer.
     * 
     * This is often the developer's username or a unique ID.
     */
    var id: String
        get() = _id
        set(value) { _id = value }
    
    /**
     * The full name of the developer.
     */
    var name: String
        get() = _name
        set(value) { _name = value }
    
    /**
     * The email address of the developer.
     */
    var email: String
        get() = _email
        set(value) { _email = value }
    
    /**
     * The organization or company the developer belongs to.
     */
    var organization: String
        get() = _organization
        set(value) { _organization = value }
    
    /**
     * The URL of the organization's website.
     */
    var organizationUrl: String
        get() = _organizationUrl
        set(value) { _organizationUrl = value }
}

/**
 * DSL for configuring GPG signing settings.
 * 
 * Provides type-safe configuration for GPG key information required for artifact signing.
 * Maven Central requires all artifacts to be signed.
 * 
 * @since 0.0.5
 */
@CentralPublisherDsl
class SigningDSL {
    internal var _keyId: String = ""
    internal var _password: String = ""
    internal var _secretKeyRingFile: String = ""
    
    /**
     * The GPG key ID to use for signing.
     * 
     * This should be the last 8 characters of your GPG key ID.
     * Example: "12345678"
     */
    var keyId: String
        get() = _keyId
        set(value) { _keyId = value }
    
    /**
     * The passphrase for the GPG key.
     * 
     * This is the password you set when creating your GPG key.
     */
    var password: String
        get() = _password
        set(value) { _password = value }
    
    /**
     * The path to the GPG secret key ring file.
     * 
     * Example: "~/.gnupg/secring.gpg"
     * Note: Modern GPG versions store keys differently, so this may not be needed.
     */
    var secretKeyRingFile: String
        get() = _secretKeyRingFile
        set(value) { _secretKeyRingFile = value }
}

/**
 * DSL for configuring publishing behavior and options.
 * 
 * Provides type-safe configuration for publishing settings like auto-publish,
 * aggregation, and dry-run mode.
 * 
 * @since 0.0.5
 */
@CentralPublisherDsl
class PublishingDSL {
    internal var _autoPublish: Boolean? = null
    internal var _aggregation: Boolean? = null
    internal var _dryRun: Boolean? = null
    
    /**
     * Whether to automatically publish artifacts after upload.
     * 
     * When true, artifacts are automatically promoted to public repositories.
     * When false, you need to manually promote them through the Sonatype UI.
     * Default: false (safer for production)
     */
    var autoPublish: Boolean
        get() = _autoPublish ?: false
        set(value) { _autoPublish = value }
    
    /**
     * Whether to use aggregated publishing.
     * 
     * When true, all artifacts are uploaded to a single staging repository.
     * When false, each artifact gets its own staging repository.
     * Default: true (recommended)
     */
    var aggregation: Boolean
        get() = _aggregation ?: true
        set(value) { _aggregation = value }
    
    /**
     * Whether to run in dry-run mode.
     * 
     * When true, all operations are simulated without actual publishing.
     * Useful for testing configuration and validation.
     * Default: false
     */
    var dryRun: Boolean
        get() = _dryRun ?: false
        set(value) { _dryRun = value }
}