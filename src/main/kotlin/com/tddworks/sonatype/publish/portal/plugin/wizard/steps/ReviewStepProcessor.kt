package com.tddworks.sonatype.publish.portal.plugin.wizard.steps

import com.tddworks.sonatype.publish.portal.plugin.wizard.*

/**
 * Processes the review step of the setup wizard
 */
class ReviewStepProcessor : WizardStepProcessor {
    override val step = WizardStep.REVIEW
    
    override fun process(
        context: WizardContext,
        promptSystem: PromptSystem
    ): WizardStepResult {
        val config = context.wizardConfig
        val detectedInfo = context.detectedInfo
        
        val reviewSummary = buildString {
            appendLine("📋 CONFIGURATION REVIEW")
            appendLine("=".repeat(50))
            appendLine()
            
            appendLine("Project Information:")
            appendLine("• Name: ${config.projectInfo.name.ifBlank { detectedInfo?.projectName ?: "Not set" }}")
            appendLine("• URL: ${config.projectInfo.url.ifBlank { detectedInfo?.projectUrl ?: "Not set" }}")
            appendLine("• License: ${config.projectInfo.license.name}")
            appendLine("• Description: ${config.projectInfo.description}")
            
            // Show developers from config first, fallback to detected
            val developers = if (config.projectInfo.developers.isNotEmpty()) {
                config.projectInfo.developers.map { dev -> "${dev.name} <${dev.email}>" }
            } else {
                detectedInfo?.developers?.map { dev -> "${dev.name} <${dev.email}>" } ?: emptyList()
            }
            
            if (developers.isNotEmpty()) {
                appendLine("• Developers:")
                developers.forEach { dev ->
                    appendLine("  - $dev")
                }
            }
            
            appendLine()
            appendLine("Publishing Configuration:")
            appendLine("• Auto-publish: ${config.publishing.autoPublish}")
            appendLine("• Aggregation: ${config.publishing.aggregation}")
            appendLine("• Exclude patterns: ${config.publishing.excludeModules.joinToString()}")
            
            appendLine()
            appendLine("Security Configuration:")
            when {
                context.hasAutoDetectedCredentials -> 
                    appendLine("✅ Credentials: Auto-detected from ${getCredentialsSource()}")
                else -> 
                    appendLine("⚠️ Credentials: Manual configuration required")
            }
            
            when {
                context.hasAutoDetectedSigning -> 
                    appendLine("✅ Signing: Auto-detected from ${getSigningSource()}")
                else -> 
                    appendLine("⚠️ Signing: Manual configuration required")
            }
            
            appendLine()
            appendLine("Repository Configuration:")
            appendLine("• Staging repository: https://central.sonatype.com/") // Default staging repo
            appendLine("• Release repository: https://central.sonatype.com/") // Default release repo
        }
        
        promptSystem.prompt(reviewSummary)
        
        val confirmed = promptSystem.confirm("Does this configuration look correct? Proceed with setup?")
        
        return if (confirmed) {
            WizardStepResult(
                currentStep = step,
                isValid = true,
                validationErrors = emptyList()
            )
        } else {
            WizardStepResult(
                currentStep = step,
                isValid = false,
                validationErrors = listOf("Please make necessary changes and return to review")
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