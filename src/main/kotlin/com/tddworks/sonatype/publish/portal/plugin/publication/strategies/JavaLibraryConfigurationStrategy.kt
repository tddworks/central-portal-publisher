package com.tddworks.sonatype.publish.portal.plugin.publication.strategies

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.publication.PluginConfigurationStrategy
import com.tddworks.sonatype.publish.portal.plugin.publication.configurePom
import com.tddworks.sonatype.publish.portal.plugin.publication.configureSigningIfAvailable
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get

/**
 * Configuration strategy for Java Library projects.
 *
 * This strategy handles projects that use the Java Library plugin by:
 * - Applying maven-publish plugin
 * - Creating a Maven publication with main JAR, sources JAR, and Javadoc JAR
 * - Configuring the publication with project metadata
 */
class JavaLibraryConfigurationStrategy : PluginConfigurationStrategy {

    override fun canHandle(project: Project): Boolean {
        return project.plugins.hasPlugin("java-library") || project.plugins.hasPlugin("java")
    }

    override fun configure(project: Project, config: CentralPublisherConfig) {
        try {
            // Apply required plugins
            project.plugins.apply("maven-publish")
            // Note: signing plugin should be applied by user if they want signing

            // Configure sources jar
            project.extensions.configure<JavaPluginExtension> {
                withSourcesJar()
                withJavadocJar()
            }

            // Configure publishing directly - following SRP and OCP principles
            project.extensions.configure<PublishingExtension> {
                publications {
                    create<MavenPublication>("maven") {
                        from(project.components["java"])
                        configurePom(project, config)

                        // Configure signing right here inside the publication - perfect!
                        configureSigningIfAvailable(project, config)
                    }
                }
            }

            project.logger.quiet(
                "Configured Java Library project for publishing using ${getPluginType()} strategy"
            )
        } catch (e: Exception) {
            project.logger.error("Failed to configure Java Library project: ${e.message}", e)
            throw e
        }
    }

    override fun getPluginType(): String = "java-library"

    override fun getPriority(): Int = 5 // Lower than Kotlin variants
}
