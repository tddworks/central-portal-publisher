package com.tddworks.sonatype.publish.portal.plugin.wizard

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig

/**
 * Result of starting the wizard
 */
data class WizardStartResult(
    val currentStep: WizardStep,
    val detectedInfo: DetectedProjectInfo
)

/**
 * Result of processing a wizard step
 */
data class WizardStepResult(
    val currentStep: WizardStep,
    val isValid: Boolean,
    val validationErrors: List<String> = emptyList(),
    val canProceed: Boolean = isValid,
    val updatedContext: WizardContext? = null
)

/**
 * Result of completing the entire wizard
 */
data class WizardCompletionResult(
    val isComplete: Boolean,
    val finalConfiguration: CentralPublisherConfig,
    val stepsCompleted: List<WizardStep>,
    val summary: String,
    val filesGenerated: List<String> = emptyList()
)

/**
 * Simplified representation of detected project information
 */
data class DetectedProjectInfo(
    val projectName: String,
    val projectUrl: String = "",
    val developers: List<DetectedDeveloper> = emptyList()
)

/**
 * Simplified representation of detected developer information
 */
data class DetectedDeveloper(
    val name: String,
    val email: String = ""
)