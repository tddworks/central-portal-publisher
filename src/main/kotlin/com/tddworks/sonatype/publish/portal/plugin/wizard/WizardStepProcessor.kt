package com.tddworks.sonatype.publish.portal.plugin.wizard

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import org.gradle.api.Project

/**
 * Interface for processing individual wizard steps. Follows Open-Closed Principle - new steps can
 * be added without modifying existing code.
 */
interface WizardStepProcessor {
    /** The wizard step this processor handles */
    val step: WizardStep

    /** Process the wizard step */
    fun process(context: WizardContext, promptSystem: PromptSystem): WizardStepResult
}

/** Context object containing all wizard state and configuration */
data class WizardContext(
    val project: Project,
    val detectedInfo: DetectedProjectInfo?,
    val wizardConfig: CentralPublisherConfig,
    val hasAutoDetectedCredentials: Boolean = false,
    val hasAutoDetectedSigning: Boolean = false,
    val enableGlobalGradlePropsDetection: Boolean = true,
) {
    /** Update the wizard config */
    fun updateConfig(newConfig: CentralPublisherConfig): WizardContext {
        return copy(wizardConfig = newConfig)
    }

    /** Mark credentials as auto-detected */
    fun withAutoDetectedCredentials(): WizardContext {
        return copy(hasAutoDetectedCredentials = true)
    }

    /** Mark signing as auto-detected */
    fun withAutoDetectedSigning(): WizardContext {
        return copy(hasAutoDetectedSigning = true)
    }

    /** Reset auto-detected credentials flag (when user chooses manual input) */
    fun withManualCredentials(): WizardContext {
        return copy(hasAutoDetectedCredentials = false)
    }

    /** Reset auto-detected signing flag (when user chooses manual input) */
    fun withManualSigning(): WizardContext {
        return copy(hasAutoDetectedSigning = false)
    }
}
