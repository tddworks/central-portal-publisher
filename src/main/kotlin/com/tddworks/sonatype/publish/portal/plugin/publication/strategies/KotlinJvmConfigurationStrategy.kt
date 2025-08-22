package com.tddworks.sonatype.publish.portal.plugin.publication.strategies

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.publication.PluginConfigurationStrategy
import com.tddworks.sonatype.publish.portal.plugin.publication.configureSigningIfAvailable
import com.tddworks.sonatype.publish.portal.plugin.publication.configurePom
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register

/**
 * Configuration strategy for Kotlin JVM projects.
 * 
 * This strategy handles projects that use the Kotlin JVM plugin by:
 * - Applying maven-publish plugin
 * - Creating a Maven publication with main JAR, sources JAR, and Javadoc JAR
 * - Configuring the publication with project metadata
 */
class KotlinJvmConfigurationStrategy : PluginConfigurationStrategy {
    
    override fun canHandle(project: Project): Boolean {
        return project.plugins.hasPlugin("org.jetbrains.kotlin.jvm")
    }
    
    override fun configure(project: Project, config: CentralPublisherConfig) {
        try {
            // Apply required plugins
            project.plugins.apply("maven-publish")
            project.plugins.apply("signing")
            
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
            
            // Configure publishing directly - following SRP and OCP principles
            project.extensions.configure<PublishingExtension> {
                publications {
                    create<MavenPublication>("maven") {
                        from(project.components["java"])
                        artifact(javadocJar)
                        configurePom(project, config)
                        
                        // Configure signing right here inside the publication - perfect!
                        configureSigningIfAvailable(project, config)
                    }
                }
            }
            
            project.logger.quiet("Configured Kotlin JVM project for publishing using ${getPluginType()} strategy")
        } catch (e: Exception) {
            project.logger.error("Failed to configure Kotlin JVM project: ${e.message}", e)
            throw e
        }
    }
    
    override fun getPluginType(): String = "kotlin-jvm"
    
    override fun getPriority(): Int = 10 // Higher than basic Java, lower than KMP
}