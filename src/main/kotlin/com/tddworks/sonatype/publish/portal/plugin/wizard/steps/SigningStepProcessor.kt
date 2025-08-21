package com.tddworks.sonatype.publish.portal.plugin.wizard.steps

import com.tddworks.sonatype.publish.portal.plugin.wizard.*
import java.io.File

/**
 * Processes the signing step of the setup wizard
 */
class SigningStepProcessor : WizardStepProcessor {
    override val step = WizardStep.SIGNING
    
    override fun process(
        context: WizardContext,
        promptSystem: PromptSystem
    ): WizardStepResult {
        val validationErrors = mutableListOf<String>()
        var updatedContext = context
        
        // Check for existing environment variables
        val envKey = System.getenv("SIGNING_KEY")
        val envPassword = System.getenv("SIGNING_PASSWORD")
        val hasEnvSigning = !envKey.isNullOrBlank() && !envPassword.isNullOrBlank()
        
        // Check for existing global gradle.properties (only if enabled)
        val globalGradleProps = if (context.enableGlobalGradlePropsDetection) {
            File(System.getProperty("user.home"), ".gradle/gradle.properties")
        } else null
        val globalKey = if (globalGradleProps?.exists() == true) {
            globalGradleProps.readLines().find { it.startsWith("SIGNING_KEY=") }?.substringAfter("=")?.trim()
        } else null
        val globalPassword = if (globalGradleProps?.exists() == true) {
            globalGradleProps.readLines().find { it.startsWith("SIGNING_PASSWORD=") }?.substringAfter("=")?.trim()
        } else null
        val hasGlobalSigning = !globalKey.isNullOrBlank() && !globalPassword.isNullOrBlank()
        
        when {
            hasEnvSigning -> {
                val useAutoDetected = promptSystem.confirm("""
                    🔐 SIGNING SETUP - AUTO-DETECTED!
                    ✅ Found existing environment variables:
                    • SIGNING_KEY: ${maskKey(envKey!!)}
                    • SIGNING_PASSWORD: ${"*".repeat(envPassword!!.length.coerceAtMost(8))}
                    
                    Use these auto-detected signing credentials?
                """.trimIndent())
                
                if (useAutoDetected) {
                    updatedContext = context.updateConfig(
                        context.wizardConfig.copy(
                            signing = context.wizardConfig.signing.copy(
                                keyId = envKey,
                                password = envPassword
                            )
                        )
                    ).withAutoDetectedSigning()
                } else {
                    updatedContext = handleManualSigningInput(context, promptSystem, validationErrors)
                }
            }
            
            hasGlobalSigning -> {
                val useAutoDetected = promptSystem.confirm("""
                    🔐 SIGNING SETUP - AUTO-DETECTED!
                    ✅ Found existing global gradle.properties (~/.gradle/gradle.properties):
                    • SIGNING_KEY: ${maskKey(globalKey!!)}
                    • SIGNING_PASSWORD: ${"*".repeat(globalPassword!!.length.coerceAtMost(8))}
                    
                    Use these auto-detected signing credentials?
                """.trimIndent())
                
                if (useAutoDetected) {
                    updatedContext = context.updateConfig(
                        context.wizardConfig.copy(
                            signing = context.wizardConfig.signing.copy(
                                keyId = globalKey,
                                password = globalPassword
                            )
                        )
                    ).withAutoDetectedSigning()
                } else {
                    updatedContext = handleManualSigningInput(context, promptSystem, validationErrors)
                }
            }
            
            else -> {
                updatedContext = handleManualSigningInput(context, promptSystem, validationErrors)
            }
        }
        
        return WizardStepResult(
            currentStep = step,
            isValid = validationErrors.isEmpty(),
            validationErrors = validationErrors,
            updatedContext = updatedContext
        )
    }
    
    private fun handleManualSigningInput(
        context: WizardContext,
        promptSystem: PromptSystem,
        validationErrors: MutableList<String>
    ): WizardContext {
        // Show configuration options
        promptSystem.prompt("""
            🔐 SIGNING SETUP
            No auto-detected signing credentials or user chose manual input. Manual configuration needed.
            
            Configuration options (in order of preference):
            1. Environment variables (recommended for CI/CD):
               export SIGNING_KEY=your-private-key
               export SIGNING_PASSWORD=your-key-password
            
            2. Global gradle.properties (~/.gradle/gradle.properties):
               SIGNING_KEY=your-private-key
               SIGNING_PASSWORD=your-key-password
            
            3. Local gradle.properties (this project only - not recommended):
               Will be generated for you but should not be committed to git
            
            Press Enter to continue...
        """.trimIndent())
        
        val keyId = promptSystem.prompt("Enter your GPG signing key (private key or key ID):")
        if (keyId.isEmpty()) {
            validationErrors.add("Signing key is required")
            return context
        } else {
            val password = promptSystem.prompt("Enter your GPG key password:")
            return context.updateConfig(
                context.wizardConfig.copy(
                    signing = context.wizardConfig.signing.copy(
                        keyId = keyId,
                        password = password
                    )
                )
            )
        }
    }
    
    private fun maskKey(key: String): String {
        return when {
            key.length <= 8 -> "*".repeat(key.length)
            else -> key.take(4) + "*".repeat(key.length - 8) + key.takeLast(4)
        }
    }
}