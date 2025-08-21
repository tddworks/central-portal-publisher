package com.tddworks.sonatype.publish.portal.plugin.wizard

import com.tddworks.sonatype.publish.portal.plugin.config.*
import com.tddworks.sonatype.publish.portal.plugin.autodetection.AutoDetectionManager
import com.tddworks.sonatype.publish.portal.plugin.autodetection.GitInfoDetector
import com.tddworks.sonatype.publish.portal.plugin.autodetection.ProjectInfoDetector
import com.tddworks.sonatype.publish.portal.plugin.defaults.SmartDefaultManager
import com.tddworks.sonatype.publish.portal.plugin.wizard.steps.*
import org.gradle.api.Project
import java.io.File

/**
 * Refactored Setup Wizard following SOLID principles
 * 
 * This design follows:
 * - Open-Closed Principle: New steps can be added without modifying existing code
 * - Single Responsibility: Each component has one clear responsibility
 * - Dependency Inversion: Depends on abstractions (interfaces) not concrete classes
 */
class RefactoredSetupWizard(
    private val project: Project,
    private val promptSystem: PromptSystem = ConsolePromptSystem(),
    private val stepProcessors: List<WizardStepProcessor> = defaultStepProcessors(),
    private val fileGenerator: WizardFileGenerator = DefaultWizardFileGenerator(),
    private val enableGlobalGradlePropsDetection: Boolean = true
) {
    
    private var _currentStep = WizardStep.WELCOME
    private var _isComplete = false
    private val completedSteps = mutableListOf<WizardStep>()
    private var context: WizardContext
    
    init {
        val detectedInfo = performAutoDetection()
        context = WizardContext(
            project = project,
            detectedInfo = detectedInfo,
            wizardConfig = CentralPublisherConfigBuilder().build(),
            enableGlobalGradlePropsDetection = enableGlobalGradlePropsDetection
        )
    }
    
    val currentStep: WizardStep get() = _currentStep
    val isComplete: Boolean get() = _isComplete
    
    /**
     * Start the wizard and perform auto-detection
     */
    fun start(): WizardStartResult {
        _currentStep = WizardStep.WELCOME
        completedSteps.clear()
        
        return WizardStartResult(
            currentStep = _currentStep,
            detectedInfo = context.detectedInfo!!
        )
    }
    
    /**
     * Process a specific wizard step using the appropriate processor
     */
    fun processStep(step: WizardStep): WizardStepResult {
        _currentStep = step
        
        val processor = stepProcessors.find { it.step == step }
            ?: throw IllegalArgumentException("No processor found for step: $step")
        
        val result = processor.process(context, promptSystem)
        
        // Update context if the step processor returned an updated context
        result.updatedContext?.let { context = it }
        
        return result
    }
    
    /**
     * Run the complete wizard flow
     */
    fun runComplete(): WizardCompletionResult {
        start()
        
        // Process each step
        WizardStep.values().forEach { step ->
            navigateToStep(step)
            val result = processStep(step)
            if (result.isValid) {
                completedSteps.add(step)
            }
        }
        
        // Create final configuration
        val finalConfig = createFinalConfiguration()
        _isComplete = true
        
        // Generate files using the file generator
        val generatedFiles = fileGenerator.generateFiles(context, finalConfig)
        
        return WizardCompletionResult(
            isComplete = true,
            finalConfiguration = finalConfig,
            stepsCompleted = completedSteps.toList(),
            summary = generateSummary(finalConfig),
            filesGenerated = generatedFiles
        )
    }
    
    /**
     * Navigate to a specific step
     */
    fun navigateToStep(step: WizardStep) {
        _currentStep = step
    }
    
    /**
     * Check if can navigate back from current step
     */
    fun canNavigateBack(): Boolean = _currentStep.canGoBack()
    
    /**
     * Check if can navigate forward from current step
     */
    fun canNavigateForward(): Boolean = _currentStep.canGoForward()
    
    private fun performAutoDetection(): DetectedProjectInfo {
        val detectors = listOf(
            GitInfoDetector(),
            ProjectInfoDetector()
        )
        
        val manager = AutoDetectionManager(detectors)
        val summary = manager.detectConfiguration(project)
        
        // Convert detection summary to simplified format
        var developers = summary.config.projectInfo.developers.map { dev ->
            DetectedDeveloper(
                name = dev.name,
                email = dev.email
            )
        }
        
        // If no developers in config, try to extract from detected values
        if (developers.isEmpty()) {
            val detectedName = summary.detectedValues["projectInfo.developer.name"]?.value
            val detectedEmail = summary.detectedValues["projectInfo.developer.email"]?.value
                
            if (!detectedName.isNullOrEmpty() && !detectedEmail.isNullOrEmpty()) {
                developers = listOf(DetectedDeveloper(
                    name = detectedName,
                    email = detectedEmail
                ))
            }
        }
        
        return DetectedProjectInfo(
            projectName = summary.config.projectInfo.name.ifEmpty { project.name },
            projectUrl = summary.config.projectInfo.url,
            developers = developers
        )
    }
    
    private fun createFinalConfiguration(): CentralPublisherConfig {
        val smartDefaultManager = SmartDefaultManager(project)
        val configWithDefaults = smartDefaultManager.applySmartDefaults(project, context.wizardConfig)
        
        return configWithDefaults.copy(
            projectInfo = configWithDefaults.projectInfo.copy(
                name = context.detectedInfo?.projectName ?: project.name,
                url = context.detectedInfo?.projectUrl ?: configWithDefaults.projectInfo.url
            )
        )
    }
    
    private fun generateSummary(finalConfig: CentralPublisherConfig): String {
        return buildString {
            appendLine("Setup completed successfully!")
            appendLine()
            appendLine("Configuration Summary:")
            appendLine("- Project: ${finalConfig.projectInfo.name}")
            appendLine("- License: ${finalConfig.projectInfo.license.name}")
            appendLine("- Auto-publish: ${finalConfig.publishing.autoPublish}")
            appendLine("- Aggregation: ${finalConfig.publishing.aggregation}")
            
            if (context.hasAutoDetectedCredentials || context.hasAutoDetectedSigning) {
                appendLine()
                appendLine("Auto-detection Results:")
                if (context.hasAutoDetectedCredentials) {
                    appendLine("✅ Credentials: Auto-detected from environment/global gradle.properties")
                }
                if (context.hasAutoDetectedSigning) {
                    appendLine("✅ Signing: Auto-detected from environment/global gradle.properties")
                }
            }
            
            appendLine()
            appendLine("Next steps:")
            appendLine("1. Review generated build.gradle.kts")
            
            if (!context.hasAutoDetectedCredentials) {
                appendLine("2. Configure credentials (recommended: environment variables):")
                appendLine("   - Environment: export SONATYPE_USERNAME=... SONATYPE_PASSWORD=...")
                appendLine("   - Global gradle.properties: ~/.gradle/gradle.properties")
                appendLine("   - Local gradle.properties: update generated file (not recommended)")
            }
            
            if (!context.hasAutoDetectedSigning) {
                val stepNum = if (context.hasAutoDetectedCredentials) "2" else "3"
                appendLine("$stepNum. Configure GPG signing:")
                appendLine("   - Environment: export SIGNING_KEY=... SIGNING_PASSWORD=...")
                appendLine("   - Global gradle.properties: ~/.gradle/gradle.properties")
                appendLine("   - Local gradle.properties: update generated file (not recommended)")
            }
            
            val finalStepNum = when {
                context.hasAutoDetectedCredentials && context.hasAutoDetectedSigning -> "2"
                context.hasAutoDetectedCredentials || context.hasAutoDetectedSigning -> "3"
                else -> "4"
            }
            
            appendLine("$finalStepNum. Run './gradlew publishToCentral' to publish")
        }
    }
    
    companion object {
        /**
         * Default step processors - easily extensible for new steps
         */
        fun defaultStepProcessors(): List<WizardStepProcessor> = listOf(
            WelcomeStepProcessor(),
            ProjectInfoStepProcessor(),
            CredentialsStepProcessor(),
            SigningStepProcessor(),
            ReviewStepProcessor(),
            TestStepProcessor()
        )
    }
}