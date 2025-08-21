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
        
        // Handle individual field confirmation
        return handleIndividualFieldConfirmation(context, promptSystem)
    }
    
    private fun handleIndividualFieldConfirmation(
        context: WizardContext,
        promptSystem: PromptSystem
    ): WizardStepResult {
        val detectedInfo = context.detectedInfo!!
        
        // Start with the base config and update as we go
        var configBuilder = context.wizardConfig.projectInfo
        
        // Project Name
        val useProjectName = if (detectedInfo.projectName.isNotBlank()) {
            promptSystem.confirm("Use auto-detected project name '${detectedInfo.projectName}'?")
        } else false
        
        if (useProjectName) {
            configBuilder = configBuilder.copy(name = detectedInfo.projectName)
        } else {
            val manualName = promptSystem.prompt("Enter project name:") ?: ""
            if (manualName.isBlank()) {
                return WizardStepResult(
                    currentStep = step,
                    isValid = false,
                    validationErrors = listOf("Project name is required")
                )
            }
            configBuilder = configBuilder.copy(name = manualName)
        }
        
        // Project URL
        val useProjectUrl = if (detectedInfo.projectUrl.isNotBlank()) {
            promptSystem.confirm("Use auto-detected project URL '${detectedInfo.projectUrl}'?")
        } else false
        
        if (useProjectUrl) {
            configBuilder = configBuilder.copy(url = detectedInfo.projectUrl)
        } else {
            val manualUrl = promptSystem.prompt("Enter project URL:") ?: ""
            configBuilder = configBuilder.copy(url = manualUrl)
        }
        
        // Project Description (always manual for now as we don't auto-detect it)
        val manualDescription = promptSystem.prompt("Enter project description:") ?: ""
        configBuilder = configBuilder.copy(description = manualDescription)
        
        // Developer
        val useDeveloper = if (detectedInfo.developers.isNotEmpty()) {
            val firstDev = detectedInfo.developers.first()
            promptSystem.confirm("Use auto-detected developer '${firstDev.name} <${firstDev.email}>'?")
        } else false
        
        if (useDeveloper) {
            val detectedDev = detectedInfo.developers.first()
            configBuilder = configBuilder.copy(
                developers = listOf(
                    com.tddworks.sonatype.publish.portal.plugin.config.DeveloperConfig(
                        id = detectedDev.email.substringBefore("@"),
                        name = detectedDev.name,
                        email = detectedDev.email
                    )
                )
            )
        } else {
            val developerId = promptSystem.prompt("Enter developer ID:") ?: ""
            val developerName = promptSystem.prompt("Enter developer name:") ?: ""
            val developerEmail = promptSystem.prompt("Enter developer email:") ?: ""
            
            if (developerName.isBlank() || developerEmail.isBlank()) {
                return WizardStepResult(
                    currentStep = step,
                    isValid = false,
                    validationErrors = listOfNotNull(
                        if (developerName.isBlank()) "Developer name is required" else null,
                        if (developerEmail.isBlank()) "Developer email is required" else null
                    )
                )
            }
            
            configBuilder = configBuilder.copy(
                developers = listOf(
                    com.tddworks.sonatype.publish.portal.plugin.config.DeveloperConfig(
                        id = developerId.ifEmpty { developerEmail.substringBefore("@") },
                        name = developerName,
                        email = developerEmail
                    )
                )
            )
        }
        
        // Create updated configuration
        val updatedConfig = context.wizardConfig.copy(projectInfo = configBuilder)
        val updatedContext = context.copy(wizardConfig = updatedConfig)
        
        return WizardStepResult(
            currentStep = step,
            isValid = true,
            validationErrors = emptyList(),
            updatedContext = updatedContext
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