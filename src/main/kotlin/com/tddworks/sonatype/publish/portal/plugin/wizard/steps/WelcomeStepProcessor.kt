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
        // Welcome step - just show detected info
        // No validation needed
        return WizardStepResult(
            currentStep = step,
            isValid = true,
            validationErrors = emptyList()
        )
    }
}