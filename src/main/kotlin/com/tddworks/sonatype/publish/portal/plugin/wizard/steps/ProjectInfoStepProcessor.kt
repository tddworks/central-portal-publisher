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
        // If no project info was detected, go directly to manual input
        if (context.detectedInfo == null) {
            return handleManualInput(context, promptSystem)
        }
        
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
            return handleManualInput(context, promptSystem)
        }
        
        return WizardStepResult(
            currentStep = step,
            isValid = true,
            validationErrors = emptyList()
        )
    }
    
    private fun handleManualInput(context: WizardContext, promptSystem: PromptSystem): WizardStepResult {
        val validationErrors = mutableListOf<String>()
        
        // Prompt for project information
        val projectName = promptSystem.prompt("Enter project name:")?.trim() ?: ""
        val projectDescription = promptSystem.prompt("Enter project description:")?.trim() ?: ""
        val projectUrl = promptSystem.prompt("Enter project URL:")?.trim() ?: ""
        
        // License information
        val licenseName = promptSystem.prompt("Enter license name (e.g., Apache License 2.0):")?.trim() ?: ""
        val licenseUrl = promptSystem.prompt("Enter license URL:")?.trim() ?: ""
        
        // Developer information
        val developerId = promptSystem.prompt("Enter developer ID:")?.trim() ?: ""
        val developerName = promptSystem.prompt("Enter developer name:")?.trim() ?: ""
        val developerEmail = promptSystem.prompt("Enter developer email:")?.trim() ?: ""
        
        // Validate required fields
        if (projectName.isEmpty()) {
            validationErrors.add("Project name is required")
        }
        if (projectUrl.isEmpty()) {
            validationErrors.add("Project URL is required")
        }
        if (developerName.isEmpty()) {
            validationErrors.add("Developer name is required")
        }
        if (developerEmail.isEmpty()) {
            validationErrors.add("Developer email is required")
        }
        
        // If validation failed, return errors
        if (validationErrors.isNotEmpty()) {
            return WizardStepResult(
                currentStep = step,
                isValid = false,
                validationErrors = validationErrors
            )
        }
        
        // Create updated configuration with manual input
        val updatedConfig = context.wizardConfig.copy(
            projectInfo = context.wizardConfig.projectInfo.copy(
                name = projectName,
                description = projectDescription,
                url = projectUrl,
                license = context.wizardConfig.projectInfo.license.copy(
                    name = licenseName,
                    url = licenseUrl
                ),
                developers = listOf(
                    com.tddworks.sonatype.publish.portal.plugin.config.DeveloperConfig(
                        id = developerId.ifEmpty { developerEmail.substringBefore("@") },
                        name = developerName,
                        email = developerEmail
                    )
                )
            )
        )
        
        // Return updated context
        val updatedContext = context.copy(wizardConfig = updatedConfig)
        
        return WizardStepResult(
            currentStep = step,
            isValid = true,
            validationErrors = emptyList(),
            updatedContext = updatedContext
        )
    }
}