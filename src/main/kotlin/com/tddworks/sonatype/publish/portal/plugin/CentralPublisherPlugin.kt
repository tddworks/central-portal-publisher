package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.api.Authentication
import com.tddworks.sonatype.publish.portal.api.DeploymentBundle
import com.tddworks.sonatype.publish.portal.api.PublicationType
import com.tddworks.sonatype.publish.portal.api.SonatypePortalPublisher
import com.tddworks.sonatype.publish.portal.plugin.dsl.CentralPublisherExtension
import com.tddworks.sonatype.publish.portal.plugin.publication.PublicationProviderRegistry
import com.tddworks.sonatype.publish.portal.plugin.validation.ValidationEngine
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Modern Central Portal Publisher plugin with simplified DSL and auto-detection.
 * 
 * Features:
 * - Type-safe Kotlin DSL (`centralPublisher { }`)
 * - Automatic project information detection
 * - Multi-source configuration (DSL, properties, environment, auto-detection)
 * - Comprehensive validation with actionable error messages
 * - Zero-to-publish in <5 minutes goal
 * 
 * Usage:
 * ```kotlin
 * plugins {
 *     id("com.tddworks.central-publisher")
 * }
 * 
 * centralPublisher {
 *     credentials {
 *         username = "your-username"
 *         password = "your-token" 
 *     }
 *     
 *     projectInfo {
 *         name = "my-library"
 *         description = "An amazing library"
 *         // Many values auto-detected from git/project structure
 *     }
 * }
 * ```
 */
class CentralPublisherPlugin : Plugin<Project> {
    
    companion object {
        const val EXTENSION_NAME = "centralPublisher"
        const val PLUGIN_GROUP = "Central Publishing"
        
        // Task names - simple and memorable
        const val TASK_PUBLISH_TO_CENTRAL = "publishToCentral"
        const val TASK_BUNDLE_ARTIFACTS = "bundleArtifacts" 
        const val TASK_VALIDATE_PUBLISHING = "validatePublishing"
        const val TASK_SETUP_PUBLISHING = "setupPublishing"
    }
    
    override fun apply(project: Project) {
        with(project) {
            logger.quiet("Applying Central Publisher plugin to project: $path")
            
            // Register the type-safe DSL extension
            val extension = extensions.create(EXTENSION_NAME, CentralPublisherExtension::class.java, project)
            
            // Configure after project evaluation
            afterEvaluate {
                configurePlugin(extension)
            }
        }
    }
    
    private fun Project.configurePlugin(extension: CentralPublisherExtension) {
        // Get the final configuration with all sources merged
        val config = extension.build()
        
        // Validate configuration and show actionable errors
        val validationEngine = ValidationEngine()
        val validationResult = validationEngine.validate(config)
        
        if (!validationResult.isValid) {
            logger.error("Configuration validation failed:")
            logger.error(validationResult.formatReport())
            
            // Don't fail the build during configuration, but warn user
            logger.warn("Publishing tasks may fail due to configuration errors above")
        } else if (validationResult.warningCount > 0) {
            logger.warn("Configuration warnings:")
            logger.warn(validationResult.formatReport())
        } else {
            logger.quiet("✅ Central Publisher configuration validated successfully")
        }
        
        // Auto-configure publications
        configurePublications(config)
        
        // Create publishing tasks
        createPublishingTasks(config)
    }
    
    private fun Project.configurePublications(config: com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig) {
        // Use the publication provider registry to auto-configure publications
        val publicationRegistry = PublicationProviderRegistry()
        publicationRegistry.configurePublications(this, config)
        
        logger.quiet("🔧 Publications auto-configured based on project type")
    }
    
    private fun Project.createPublishingTasks(config: com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig) {
        // Create the main publishing task
        tasks.register(TASK_PUBLISH_TO_CENTRAL) {
            group = PLUGIN_GROUP
            description = "Publishes all artifacts to Maven Central"
            
            // Make sure bundle is created first
            dependsOn(TASK_BUNDLE_ARTIFACTS)
            
            doLast {
                logger.quiet("🚀 Publishing to Maven Central...")
                logger.quiet("Configuration: ${config.credentials.username}@${config.projectInfo.name}")
                
                if (config.publishing.dryRun) {
                    logger.quiet("📦 Would publish:")
                    logger.quiet("  - Project: ${config.projectInfo.name}")
                    logger.quiet("  - Description: ${config.projectInfo.description}")
                    logger.quiet("  - URL: ${config.projectInfo.url}")
                    logger.quiet("  - Auto-publish: ${config.publishing.autoPublish}")
                    logger.lifecycle("🧪 DRY RUN MODE - No actual publishing performed")
                } else {
                    // Actual publishing logic
                    val result = publishToSonatypePortal(project, config)
                    logger.lifecycle("✅ Publishing completed: $result")
                }
            }
        }
        
        // Create validation task
        tasks.register(TASK_VALIDATE_PUBLISHING) {
            group = PLUGIN_GROUP
            description = "Validates publishing configuration without publishing"
            
            doLast {
                val validationEngine = ValidationEngine()
                val result = validationEngine.validate(config)
                
                println(result.formatReport())
                
                if (result.isValid) {
                    logger.lifecycle("✅ Configuration is valid and ready for publishing")
                } else {
                    throw org.gradle.api.GradleException("❌ Configuration validation failed. See errors above.")
                }
            }
        }
        
        // Create bundle artifacts task  
        tasks.register(TASK_BUNDLE_ARTIFACTS) {
            group = PLUGIN_GROUP
            description = "Creates deployment bundle for Maven Central"
            
            // Make sure publications are built first
            dependsOn("publishToMavenLocal")
            
            doLast {
                logger.quiet("📦 Creating deployment bundle...")
                logger.quiet("  - Signing enabled: ${config.signing.keyId.isNotBlank()}")
                
                val bundleFile = createDeploymentBundle(project, config)
                logger.lifecycle("✅ Bundle created: ${bundleFile.absolutePath}")
            }
        }
        
        // Create setup wizard task
        tasks.register(TASK_SETUP_PUBLISHING) {
            group = PLUGIN_GROUP
            description = "Interactive setup wizard for Maven Central publishing"
            
            doLast {
                logger.lifecycle("🧙 Starting Central Publisher Setup Wizard...")
                
                try {
                    val wizard = com.tddworks.sonatype.publish.portal.plugin.wizard.SetupWizard(project)
                    val result = wizard.runComplete()
                    
                    if (result.isComplete) {
                        logger.lifecycle("✅ Setup completed successfully!")
                        logger.lifecycle("")
                        logger.lifecycle(result.summary)
                        logger.lifecycle("")
                        logger.lifecycle("Generated files:")
                        result.filesGenerated.forEach { file ->
                            logger.lifecycle("  - $file")
                        }
                    } else {
                        logger.warn("⚠️ Setup was not completed successfully")
                    }
                } catch (e: Exception) {
                    logger.error("❌ Setup wizard failed: ${e.message}")
                    logger.lifecycle("For manual setup, please configure using the centralPublisher DSL block")
                    logger.lifecycle("See documentation: https://github.com/tddworks/central-portal-publisher")
                }
            }
        }
    }
    
    /**
     * Creates a deployment bundle ZIP file containing all published artifacts with proper Maven repository layout.
     */
    private fun createDeploymentBundle(project: Project, config: com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig): File {
        val bundleDir = project.layout.buildDirectory.dir("central-portal").get().asFile
        bundleDir.mkdirs()
        
        val bundleFile = File(bundleDir, "${project.name}-${project.version}-bundle.zip")
        
        // Get local Maven repository path where artifacts were published
        val localRepo = File(System.getProperty("user.home"), ".m2/repository")
        val groupPath = project.group.toString().replace('.', '/')
        val artifactDir = File(localRepo, "$groupPath/${project.name}/${project.version}")
        
        if (!artifactDir.exists()) {
            throw IllegalStateException("Published artifacts not found at: ${artifactDir.absolutePath}. Run 'publishToMavenLocal' first.")
        }
        
        // Validate namespace (Maven Central requirement)
        val groupId = project.group.toString()
        if (groupId.startsWith("com.example") || groupId.startsWith("org.example")) {
            project.logger.warn("⚠️ Group ID '$groupId' uses example namespace which is not allowed on Maven Central")
            project.logger.warn("   Please use a valid domain you own (e.g., com.yourcompany.projectname)")
        }
        
        // Create ZIP bundle with proper Maven repository layout
        ZipOutputStream(bundleFile.outputStream()).use { zip ->
            artifactDir.listFiles()?.forEach { file ->
                if (file.isFile && !file.name.endsWith(".asc") && !file.name.endsWith(".md5") && !file.name.endsWith(".sha1")) {
                    val relativePath = "$groupPath/${project.name}/${project.version}/${file.name}"
                    
                    // Add the main file
                    addFileToZip(zip, file, relativePath, project)
                    
                    // Generate and add checksums
                    addChecksumsToZip(zip, file, relativePath, project)
                    
                    // Add signature if signing is configured and signature exists
                    val signatureFile = File(file.parent, "${file.name}.asc")
                    if (config.signing.keyId.isNotBlank() && signatureFile.exists()) {
                        addFileToZip(zip, signatureFile, "$relativePath.asc", project)
                    } else if (config.signing.keyId.isNotBlank()) {
                        project.logger.warn("⚠️ Signature file not found for ${file.name}. Run gradle signing tasks first.")
                    }
                }
            }
        }
        
        return bundleFile
    }
    
    /**
     * Adds a file to the ZIP with proper entry.
     */
    private fun addFileToZip(zip: ZipOutputStream, file: File, entryPath: String, project: Project) {
        zip.putNextEntry(ZipEntry(entryPath))
        file.inputStream().use { input ->
            input.copyTo(zip)
        }
        zip.closeEntry()
        project.logger.quiet("  ✓ Added $entryPath")
    }
    
    /**
     * Generates and adds MD5 and SHA1 checksums to the ZIP.
     */
    private fun addChecksumsToZip(zip: ZipOutputStream, file: File, basePath: String, project: Project) {
        val content = file.readBytes()
        
        // Generate MD5
        val md5 = MessageDigest.getInstance("MD5").digest(content).joinToString("") { 
            "%02x".format(it) 
        }
        zip.putNextEntry(ZipEntry("$basePath.md5"))
        zip.write(md5.toByteArray())
        zip.closeEntry()
        project.logger.quiet("  ✓ Added $basePath.md5")
        
        // Generate SHA1  
        val sha1 = MessageDigest.getInstance("SHA-1").digest(content).joinToString("") { 
            "%02x".format(it) 
        }
        zip.putNextEntry(ZipEntry("$basePath.sha1"))
        zip.write(sha1.toByteArray())
        zip.closeEntry()
        project.logger.quiet("  ✓ Added $basePath.sha1")
    }
    
    /**
     * Publishes the deployment bundle to Sonatype Central Portal.
     */
    private fun publishToSonatypePortal(project: Project, config: com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig): String {
        // Create deployment bundle
        val bundleFile = createDeploymentBundle(project, config)
        
        // Create authentication
        val auth = Authentication(
            username = config.credentials.username.ifBlank { null },
            password = config.credentials.password.ifBlank { null }
        )
        
        if (auth.username.isNullOrBlank() || auth.password.isNullOrBlank()) {
            throw IllegalStateException("Username and password are required for publishing. Configure them in gradle.properties or environment variables.")
        }
        
        // Determine publication type based on auto-publish setting
        val publicationType = if (config.publishing.autoPublish) {
            PublicationType.AUTOMATIC
        } else {
            PublicationType.USER_MANAGED
        }
        
        // Create deployment bundle
        val deploymentBundle = DeploymentBundle(
            file = bundleFile,
            publicationType = publicationType
        )
        
        // Publish to Sonatype
        val publisher = SonatypePortalPublisher.default()
        return publisher.deploy(auth, deploymentBundle)
    }
}