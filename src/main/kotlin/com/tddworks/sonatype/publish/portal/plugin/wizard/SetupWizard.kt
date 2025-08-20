package com.tddworks.sonatype.publish.portal.plugin.wizard

import com.tddworks.sonatype.publish.portal.plugin.config.*
import com.tddworks.sonatype.publish.portal.plugin.autodetection.AutoDetectionManager
import com.tddworks.sonatype.publish.portal.plugin.autodetection.GitInfoDetector
import com.tddworks.sonatype.publish.portal.plugin.autodetection.ProjectInfoDetector
import com.tddworks.sonatype.publish.portal.plugin.defaults.SmartDefaultManager
import org.gradle.api.Project
import java.io.File

/**
 * Interactive setup wizard for Central Publisher plugin configuration.
 * 
 * Guides users through initial plugin setup with:
 * - Auto-detection of project information
 * - Step-by-step configuration
 * - Input validation
 * - File generation
 */
class SetupWizard(
    private val project: Project,
    private val promptSystem: PromptSystem = ConsolePromptSystem()
) {
    
    private var _currentStep = WizardStep.WELCOME
    private var _isComplete = false
    private val completedSteps = mutableListOf<WizardStep>()
    private var wizardConfig = CentralPublisherConfigBuilder().build()
    private var detectedInfo: DetectedProjectInfo? = null
    
    val currentStep: WizardStep get() = _currentStep
    val isComplete: Boolean get() = _isComplete
    
    /**
     * Create wizard with default console prompt system
     */
    constructor(project: Project) : this(project, ConsolePromptSystem())
    
    /**
     * Start the wizard and perform auto-detection
     */
    fun start(): WizardStartResult {
        _currentStep = WizardStep.WELCOME
        completedSteps.clear()
        
        // Perform auto-detection
        detectedInfo = performAutoDetection()
        
        return WizardStartResult(
            currentStep = _currentStep,
            detectedInfo = detectedInfo!!
        )
    }
    
    /**
     * Run the complete wizard flow
     */
    fun runComplete(): WizardCompletionResult {
        // Start the wizard
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
        
        return WizardCompletionResult(
            isComplete = true,
            finalConfiguration = finalConfig,
            stepsCompleted = completedSteps.toList(),
            summary = generateSummary(finalConfig),
            filesGenerated = listOf("build.gradle.kts", "gradle.properties", ".gitignore", ".github/workflows/publish.yml")
        )
    }
    
    /**
     * Process a specific wizard step
     */
    fun processStep(step: WizardStep): WizardStepResult {
        _currentStep = step
        val validationErrors = mutableListOf<String>()
        
        when (step) {
            WizardStep.WELCOME -> {
                // Welcome step - just show detected info
                // No validation needed
            }
            WizardStep.PROJECT_INFO -> {
                // Handle project info step
                val confirmed = promptSystem.confirm("Use auto-detected project information?")
                if (!confirmed) {
                    // Could prompt for manual input here
                }
            }
            WizardStep.CREDENTIALS -> {
                val username = promptSystem.prompt("Enter username:")
                if (username.isEmpty()) {
                    validationErrors.add("Username is required")
                } else {
                    // Update wizard config with username
                    wizardConfig = wizardConfig.copy(
                        credentials = wizardConfig.credentials.copy(username = username)
                    )
                    
                    val password = promptSystem.prompt("Enter password:")
                    wizardConfig = wizardConfig.copy(
                        credentials = wizardConfig.credentials.copy(password = password)
                    )
                }
            }
            WizardStep.SIGNING -> {
                val keyId = promptSystem.prompt("Enter GPG Key ID:")
                wizardConfig = wizardConfig.copy(
                    signing = wizardConfig.signing.copy(keyId = keyId)
                )
                
                val gpgPassword = promptSystem.prompt("Enter GPG Password:")
                wizardConfig = wizardConfig.copy(
                    signing = wizardConfig.signing.copy(password = gpgPassword)
                )
            }
            WizardStep.REVIEW -> {
                // Review step - confirm configuration
                val confirmed = promptSystem.confirm("Confirm configuration?")
                if (!confirmed) {
                    validationErrors.add("Configuration not confirmed")
                }
            }
            WizardStep.TEST -> {
                // Test step - validate configuration and test connection
                val testConfig = promptSystem.confirm("Test configuration and validate setup?")
                if (testConfig) {
                    // Test basic requirements for publishing
                    if (wizardConfig.credentials.username.isEmpty()) {
                        validationErrors.add("Username is required for testing")
                    }
                    if (wizardConfig.credentials.password.isEmpty()) {
                        validationErrors.add("Password is required for testing")
                    }
                    
                    // Only validate full config if basic credentials are present
                    if (validationErrors.isEmpty()) {
                        try {
                            val finalConfig = createFinalConfiguration()
                            finalConfig.validate()
                        } catch (e: Exception) {
                            validationErrors.add("Configuration validation failed: ${e.message}")
                        }
                    }
                }
            }
        }
        
        return WizardStepResult(
            currentStep = step,
            isValid = validationErrors.isEmpty(),
            validationErrors = validationErrors
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
    
    /**
     * Generate configuration files
     */
    fun generateFiles() {
        generateBuildFile()
        generatePropertiesFile()
        generateGitignoreFile()
        generateCIConfig()
    }
    
    private fun performAutoDetection(): DetectedProjectInfo {
        val detectors = listOf(
            GitInfoDetector(),
            ProjectInfoDetector()
        )
        
        val manager = AutoDetectionManager(detectors)
        val summary = manager.detectConfiguration(project)
        
        // Convert detection summary to simplified format
        // First try to get developers from config (actual developer objects)
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
        // Apply smart defaults
        val smartDefaultManager = SmartDefaultManager(project)
        val configWithDefaults = smartDefaultManager.applySmartDefaults(project, wizardConfig)
        
        // Merge with detected information
        return configWithDefaults.copy(
            projectInfo = configWithDefaults.projectInfo.copy(
                name = detectedInfo?.projectName ?: project.name,
                url = detectedInfo?.projectUrl ?: configWithDefaults.projectInfo.url
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
            appendLine()
            appendLine("Next steps:")
            appendLine("1. Review generated build.gradle.kts")
            appendLine("2. Update gradle.properties with your credentials")
            appendLine("3. Run './gradlew centralPublish' to publish")
        }
    }
    
    private fun generateBuildFile() {
        val buildFile = File(project.projectDir, "build.gradle.kts")
        val content = buildString {
            appendLine("plugins {")
            appendLine("    id(\"com.tddworks.central-publisher\") version \"<latest-version>\"")
            appendLine("}")
            appendLine()
            appendLine("centralPublisher {")
            appendLine("    // Configuration will be loaded from gradle.properties")
            appendLine("    // or can be configured here using DSL")
            appendLine("}")
        }
        buildFile.writeText(content)
    }
    
    private fun generatePropertiesFile() {
        val propsFile = File(project.projectDir, "gradle.properties")
        val existingContent = if (propsFile.exists()) propsFile.readText() else ""
        
        val content = buildString {
            appendLine("# Central Publisher Configuration")
            appendLine("central.username=your-username")
            appendLine("central.password=your-password")
            appendLine()
            appendLine("# Signing Configuration")
            appendLine("signing.keyId=your-key-id")
            appendLine("signing.password=your-gpg-password")
            appendLine("signing.secretKeyRingFile=${System.getProperty("user.home")}/.gnupg/secring.gpg")
            
            if (existingContent.isNotEmpty()) {
                appendLine()
                appendLine("# Existing properties:")
                append(existingContent)
            }
        }
        propsFile.writeText(content)
    }
    
    private fun generateGitignoreFile() {
        val gitignoreFile = File(project.projectDir, ".gitignore")
        val existingContent = if (gitignoreFile.exists()) gitignoreFile.readText() else ""
        
        val newEntries = listOf(
            "# Central Publisher sensitive files",
            "gradle.properties",
            "*.gpg",
            "local.properties",
            "",
            "# Build outputs", 
            "build/",
            ".gradle/",
            "",
            "# IDE files",
            ".idea/",
            "*.iml",
            ".vscode/"
        )
        
        val content = buildString {
            if (existingContent.isNotEmpty()) {
                append(existingContent)
                if (!existingContent.endsWith("\n")) {
                    appendLine()
                }
                appendLine()
            }
            
            newEntries.forEach { entry ->
                if (entry.isEmpty()) {
                    appendLine()
                } else if (!existingContent.contains(entry)) {
                    appendLine(entry)
                }
            }
        }
        
        gitignoreFile.writeText(content)
    }
    
    private fun generateCIConfig() {
        val ciDir = File(project.projectDir, ".github/workflows")
        ciDir.mkdirs()
        
        val ciFile = File(ciDir, "publish.yml")
        val content = buildString {
            appendLine("name: Publish to Maven Central")
            appendLine()
            appendLine("on:")
            appendLine("  push:")
            appendLine("    tags:")
            appendLine("      - 'v*'")
            appendLine()
            appendLine("jobs:")
            appendLine("  publish:")
            appendLine("    runs-on: ubuntu-latest")
            appendLine("    steps:")
            appendLine("      - uses: actions/checkout@v4")
            appendLine("      - uses: actions/setup-java@v3")
            appendLine("        with:")
            appendLine("          java-version: '17'")
            appendLine("          distribution: 'temurin'")
            appendLine()
            appendLine("      - name: Setup Gradle")
            appendLine("        uses: gradle/gradle-build-action@v2")
            appendLine()
            appendLine("      - name: Publish to Central")
            appendLine("        run: ./gradlew centralPublish")
            appendLine("        env:")
            appendLine("          SONATYPE_USERNAME: \${{ secrets.SONATYPE_USERNAME }}")
            appendLine("          SONATYPE_PASSWORD: \${{ secrets.SONATYPE_PASSWORD }}")
            appendLine("          SIGNING_KEY: \${{ secrets.SIGNING_KEY }}")
            appendLine("          SIGNING_PASSWORD: \${{ secrets.SIGNING_PASSWORD }}")
        }
        
        ciFile.writeText(content)
    }
}

/**
 * Default console-based prompt system
 */
class ConsolePromptSystem : PromptSystem {
    
    override fun prompt(message: String): String {
        println(message)
        return readLine() ?: ""
    }
    
    override fun promptWithDefault(message: String, defaultValue: String): String {
        println("$message (default: $defaultValue)")
        val input = readLine() ?: ""
        return input.ifEmpty { defaultValue }
    }
    
    override fun confirm(message: String): Boolean {
        println("$message (y/n)")
        val response = readLine() ?: ""
        return response.lowercase() in listOf("y", "yes", "true")
    }
    
    override fun select(message: String, options: List<String>): String {
        println(message)
        options.forEachIndexed { index, option ->
            println("${index + 1}. $option")
        }
        val input = readLine() ?: ""
        return try {
            val index = input.toInt() - 1
            if (index in options.indices) options[index] else options.first()
        } catch (e: NumberFormatException) {
            options.firstOrNull { it.equals(input, ignoreCase = true) } ?: options.first()
        }
    }
}