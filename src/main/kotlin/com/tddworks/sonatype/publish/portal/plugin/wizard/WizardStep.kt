package com.tddworks.sonatype.publish.portal.plugin.wizard

/**
 * Represents the different steps in the setup wizard
 */
enum class WizardStep(val displayName: String) {
    WELCOME("Welcome"),
    PROJECT_INFO("Project Information"),
    CREDENTIALS("Credentials"),
    SIGNING("Signing Configuration"),
    REVIEW("Review Configuration");
    
    /**
     * Get the next step in the wizard flow
     */
    fun next(): WizardStep? {
        val steps = values()
        val currentIndex = steps.indexOf(this)
        return if (currentIndex < steps.size - 1) steps[currentIndex + 1] else null
    }
    
    /**
     * Get the previous step in the wizard flow
     */
    fun previous(): WizardStep? {
        val steps = values()
        val currentIndex = steps.indexOf(this)
        return if (currentIndex > 0) steps[currentIndex - 1] else null
    }
    
    /**
     * Check if this step allows navigation back
     */
    fun canGoBack(): Boolean = this != WELCOME
    
    /**
     * Check if this step allows navigation forward
     */
    fun canGoForward(): Boolean = this != REVIEW
}