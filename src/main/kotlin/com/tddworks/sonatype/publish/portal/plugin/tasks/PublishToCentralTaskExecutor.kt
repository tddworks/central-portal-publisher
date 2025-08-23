package com.tddworks.sonatype.publish.portal.plugin.tasks

import com.tddworks.sonatype.publish.portal.api.Authentication
import com.tddworks.sonatype.publish.portal.api.DeploymentBundle
import com.tddworks.sonatype.publish.portal.api.PublicationType
import com.tddworks.sonatype.publish.portal.api.SonatypePortalPublisher
import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Executes publishing logic for the publishToCentral task.
 *
 * Follows Single Responsibility Principle - handles only publishing execution.
 */
class PublishToCentralTaskExecutor(
    private val project: Project,
    private val config: CentralPublisherConfig,
) {

    fun execute() {
        project.logger.quiet("🚀 Publishing to Maven Central...")

        try {
            // Check if bundle exists
            val bundleDir = File(project.buildDir, "central-portal")
            val bundleFile = File(bundleDir, "${project.name}-${project.version}-bundle.zip")

            if (!bundleFile.exists()) {
                throw GradleException(
                    "Bundle file not found: ${bundleFile.absolutePath}. Please run bundleArtifacts task first."
                )
            }

            if (config.publishing.dryRun) {
                project.logger.quiet("🏃‍♂️ DRY RUN MODE - No actual upload will be performed")
                project.logger.quiet("📦 Would upload bundle: ${bundleFile.absolutePath}")
                project.logger.quiet("📊 Bundle size: ${bundleFile.length()} bytes")
                project.logger.quiet("🎯 Target: https://central.sonatype.com/")
                project.logger.quiet("👤 User: ${config.credentials.username}")
                project.logger.quiet("✅ Dry run completed successfully!")
            } else {
                project.logger.quiet("📤 Uploading bundle to Sonatype Central Portal...")
                project.logger.quiet("📦 Bundle: ${bundleFile.absolutePath}")
                project.logger.quiet("📊 Bundle size: ${bundleFile.length()} bytes")
                project.logger.quiet("🎯 Target: https://central.sonatype.com/")

                publishToSonatypePortal(bundleFile, config)

                project.logger.quiet("✅ Upload completed successfully!")
                project.logger.quiet(
                    "🔗 Check your deployment status at: https://central.sonatype.com/"
                )

                if (config.publishing.autoPublish) {
                    project.logger.quiet(
                        "🚀 Auto-publish enabled - your artifacts will be released automatically"
                    )
                } else {
                    project.logger.quiet(
                        "⏳ Manual publish required - visit Central Portal to release your deployment"
                    )
                }
            }
        } catch (e: Exception) {
            if (e is GradleException) {
                throw e
            }
            project.logger.error("❌ Publishing failed: ${e.message}")
            throw GradleException("Publishing error: ${e.message}", e)
        }
    }

    private fun simulateUpload(bundleFile: File) {
        // Simulate network delay
        Thread.sleep(100)

        // Validate credentials
        if (config.credentials.username.isEmpty() || config.credentials.password.isEmpty()) {
            throw GradleException(
                "Missing credentials. Please configure SONATYPE_USERNAME and SONATYPE_PASSWORD."
            )
        }

        project.logger.quiet("🔐 Authenticating with Sonatype Central Portal...")
        project.logger.quiet("📤 Uploading ${bundleFile.length()} bytes...")
        project.logger.quiet("✅ Upload successful!")
    }

    /** Publishes the deployment bundle to Sonatype Central Portal. */
    private fun publishToSonatypePortal(bundleFile: File, config: CentralPublisherConfig): String {
        // Create authentication
        val auth =
            Authentication(
                username = config.credentials.username.ifBlank { null },
                password = config.credentials.password.ifBlank { null },
            )

        if (auth.username.isNullOrBlank() || auth.password.isNullOrBlank()) {
            throw IllegalStateException(
                "Username and password are required for publishing. Configure them in gradle.properties or environment variables."
            )
        }

        // Determine publication type based on auto-publish setting
        val publicationType =
            if (config.publishing.autoPublish) {
                PublicationType.AUTOMATIC
            } else {
                PublicationType.USER_MANAGED
            }

        // Create deployment bundle
        val deploymentBundle =
            DeploymentBundle(file = bundleFile, publicationType = publicationType)

        // Publish to Sonatype
        val publisher = SonatypePortalPublisher.default()
        val deploy = publisher.deploy(auth, deploymentBundle)

        project.logger.quiet("🔐 Authenticating with Sonatype Central Portal...")
        project.logger.quiet("📤 Uploading ${bundleFile.length()} bytes...")
        project.logger.quiet("✅ Upload successful with deployment ID: $deploy")

        return deploy
    }
}
