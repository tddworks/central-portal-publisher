package com.tddworks.sonatype.publish.portal.plugin.wizard.steps

import com.tddworks.sonatype.publish.portal.plugin.wizard.*

/**
 * Processes the welcome step of the setup wizard
 */
class WelcomeStepProcessor : WizardStepProcessor {
    override val step = WizardStep.WELCOME
    
    override fun process(
        context: WizardContext,
        promptSystem: PromptSystem
    ): WizardStepResult {
        // Show welcome message with progress indicator
        val totalSteps = WizardStep.values().size
        val currentStepIndex = WizardStep.values().indexOf(step) + 1
        
        val welcomeMessage = buildString {
            appendLine("🧙 WELCOME TO CENTRAL PUBLISHER SETUP (Step $currentStepIndex of $totalSteps)")
            appendLine("=".repeat(60))
            appendLine()
            appendLine("This wizard will help you configure publishing to Maven Central.")
            appendLine()
            appendLine("What we'll configure:")
            appendLine("• Project information (name, URL, description, developers)")
            appendLine("• Sonatype credentials for Maven Central")
            appendLine("• GPG signing configuration")
            appendLine("• Review and test your configuration")
            appendLine()
            
            // Show detected project info if available
            context.detectedInfo?.let { detected ->
                appendLine("🔍 Auto-detected information:")
                if (detected.projectName.isNotBlank()) {
                    appendLine("• Project name: ${detected.projectName}")
                }
                if (detected.projectUrl.isNotBlank()) {
                    appendLine("• Project URL: ${detected.projectUrl}")
                }
                if (detected.developers.isNotEmpty()) {
                    appendLine("• Developers: ${detected.developers.joinToString { "${it.name} <${it.email}>" }}")
                }
                appendLine()
            }
            
            appendLine("Let's get started! 🚀")
        }
        
        // Display welcome message
        promptSystem.display(welcomeMessage)
        
        // Pause for user to read
        promptSystem.prompt("Press Enter to continue...")
        
        return WizardStepResult(
            currentStep = step,
            isValid = true,
            validationErrors = emptyList()
        )
    }
}