package com.tddworks.sonatype.publish.portal.plugin.publication

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.plugins.signing.SigningExtension

/**
 * Simple signing configurator following VanNikTech's clean approach.
 * 
 * Handles signing configuration for all publication strategies using a unified approach.
 */
object SigningConfigurator {
    
    /**
     * Configures signing for all Maven publications if signing credentials are available.
     * 
     * Uses VanNikTech's simple pattern but adapted to our existing approach.
     */
    fun configureSigningIfAvailable(project: Project, config: CentralPublisherConfig) {
        project.plugins.withId("signing") {
            // Check for in-memory keys first
            val signingKey = project.findProperty("SIGNING_KEY")?.toString() ?: System.getenv("SIGNING_KEY")
            val signingPassword = project.findProperty("SIGNING_PASSWORD")?.toString() ?: System.getenv("SIGNING_PASSWORD")
            
            when {
                !signingKey.isNullOrBlank() -> {
                    project.logger.quiet("🔐 Using in-memory GPG keys for signing")
                    project.configure<SigningExtension> {
                        useInMemoryPgpKeys(signingKey, signingPassword ?: "")
                    }
                    signAllPublications(project)
                }
                config.signing.keyId.isNotBlank() -> {
                    project.logger.quiet("🔐 Using file-based signing with keyId: ${config.signing.keyId}")
                    signAllPublications(project)
                }
                else -> {
                    project.logger.quiet("⚠️ No signing configuration found - artifacts will be unsigned")
                    // Don't call sign() when no configuration available
                }
            }
        }
    }
    
    /**
     * Signs all Maven publications using VanNikTech's pattern adapted to our codebase.
     */
    private fun signAllPublications(project: Project) {
        val signing = project.extensions.getByType(SigningExtension::class.java)
        project.mavenPublications(object : Action<MavenPublication> {
            override fun execute(publication: MavenPublication) {
                signing.sign(publication)
            }
        })
    }
    
    /**
     * Extension function following VanNikTech's exact pattern.
     */
    private fun Project.mavenPublications(action: Action<MavenPublication>) {
        gradlePublishing.publications.withType(MavenPublication::class.java).configureEach(action)
    }
    
    /**
     * Extension property following VanNikTech's exact pattern.
     */
    private val Project.gradlePublishing: PublishingExtension
        get() = extensions.getByType(PublishingExtension::class.java)
}