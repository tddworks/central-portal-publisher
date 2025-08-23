package com.tddworks.sonatype.publish.portal.plugin.wizard.steps

import com.tddworks.sonatype.publish.portal.plugin.wizard.*

/** Processes the review step of the setup wizard */
class ReviewStepProcessor : WizardStepProcessor {
    override val step = WizardStep.REVIEW

    override fun process(context: WizardContext, promptSystem: PromptSystem): WizardStepResult {
        val config = context.wizardConfig
        val detectedInfo = context.detectedInfo

        val reviewSummary = buildString {
            // Add progress indicator
            val totalSteps = WizardStep.values().size
            val currentStepIndex = WizardStep.values().indexOf(step) + 1
            appendLine("📋 CONFIGURATION REVIEW (Step $currentStepIndex of $totalSteps)")
            appendLine("=".repeat(60))
            appendLine()

            appendLine("Project Information:")
            appendLine(
                "• Name: ${config.projectInfo.name.ifBlank { detectedInfo?.projectName ?: "Not set" }}"
            )
            appendLine(
                "• URL: ${config.projectInfo.url.ifBlank { detectedInfo?.projectUrl ?: "Not set" }}"
            )
            appendLine("• License: ${config.projectInfo.license.name}")
            appendLine("• Description: ${config.projectInfo.description}")

            // Show developers from config first, fallback to detected
            val developers =
                if (config.projectInfo.developers.isNotEmpty()) {
                    config.projectInfo.developers.map { dev -> "${dev.name} <${dev.email}>" }
                } else {
                    detectedInfo?.developers?.map { dev -> "${dev.name} <${dev.email}>" }
                        ?: emptyList()
                }

            if (developers.isNotEmpty()) {
                appendLine("• Developers:")
                developers.forEach { dev -> appendLine("  - $dev") }
            }

            appendLine()
            appendLine("Publishing Configuration:")
            appendLine("• Auto-publish: ${config.publishing.autoPublish}")
            appendLine("• Aggregation: ${config.publishing.aggregation}")
            appendLine("• Exclude patterns: ${config.publishing.excludeModules.joinToString()}")

            appendLine()
            appendLine("Security Configuration:")
            // Check if credentials are actually configured (not just auto-detected)
            val hasCredentials =
                config.credentials.username.isNotBlank() && config.credentials.password.isNotBlank()
            when {
                context.hasAutoDetectedCredentials && hasCredentials ->
                    appendLine("✅ Credentials: Auto-detected from ${getCredentialsSource()}")
                hasCredentials -> appendLine("✅ Credentials: Configured")
                else -> appendLine("⚠️ Credentials: Manual configuration required")
            }

            // Check if signing is actually configured (not just auto-detected)
            val hasSigning =
                config.signing.keyId.isNotBlank() && config.signing.password.isNotBlank()
            when {
                context.hasAutoDetectedSigning && hasSigning ->
                    appendLine("✅ Signing: Auto-detected from ${getSigningSource()}")
                hasSigning -> appendLine("✅ Signing: Configured")
                else -> appendLine("⚠️ Signing: Manual configuration required")
            }

            appendLine()
            appendLine("Repository Configuration:")
            appendLine(
                "• Staging repository: https://central.sonatype.com/"
            ) // Default staging repo
            appendLine(
                "• Release repository: https://central.sonatype.com/"
            ) // Default release repo
        }

        // Display the review summary without waiting for input
        promptSystem.display(reviewSummary)

        val confirmed =
            promptSystem.confirm("Does this configuration look correct? Proceed with setup?")

        return if (confirmed) {
            WizardStepResult(currentStep = step, isValid = true, validationErrors = emptyList())
        } else {
            WizardStepResult(
                currentStep = step,
                isValid = false,
                validationErrors = listOf("Please make necessary changes and return to review"),
            )
        }
    }

    private fun getCredentialsSource(): String {
        return if (!System.getenv("SONATYPE_USERNAME").isNullOrBlank()) {
            "environment variables"
        } else {
            "global gradle.properties"
        }
    }

    private fun getSigningSource(): String {
        return if (!System.getenv("SIGNING_KEY").isNullOrBlank()) {
            "environment variables"
        } else {
            "global gradle.properties"
        }
    }
}
