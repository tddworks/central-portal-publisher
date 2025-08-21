package com.tddworks.sonatype.publish.portal.plugin.wizard.steps

import com.tddworks.sonatype.publish.portal.plugin.wizard.*
import java.io.File

/**
 * Processes the credentials step of the setup wizard
 */
class CredentialsStepProcessor : WizardStepProcessor {
    override val step = WizardStep.CREDENTIALS
    
    override fun process(
        context: WizardContext,
        promptSystem: PromptSystem
    ): WizardStepResult {
        val validationErrors = mutableListOf<String>()
        var updatedContext = context
        
        // Check for existing environment variables
        val envUsername = System.getenv("SONATYPE_USERNAME")
        val envPassword = System.getenv("SONATYPE_PASSWORD")
        val hasEnvCredentials = !envUsername.isNullOrBlank() && !envPassword.isNullOrBlank()
        
        // Check for existing global gradle.properties (only if enabled)
        val globalGradleProps = if (context.enableGlobalGradlePropsDetection) {
            File(System.getProperty("user.home"), ".gradle/gradle.properties")
        } else null
        val globalUsername = if (globalGradleProps?.exists() == true) {
            globalGradleProps.readLines().find { it.startsWith("SONATYPE_USERNAME=") }?.substringAfter("=")?.trim()
        } else null
        val globalPassword = if (globalGradleProps?.exists() == true) {
            globalGradleProps.readLines().find { it.startsWith("SONATYPE_PASSWORD=") }?.substringAfter("=")?.trim()
        } else null
        val hasGlobalCredentials = !globalUsername.isNullOrBlank() && !globalPassword.isNullOrBlank()
        
        when {
            hasEnvCredentials -> {
                val useAutoDetected = promptSystem.confirm("""
                    📋 CREDENTIALS SETUP - AUTO-DETECTED!
                    ✅ Found existing environment variables:
                    • SONATYPE_USERNAME: $envUsername
                    • SONATYPE_PASSWORD: ${"*".repeat(envPassword!!.length.coerceAtMost(8))}
                    
                    Use these auto-detected credentials?
                """.trimIndent())
                
                if (useAutoDetected) {
                    updatedContext = context.updateConfig(
                        context.wizardConfig.copy(
                            credentials = context.wizardConfig.credentials.copy(
                                username = envUsername!!,
                                password = envPassword
                            )
                        )
                    ).withAutoDetectedCredentials()
                } else {
                    updatedContext = handleManualCredentialsInput(context, promptSystem, validationErrors)
                }
            }
            
            hasGlobalCredentials -> {
                val useAutoDetected = promptSystem.confirm("""
                    📋 CREDENTIALS SETUP - AUTO-DETECTED!
                    ✅ Found existing global gradle.properties (~/.gradle/gradle.properties):
                    • SONATYPE_USERNAME: $globalUsername
                    • SONATYPE_PASSWORD: ${"*".repeat(globalPassword!!.length.coerceAtMost(8))}
                    
                    Use these auto-detected credentials?
                """.trimIndent())
                
                if (useAutoDetected) {
                    updatedContext = context.updateConfig(
                        context.wizardConfig.copy(
                            credentials = context.wizardConfig.credentials.copy(
                                username = globalUsername!!,
                                password = globalPassword
                            )
                        )
                    ).withAutoDetectedCredentials()
                } else {
                    updatedContext = handleManualCredentialsInput(context, promptSystem, validationErrors)
                }
            }
            
            else -> {
                updatedContext = handleManualCredentialsInput(context, promptSystem, validationErrors)
            }
        }
        
        return WizardStepResult(
            currentStep = step,
            isValid = validationErrors.isEmpty(),
            validationErrors = validationErrors,
            updatedContext = updatedContext
        )
    }
    
    private fun handleManualCredentialsInput(
        context: WizardContext,
        promptSystem: PromptSystem,
        validationErrors: MutableList<String>
    ): WizardContext {
        // Show configuration options
        promptSystem.prompt("""
            📋 CREDENTIALS SETUP
            No auto-detected credentials or user chose manual input. Manual configuration needed.
            
            Configuration options (in order of preference):
            1. Environment variables (recommended for CI/CD):
               export SONATYPE_USERNAME=your-username
               export SONATYPE_PASSWORD=your-password
            
            2. Global gradle.properties (~/.gradle/gradle.properties):
               SONATYPE_USERNAME=your-username
               SONATYPE_PASSWORD=your-password
            
            3. Local gradle.properties (this project only - not recommended):
               Will be generated for you but should not be committed to git
            
            Press Enter to continue...
        """.trimIndent())
        
        val username = promptSystem.prompt("Enter your Sonatype username:")
        if (username.isEmpty()) {
            validationErrors.add("Username is required")
            return context
        } else {
            val password = promptSystem.prompt("Enter your Sonatype password/token:")
            return context.updateConfig(
                context.wizardConfig.copy(
                    credentials = context.wizardConfig.credentials.copy(
                        username = username,
                        password = password
                    )
                )
            )
        }
    }
}