package com.tddworks.sonatype.publish.portal.plugin.wizard.steps

import com.tddworks.sonatype.publish.portal.plugin.wizard.*

/**
 * Processes the project information step of the setup wizard
 */
class ProjectInfoStepProcessor : WizardStepProcessor {
    override val step = WizardStep.PROJECT_INFO
    
    override fun process(
        context: WizardContext,
        promptSystem: PromptSystem
    ): WizardStepResult {
        // Show what was detected first
        promptSystem.prompt("""
            Auto-detected project information:
            • Project: ${context.detectedInfo?.projectName ?: "not detected"}
            • URL: ${context.detectedInfo?.projectUrl ?: "not detected"}
            • Developers: ${context.detectedInfo?.developers?.map { "${it.name} <${it.email}>" }?.joinToString() ?: "not detected"}
            
            Press Enter to continue...
        """.trimIndent())
        
        val confirmed = promptSystem.confirm("Use this auto-detected project information?")
        if (!confirmed) {
            // Could prompt for manual input here in future
            promptSystem.prompt("Manual project setup not yet implemented. Using auto-detected values...")
        }
        
        return WizardStepResult(
            currentStep = step,
            isValid = true,
            validationErrors = emptyList()
        )
    }
}