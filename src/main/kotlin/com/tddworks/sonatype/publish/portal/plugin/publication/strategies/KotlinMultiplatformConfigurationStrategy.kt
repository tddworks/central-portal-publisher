package com.tddworks.sonatype.publish.portal.plugin.publication.strategies

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.publication.PluginConfigurationStrategy
import com.tddworks.sonatype.publish.portal.plugin.publication.configurePom
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/**
 * Configuration strategy for Kotlin Multiplatform projects.
 * 
 * This strategy handles projects that use the Kotlin Multiplatform plugin by:
 * - Applying maven-publish plugin (usually already applied by KMP plugin)
 * - Configuring publications for all KMP targets
 * - Setting up appropriate source and documentation artifacts
 * 
 * This strategy has the highest priority as KMP projects often also have
 * kotlin-jvm or other plugins applied.
 */
class KotlinMultiplatformConfigurationStrategy : PluginConfigurationStrategy {
    
    override fun canHandle(project: Project): Boolean {
        return project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
    }
    
    override fun configure(project: Project, config: CentralPublisherConfig) {
        try {
            // Apply maven-publish plugin if not already applied (though KMP usually applies it)
            project.plugins.apply("maven-publish")
            
            // Configure publishing directly - following SRP and OCP principles
            // KMP projects typically have publications already created by the KMP plugin
            // We mainly need to configure the POM metadata for all publications
            project.extensions.configure<PublishingExtension> {
                publications.withType<MavenPublication> {
                    // Configure POM with project metadata for all KMP publications
                    configurePom(project, config)
                }
            }
            
            // TODO: Handle Android variants configuration
            // TODO: Configure dokka for multiplatform documentation
            project.logger.quiet("Configured Kotlin Multiplatform project for publishing using ${getPluginType()} strategy")
        } catch (e: Exception) {
            project.logger.error("Failed to configure Kotlin Multiplatform project: ${e.message}", e)
            throw e
        }
    }
    
    override fun getPluginType(): String = "kotlin-multiplatform"
    
    override fun getPriority(): Int = 20 // Highest priority - should take precedence over kotlin-jvm
}