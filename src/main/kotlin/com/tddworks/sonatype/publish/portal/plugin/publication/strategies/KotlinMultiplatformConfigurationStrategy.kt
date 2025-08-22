package com.tddworks.sonatype.publish.portal.plugin.publication.strategies

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.publication.PluginConfigurationStrategy
import com.tddworks.sonatype.publish.portal.plugin.publication.configureSigningIfAvailable
import com.tddworks.sonatype.publish.portal.plugin.publication.configurePom
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Configuration strategy for Kotlin Multiplatform projects.
 * 
 * This strategy handles projects that use the Kotlin Multiplatform plugin by:
 * - Applying maven-publish plugin (usually already applied by KMP plugin)
 * - Configuring KMP extension to ensure sources jars are created
 * - Configuring POM metadata for all existing KMP target publications
 * - Following KMP plugin's publication model without interfering
 *
 * uses existing publications created by KMP plugin rather than creating new ones.
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
            // Apply required plugins (though KMP usually applies maven-publish)
            project.plugins.apply("maven-publish")
            project.plugins.apply("signing")
            
            // Configure KMP-specific settings using the more idiomatic approach
            // Only attempt if the extension exists (avoids warnings in test environments)
            if (project.extensions.findByType(KotlinMultiplatformExtension::class.java) != null) {
                project.extensions.configure<KotlinMultiplatformExtension> {
                    // Ensure sources jars are created for all targets (KMP best practice)
                    withSourcesJar()
                    
                    // TODO: Future enhancement - Configure Android variants
                    // targets.configureEach { target ->
                    //     if (target is KotlinAndroidTarget) {
                    //         // Configure Android variant publishing if needed
                    //     }
                    // }
                }
            } else {
                // In test environments or if KMP plugin hasn't fully applied yet
                project.logger.debug("KMP extension not yet available - will be configured when plugin is fully applied")
            }
            
            // Configure POM metadata and signing for all KMP publications
            // This is our core responsibility - setting up Maven Central metadata
            project.extensions.configure<PublishingExtension> {
                publications.withType<MavenPublication> {
                    configurePom(project, config)
                    
                    // Configure signing for each KMP publication individually - lower cognitive load!
                    configureSigningIfAvailable(project, config)
                }
            }
            
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