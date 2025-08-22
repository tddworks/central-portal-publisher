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
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.configure
import java.io.File
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
        
        // Only validate configuration if user explicitly configured the plugin
        // If no explicit configuration, defer validation to setup wizard
        if (extension.hasExplicitConfiguration()) {
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
        } else {
            // No explicit configuration - let setup wizard handle it
            logger.quiet("🔧 No explicit configuration detected - use './gradlew setupPublishing' to configure interactively")
        }
        
        // Auto-configure publications for root project and all subprojects
        configurePublications(config)
        
        // Configure all subprojects that have maven-publish
        subprojects {
            afterEvaluate {
                if (plugins.hasPlugin("maven-publish")) {
                    // Configure publications for this subproject
                    val publicationRegistry = PublicationProviderRegistry()
                    publicationRegistry.configurePublications(this, config)
                    
                    // Configure LocalRepo repository for this subproject
                    extensions.configure<PublishingExtension> {
                        repositories {
                            maven {
                                name = "LocalRepo"
                                setUrl(rootProject.layout.buildDirectory.dir("maven-repo").get().asFile)
                            }
                        }
                    }
                    
                    // Create publishToLocalRepo task for this subproject
                    tasks.register("publishToLocalRepo") {
                        group = "Central Publishing"
                        description = "Publishes to local repository (generates checksums and signatures)"
                        
                        val publishTasks = tasks.matching { task ->
                            task.name.matches(Regex("publish.+Publication[s]?ToLocalRepoRepository"))
                        }
                        
                        // Also ensure signing tasks run if signing is configured
                        val signingTasks = tasks.matching { task ->
                            task.name.startsWith("sign") && task.name.endsWith("Publication")
                        }
                        
                        dependsOn(publishTasks + signingTasks)
                    }
                }
            }
        }
        
        // Create publishing tasks
        createPublishingTasks(config)
    }
    
    private fun Project.configurePublications(config: com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig) {
        // Use the publication provider registry to auto-configure publications
        val publicationRegistry = PublicationProviderRegistry()
        publicationRegistry.configurePublications(this, config)
        
        // Create a local repository for deployment bundle (generates checksums automatically)
        // Only configure if maven-publish plugin is applied to root project
        if (plugins.hasPlugin("maven-publish")) {
            extensions.configure<PublishingExtension> {
                repositories {
                    maven {
                        name = "LocalRepo"
                        setUrl(layout.buildDirectory.dir("maven-repo").get().asFile)
                    }
                }
            }
        }
        
        
        // Create publish task that generates checksums and signatures (only if maven-publish is available)
        if (plugins.hasPlugin("maven-publish")) {
            tasks.register("publishToLocalRepo") {
                group = PLUGIN_GROUP
                description = "Publishes to local repository (generates checksums and signatures)"
                
                val publishTasks = tasks.matching { task ->
                    task.name.matches(Regex("publish.+Publication[s]?ToLocalRepoRepository"))
                }
                
                // Also ensure signing tasks run if signing is configured
                val signingTasks = tasks.matching { task ->
                    task.name.startsWith("sign") && task.name.endsWith("Publication")
                }
                
                dependsOn(publishTasks + signingTasks)
                
                // For multi-module support, depend on all subproject publish tasks to LocalRepo
                allprojects.forEach { subproject ->
                    if (subproject != project && subproject.plugins.hasPlugin("maven-publish")) {
                        val subprojectPublishTasks = subproject.tasks.matching { task ->
                            task.name.matches(Regex("publish.+Publication[s]?ToLocalRepoRepository"))
                        }
                        dependsOn(subprojectPublishTasks)
                    }
                }
            }
        }
        
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
            
            // Use local repository that generates checksums and signatures automatically
            // Only depend on publishToLocalRepo if it exists (i.e., maven-publish plugin is applied)
            if (project.plugins.hasPlugin("maven-publish")) {
                dependsOn("publishToLocalRepo")
            }
            
            doLast {
                logger.quiet("📦 Creating deployment bundle...")
                
                // Check for signing configuration in multiple places
                val hasSigningKey = project.findProperty("SIGNING_KEY") != null || 
                                  System.getenv("SIGNING_KEY") != null ||
                                  config.signing.keyId.isNotBlank()
                logger.quiet("  - Signing enabled: $hasSigningKey")
                
                val bundleFile = createDeploymentBundle(project)
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
                    val wizard = com.tddworks.sonatype.publish.portal.plugin.wizard.RefactoredSetupWizard(project)
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
     * Uses the vanniktech plugin approach: publish to local repo (generates checksums) then ZIP it up.
     */
    private fun createDeploymentBundle(project: Project): File {
        val bundleDir = project.layout.buildDirectory.dir("central-portal").get().asFile
        bundleDir.mkdirs()

        val bundleFile = File(bundleDir, "${project.name}-${project.version}-bundle.zip")

        // Get our local repository where artifacts were published with checksums and signatures
        val localRepo = project.layout.buildDirectory.dir("maven-repo").get().asFile
        val groupPath = project.group.toString().replace('.', '/')
        val groupDir = File(localRepo, groupPath)

        if (!groupDir.exists()) {
            throw IllegalStateException("Published artifacts not found at: ${groupDir.absolutePath}. Run 'publishToLocalRepo' first.")
        }

        // Validate namespace (Maven Central requirement)
        val groupId = project.group.toString()
        if (groupId.startsWith("com.example") || groupId.startsWith("org.example")) {
            project.logger.warn("⚠️ Group ID '$groupId' uses example namespace which is not allowed on Maven Central")
            project.logger.warn("   Please use a valid domain you own (e.g., com.yourcompany.projectname)")
        }

        // Create ZIP bundle with all files from the repository (includes checksums and signatures)
        // This follows the vanniktech pattern: rely on Gradle's publishing to generate everything
        // For KMP projects, this includes all platform-specific publications
        ZipOutputStream(bundleFile.outputStream()).use { zip ->
            groupDir.walkTopDown()
                .filter { it.isFile && !it.name.startsWith("maven-metadata") }
                .forEach { file ->
                    val relativePath = file.relativeTo(localRepo).path.replace('\\', '/')
                    addFileToZip(zip, file, relativePath, project)
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
     * Publishes the deployment bundle to Sonatype Central Portal.
     */
    private fun publishToSonatypePortal(project: Project, config: com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig): String {
        // Create deployment bundle
        val bundleFile = createDeploymentBundle(project)

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