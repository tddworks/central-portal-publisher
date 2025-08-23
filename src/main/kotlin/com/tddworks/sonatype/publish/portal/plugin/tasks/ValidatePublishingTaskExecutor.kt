package com.tddworks.sonatype.publish.portal.plugin.tasks

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.validation.ValidationEngine
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Executes validation logic for the validatePublishing task.
 *
 * Follows Single Responsibility Principle - handles only validation execution.
 */
class ValidatePublishingTaskExecutor(
    private val project: Project,
    private val config: CentralPublisherConfig,
) {

    fun execute() {
        project.logger.quiet("✅ Validating publishing configuration...")

        try {
            val validationEngine = ValidationEngine()
            val result = validationEngine.validate(config)

            if (result.violations.isEmpty()) {
                project.logger.quiet("✅ All validation checks passed!")
                project.logger.quiet("📋 Configuration summary:")
                project.logger.quiet("   • Project: ${config.projectInfo.name}")
                project.logger.quiet("   • Version: ${project.version}")
                project.logger.quiet(
                    "   • Credentials: ${if (config.credentials.username.isNotEmpty()) "✓ Configured" else "⚠ Missing"}"
                )
                project.logger.quiet(
                    "   • Signing: ${if (config.signing.keyId.isNotEmpty() || config.signing.secretKeyRingFile.isNotEmpty()) "✓ Configured" else "⚠ Missing"}"
                )
                project.logger.quiet(
                    "   • License: ${config.projectInfo.license.name.ifEmpty { "⚠ Not specified" }}"
                )
                project.logger.quiet(
                    "💡 Ready to publish! Run './gradlew publishToCentral' when ready."
                )
            } else {
                project.logger.error("❌ Configuration validation failed:")
                result.violations.forEach { violation ->
                    project.logger.error("   • ${violation.severity}: ${violation.message}")
                    violation.suggestion?.let { suggestion ->
                        project.logger.error("     💡 $suggestion")
                    }
                }
                throw GradleException(
                    "Configuration validation failed. Please fix the issues above."
                )
            }
        } catch (e: Exception) {
            if (e is GradleException) {
                throw e
            }
            project.logger.error("❌ Validation failed with error: ${e.message}")
            throw GradleException("Validation error: ${e.message}", e)
        }
    }
}
