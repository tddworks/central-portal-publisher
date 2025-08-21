package com.tddworks.sonatype.publish.portal.plugin.publication

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.get
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import kotlin.jvm.java

/**
 * Interface for configuring Maven publications based on project configuration.
 * 
 * Publication providers are responsible for:
 * - Detecting appropriate project types
 * - Auto-applying necessary plugins
 * - Creating and configuring Maven publications
 * - Setting up sources and javadoc JARs
 * - Configuring POM metadata from configuration
 * - Setting up signing when configured
 */
interface PublicationProvider {
    /**
     * Configure publications for the given project using the provided configuration.
     * 
     * @param project The Gradle project to configure
     * @param config The Central Publisher configuration containing metadata
     */
    fun configurePublications(project: Project, config: CentralPublisherConfig)
}

/**
 * Publication provider for JVM projects (Java and Kotlin/JVM).
 * 
 * Supports:
 * - Java projects with `java` plugin
 * - Kotlin/JVM projects with `org.jetbrains.kotlin.jvm` plugin
 * - Automatic sources and javadoc JAR creation
 * - POM population from configuration
 * - Signing configuration
 */
class JvmPublicationProvider : PublicationProvider {
    
    override fun configurePublications(project: Project, config: CentralPublisherConfig) {
        when {
            project.plugins.hasPlugin("org.jetbrains.kotlin.jvm") -> {
                configureKotlinJvmProject(project, config)
            }
            project.plugins.hasPlugin("java") -> {
                configureJavaProject(project, config)
            }
            else -> {
                // No JVM plugins found, skip configuration
                return
            }
        }
        
        // Configure signing if signing information is provided (check multiple sources)
        val hasSigningConfig = config.signing.keyId.isNotBlank() || 
                               project.get("SIGNING_KEY") != "SIGNING_KEY not found" ||
                               project.findProperty("signing.keyId") != null
        
        if (hasSigningConfig) {
            configureSigning(project, config)
        }
    }
    
    private fun configureJavaProject(project: Project, config: CentralPublisherConfig) {
        // Auto-apply maven-publish plugin if not already applied
        if (!project.plugins.hasPlugin("maven-publish")) {
            project.plugins.apply("maven-publish")
        }
        
        // Configure sources jar
        project.extensions.configure<JavaPluginExtension> {
            withSourcesJar()
        }
        
        // Create Maven publication if it doesn't exist
        val publishing = project.extensions.getByType<PublishingExtension>()
        if (publishing.publications.findByName("maven") == null) {
            publishing.publications.register<MavenPublication>("maven") {
                from(project.components["java"])
                configurePom(project, config)
            }
        } else {
            // Configure existing maven publication
            publishing.publications.withType<MavenPublication>().named("maven").configure {
                configurePom(project, config)
            }
        }
    }
    
    private fun configureKotlinJvmProject(project: Project, config: CentralPublisherConfig) {
        // Auto-apply maven-publish plugin if not already applied
        if (!project.plugins.hasPlugin("maven-publish")) {
            project.plugins.apply("maven-publish")
        }
        
        // Configure sources jar - Kotlin JVM projects need explicit configuration
        project.extensions.configure<JavaPluginExtension> {
            withSourcesJar()
        }
        
        // Create javadoc jar (empty for Kotlin projects - common practice)
        val javadocJar = project.tasks.register<Jar>("javadocJar") {
            archiveClassifier.set("javadoc")
            duplicatesStrategy = DuplicatesStrategy.WARN
            // Contents are deliberately left empty - standard practice for Kotlin
        }
        
        // Create Maven publication if it doesn't exist
        val publishing = project.extensions.getByType<PublishingExtension>()
        if (publishing.publications.findByName("maven") == null) {
            publishing.publications.register<MavenPublication>("maven") {
                from(project.components["java"])
                artifact(javadocJar)
                configurePom(project, config)
            }
        } else {
            // Configure existing maven publication
            publishing.publications.withType<MavenPublication>().named("maven").configure {
                artifact(javadocJar)
                configurePom(project, config)
            }
        }
    }
    
    private fun configureSigning(project: Project, config: CentralPublisherConfig) {
        project.plugins.apply("signing")
        project.plugins.withId("signing") {
            project.configure<SigningExtension> {
                val publishing = project.extensions.getByType<PublishingExtension>()
                sign(publishing.publications)
                
                // Use in-memory PGP keys if available (following original pattern)
                val signingKey = project.get("SIGNING_KEY")
                val signingPassword = project.get("SIGNING_PASSWORD")
                
                if (signingKey != "SIGNING_KEY not found") {
                    project.logger.info("✅ Using in-memory GPG keys for signing")
                    useInMemoryPgpKeys(signingKey, signingPassword)
                } else if (config.signing.keyId.isNotBlank()) {
                    // Fall back to file-based signing if configured
                    project.logger.info("✅ Using file-based signing with keyId: ${config.signing.keyId}")
                } else {
                    project.logger.warn("⚠️ No signing configuration found. Artifacts will not be signed.")
                    project.logger.warn("   Set SIGNING_KEY and SIGNING_PASSWORD in gradle.properties or environment variables")
                }
            }
        }
    }
    
    private fun MavenPublication.configurePom(project: Project, config: CentralPublisherConfig) {
        pom {
            name.set(config.projectInfo.name.ifBlank { project.name })
            description.set(config.projectInfo.description)
            url.set(config.projectInfo.url)
            
            // Configure license
            licenses {
                license {
                    name.set(config.projectInfo.license.name)
                    url.set(config.projectInfo.license.url)
                    distribution.set(config.projectInfo.license.distribution)
                }
            }
            
            // Configure developers
            developers {
                config.projectInfo.developers.forEach { dev ->
                    developer {
                        id.set(dev.id)
                        name.set(dev.name)
                        email.set(dev.email)
                        organization.set(dev.organization)
                        organizationUrl.set(dev.organizationUrl)
                    }
                }
            }
            
            // Configure SCM
            scm {
                url.set(config.projectInfo.scm.url)
                connection.set(config.projectInfo.scm.connection)
                developerConnection.set(config.projectInfo.scm.developerConnection)
            }
        }
    }
}

/**
 * Publication provider for Kotlin Multiplatform projects.
 * 
 * Supports:
 * - Kotlin Multiplatform projects with `org.jetbrains.kotlin.multiplatform` plugin
 * - Automatic POM configuration for all targets
 * - Signing configuration for all publications
 */
class KotlinMultiplatformPublicationProvider : PublicationProvider {
    
    override fun configurePublications(project: Project, config: CentralPublisherConfig) {
        // Only configure if Kotlin Multiplatform plugin is applied
        if (project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
            // Auto-apply maven-publish plugin for KMP projects
            if (!project.plugins.hasPlugin("maven-publish")) {
                project.plugins.apply("maven-publish")
            }
            
            configureKotlinMultiplatformProject(project, config)
            
            // Configure signing if signing information is provided
            if (config.signing.keyId.isNotBlank()) {
                configureSigning(project, config)
            }
        }
    }
    
    internal fun configureKotlinMultiplatformProject(project: Project, config: CentralPublisherConfig) {
        // KMP plugin automatically creates publications and sources JARs
        // We just need to configure POM metadata for all publications created by KMP plugin
        val publishing = project.extensions.getByType<PublishingExtension>()
        publishing.publications.withType(MavenPublication::class.java).configureEach {
            configurePom(project, config)
        }
    }
    
    private fun configureSigning(project: Project, config: CentralPublisherConfig) {
        if (!project.plugins.hasPlugin("signing")) {
            project.plugins.apply("signing")
        }
        
        project.extensions.configure<SigningExtension> {
            val publishing = project.extensions.getByType<PublishingExtension>()
            sign(publishing.publications)
        }
    }
    
    private fun MavenPublication.configurePom(project: Project, config: CentralPublisherConfig) {
        pom {
            name.set(config.projectInfo.name.ifBlank { project.name })
            description.set(config.projectInfo.description)
            url.set(config.projectInfo.url)
            
            // Configure license
            licenses {
                license {
                    name.set(config.projectInfo.license.name)
                    url.set(config.projectInfo.license.url)
                    distribution.set(config.projectInfo.license.distribution)
                }
            }
            
            // Configure developers
            developers {
                config.projectInfo.developers.forEach { dev ->
                    developer {
                        id.set(dev.id)
                        name.set(dev.name)
                        email.set(dev.email)
                        organization.set(dev.organization)
                        organizationUrl.set(dev.organizationUrl)
                    }
                }
            }
            
            // Configure SCM
            scm {
                url.set(config.projectInfo.scm.url)
                connection.set(config.projectInfo.scm.connection)
                developerConnection.set(config.projectInfo.scm.developerConnection)
            }
        }
    }
}

/**
 * Registry for managing publication providers.
 * 
 * Automatically detects project type and applies appropriate providers.
 * Uses mutual exclusion to ensure only one provider configures the project.
 */
class PublicationProviderRegistry {
    
    private val jvmProvider = JvmPublicationProvider()
    private val kmpProvider = KotlinMultiplatformPublicationProvider()
    
    /**
     * Configure publications for the project using the most appropriate provider.
     * Uses project type detection to select the right provider.
     * 
     * @param project The Gradle project to configure
     * @param config The Central Publisher configuration
     */
    fun configurePublications(project: Project, config: CentralPublisherConfig) {
        when {
            // Kotlin Multiplatform projects take precedence
            project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform") -> {
                kmpProvider.configurePublications(project, config)
            }
            // JVM projects (Java or Kotlin/JVM)
            project.plugins.hasPlugin("java") || 
            project.plugins.hasPlugin("org.jetbrains.kotlin.jvm") -> {
                jvmProvider.configurePublications(project, config)
            }
            // For projects with no recognized plugins, apply basic maven-publish
            else -> {
                if (!project.plugins.hasPlugin("maven-publish")) {
                    project.plugins.apply("maven-publish")
                }
            }
        }
    }
}