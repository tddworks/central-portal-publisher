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
    private val promptSystem: PromptSystem = ConsolePromptSystem(),
    private val enableGlobalGradlePropsDetection: Boolean = true
) {
    
    private var _currentStep = WizardStep.WELCOME
    private var _isComplete = false
    private val completedSteps = mutableListOf<WizardStep>()
    private var wizardConfig = CentralPublisherConfigBuilder().build()
    private var detectedInfo: DetectedProjectInfo? = null
    private var hasAutoDetectedCredentials = false
    private var hasAutoDetectedSigning = false
    
    val currentStep: WizardStep get() = _currentStep
    val isComplete: Boolean get() = _isComplete
    
    /**
     * Create wizard with default console prompt system
     */
    constructor(project: Project) : this(project, ConsolePromptSystem(), true)
    
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
        
        // Actually generate the files
        generateFiles()
        
        // Determine which files were actually generated
        val actualFilesGenerated = mutableListOf<String>()
        actualFilesGenerated.add("build.gradle.kts")
        if (!(hasAutoDetectedCredentials && hasAutoDetectedSigning)) {
            actualFilesGenerated.add("gradle.properties")
        }
        actualFilesGenerated.add(".gitignore")
        actualFilesGenerated.add(".github/workflows/publish.yml")
        
        return WizardCompletionResult(
            isComplete = true,
            finalConfiguration = finalConfig,
            stepsCompleted = completedSteps.toList(),
            summary = generateSummary(finalConfig),
            filesGenerated = actualFilesGenerated.toList()
        )
    }
    
    /**
     * Process a specific wizard step
     */
    fun processStep(step: WizardStep): WizardStepResult {
        _currentStep = step
        val validationErrors = mutableListOf<String>()
        
        when (step) {
            WizardStep.WELCOME -> processWelcomeStep(validationErrors)
            WizardStep.PROJECT_INFO -> processProjectInfoStep(validationErrors)
            WizardStep.CREDENTIALS -> processCredentialsStep(validationErrors)
            WizardStep.SIGNING -> processSigningStep(validationErrors)
            WizardStep.REVIEW -> processReviewStep(validationErrors)
            WizardStep.TEST -> processTestStep(validationErrors)
        }
        
        return WizardStepResult(
            currentStep = step,
            isValid = validationErrors.isEmpty(),
            validationErrors = validationErrors
        )
    }
    
    private fun processWelcomeStep(validationErrors: MutableList<String>) {
        // Welcome step - just show detected info
        // No validation needed
    }
    
    private fun processProjectInfoStep(validationErrors: MutableList<String>) {
        // Show what was detected first
        promptSystem.prompt("""
            Auto-detected project information:
            • Project: ${detectedInfo?.projectName ?: "not detected"}
            • URL: ${detectedInfo?.projectUrl ?: "not detected"}
            • Developers: ${detectedInfo?.developers?.map { "${it.name} <${it.email}>" }?.joinToString() ?: "not detected"}
            
            Press Enter to continue...
        """.trimIndent())
        
        val confirmed = promptSystem.confirm("Use this auto-detected project information?")
        if (!confirmed) {
            // Could prompt for manual input here in future
            promptSystem.prompt("Manual project setup not yet implemented. Using auto-detected values...")
        }
    }
    
    private fun processCredentialsStep(validationErrors: MutableList<String>) {
        // Check for existing environment variables
        val envUsername = System.getenv("SONATYPE_USERNAME")
        val envPassword = System.getenv("SONATYPE_PASSWORD")
        val hasEnvCredentials = !envUsername.isNullOrBlank() && !envPassword.isNullOrBlank()
        
        // Check for existing global gradle.properties (only if enabled)
        val globalGradleProps = if (enableGlobalGradlePropsDetection) {
            File(System.getProperty("user.home"), ".gradle/gradle.properties")
        } else null
        val globalUsername = if (globalGradleProps?.exists() == true) {
            globalGradleProps.readLines().find { it.startsWith("SONATYPE_USERNAME=") }?.substringAfter("=")?.trim()
        } else null
        val globalPassword = if (globalGradleProps?.exists() == true) {
            globalGradleProps.readLines().find { it.startsWith("SONATYPE_PASSWORD=") }?.substringAfter("=")?.trim()
        } else null
        val hasGlobalCredentials = !globalUsername.isNullOrBlank() && !globalPassword.isNullOrBlank()
        
        if (hasEnvCredentials) {
            promptSystem.prompt("""
                📋 CREDENTIALS SETUP - AUTO-DETECTED!
                ✅ Found existing environment variables:
                • SONATYPE_USERNAME: ${envUsername}
                • SONATYPE_PASSWORD: ${"*".repeat(envPassword!!.length.coerceAtMost(8))}
                
                Using these existing credentials (environment variables take precedence).
                
                Press Enter to continue...
            """.trimIndent())
            
            // Use detected credentials
            wizardConfig = wizardConfig.copy(
                credentials = wizardConfig.credentials.copy(
                    username = envUsername!!,
                    password = envPassword
                )
            )
            hasAutoDetectedCredentials = true
        } else if (hasGlobalCredentials) {
            promptSystem.prompt("""
                📋 CREDENTIALS SETUP - AUTO-DETECTED!
                ✅ Found existing global gradle.properties (~/.gradle/gradle.properties):
                • SONATYPE_USERNAME: ${globalUsername}
                • SONATYPE_PASSWORD: ${"*".repeat(globalPassword!!.length.coerceAtMost(8))}
                
                Using these existing credentials from global gradle.properties.
                
                Press Enter to continue...
            """.trimIndent())
            
            // Use detected credentials
            wizardConfig = wizardConfig.copy(
                credentials = wizardConfig.credentials.copy(
                    username = globalUsername!!,
                    password = globalPassword
                )
            )
            hasAutoDetectedCredentials = true
        } else {
            // Show configuration options
            promptSystem.prompt("""
                📋 CREDENTIALS SETUP
                No environment variables or global gradle.properties credentials detected. Manual configuration needed.
                
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
            } else {
                // Update wizard config with username
                wizardConfig = wizardConfig.copy(
                    credentials = wizardConfig.credentials.copy(username = username)
                )
                
                val password = promptSystem.prompt("Enter your Sonatype password/token:")
                wizardConfig = wizardConfig.copy(
                    credentials = wizardConfig.credentials.copy(password = password)
                )
            }
        }
    }
    
    private fun processSigningStep(validationErrors: MutableList<String>) {
        // Check for existing environment variables
        val envSigningKey = System.getenv("SIGNING_KEY")
        val envSigningPassword = System.getenv("SIGNING_PASSWORD")
        val hasEnvSigning = !envSigningKey.isNullOrBlank() && !envSigningPassword.isNullOrBlank()
        
        // Check for existing global gradle.properties (only if enabled)
        val globalGradleProps = if (enableGlobalGradlePropsDetection) {
            File(System.getProperty("user.home"), ".gradle/gradle.properties")
        } else null
        val globalSigningKey = if (globalGradleProps?.exists() == true) {
            globalGradleProps.readLines().find { it.startsWith("SIGNING_KEY=") }?.substringAfter("=")?.trim()
        } else null
        val globalSigningPassword = if (globalGradleProps?.exists() == true) {
            globalGradleProps.readLines().find { it.startsWith("SIGNING_PASSWORD=") }?.substringAfter("=")?.trim()
        } else null
        val hasGlobalSigning = !globalSigningKey.isNullOrBlank() && !globalSigningPassword.isNullOrBlank()
        
        if (hasEnvSigning) {
            promptSystem.prompt("""
                🔐 GPG SIGNING SETUP - AUTO-DETECTED!
                ✅ Found existing environment variables:
                • SIGNING_KEY: ${if (envSigningKey!!.contains("BEGIN PGP")) "PGP private key found" else envSigningKey.take(20) + "..."}
                • SIGNING_PASSWORD: ${"*".repeat(envSigningPassword!!.length.coerceAtMost(8))}
                
                Using these existing signing credentials (environment variables take precedence).
                
                Press Enter to continue...
            """.trimIndent())
            
            // Use detected signing config - extract key ID if possible
            val keyId = if (envSigningKey.contains("BEGIN PGP")) "detected-from-env" else envSigningKey
            wizardConfig = wizardConfig.copy(
                signing = wizardConfig.signing.copy(
                    keyId = keyId,
                    password = envSigningPassword
                )
            )
            hasAutoDetectedSigning = true
        } else if (hasGlobalSigning) {
            promptSystem.prompt("""
                🔐 GPG SIGNING SETUP - AUTO-DETECTED!
                ✅ Found existing global gradle.properties (~/.gradle/gradle.properties):
                • SIGNING_KEY: ${if (globalSigningKey!!.contains("BEGIN PGP")) "PGP private key found" else globalSigningKey.take(20) + "..."}
                • SIGNING_PASSWORD: ${"*".repeat(globalSigningPassword!!.length.coerceAtMost(8))}
                
                Using these existing signing credentials from global gradle.properties.
                
                Press Enter to continue...
            """.trimIndent())
            
            // Use detected signing config
            val keyId = if (globalSigningKey.contains("BEGIN PGP")) "detected-from-global" else globalSigningKey
            wizardConfig = wizardConfig.copy(
                signing = wizardConfig.signing.copy(
                    keyId = keyId,
                    password = globalSigningPassword
                )
            )
            hasAutoDetectedSigning = true
        } else {
            // Show configuration options
            promptSystem.prompt("""
                🔐 GPG SIGNING SETUP
                Maven Central requires all artifacts to be cryptographically signed.
                No environment variables or global gradle.properties signing detected. Manual configuration needed.
                
                Configuration options (in order of preference):
                1. Environment variables (recommended for CI/CD):
                   export SIGNING_KEY="-----BEGIN PGP PRIVATE KEY BLOCK-----..."
                   export SIGNING_PASSWORD=your-gpg-password
                
                2. Global gradle.properties (~/.gradle/gradle.properties):
                   SIGNING_KEY=-----BEGIN PGP PRIVATE KEY BLOCK-----...
                   SIGNING_PASSWORD=your-gpg-password
                
                3. Local gradle.properties (this project only - not recommended):
                   Will be generated for you but should not be committed to git
                
                Note: You can generate GPG keys with: gpg --gen-key
                
                Press Enter to continue...
            """.trimIndent())
            
            val keyId = promptSystem.prompt("Enter your GPG Key ID (e.g. 1234567890ABCDEF):")
            wizardConfig = wizardConfig.copy(
                signing = wizardConfig.signing.copy(keyId = keyId)
            )
            
            val gpgPassword = promptSystem.prompt("Enter your GPG key password:")
            wizardConfig = wizardConfig.copy(
                signing = wizardConfig.signing.copy(password = gpgPassword)
            )
        }
    }
    
    private fun processReviewStep(validationErrors: MutableList<String>) {
        // Review step - confirm configuration
        val confirmed = promptSystem.confirm("Confirm configuration?")
        if (!confirmed) {
            validationErrors.add("Configuration not confirmed")
        }
    }
    
    private fun processTestStep(validationErrors: MutableList<String>) {
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
            
            if (hasAutoDetectedCredentials || hasAutoDetectedSigning) {
                appendLine()
                appendLine("Auto-detection Results:")
                if (hasAutoDetectedCredentials) {
                    appendLine("✅ Credentials: Auto-detected from environment/global gradle.properties")
                }
                if (hasAutoDetectedSigning) {
                    appendLine("✅ Signing: Auto-detected from environment/global gradle.properties")
                }
            }
            
            appendLine()
            appendLine("Next steps:")
            appendLine("1. Review generated build.gradle.kts")
            
            if (!hasAutoDetectedCredentials) {
                appendLine("2. Configure credentials (recommended: environment variables):")
                appendLine("   - Environment: export SONATYPE_USERNAME=... SONATYPE_PASSWORD=...")
                appendLine("   - Global gradle.properties: ~/.gradle/gradle.properties")
                appendLine("   - Local gradle.properties: update generated file (not recommended)")
            }
            
            if (!hasAutoDetectedSigning) {
                val stepNum = if (hasAutoDetectedCredentials) "2" else "3"
                appendLine("$stepNum. Configure GPG signing:")
                appendLine("   - Environment: export SIGNING_KEY=... SIGNING_PASSWORD=...")
                appendLine("   - Global gradle.properties: ~/.gradle/gradle.properties")
                appendLine("   - Local gradle.properties: update generated file (not recommended)")
            }
            
            val finalStepNum = when {
                hasAutoDetectedCredentials && hasAutoDetectedSigning -> "2"
                hasAutoDetectedCredentials || hasAutoDetectedSigning -> "3"
                else -> "4"
            }
            
            appendLine("$finalStepNum. Run './gradlew publishToCentral' to publish")
        }
    }
    
    private fun generateBuildFile() {
        val buildFile = File(project.projectDir, "build.gradle.kts")
        
        // Use auto-detected information or fallback to defaults
        val projectUrl = detectedInfo?.projectUrl ?: "https://github.com/yourorg/${project.name}"
        val firstDeveloper = detectedInfo?.developers?.firstOrNull()
        val developerId = firstDeveloper?.email?.substringBefore("@") ?: "yourid"
        val developerName = firstDeveloper?.name ?: "Your Name"
        val developerEmail = firstDeveloper?.email ?: "your.email@example.com"
        
        val content = buildString {
            appendLine("plugins {")
            appendLine("    id(\"com.tddworks.central-publisher\")")
            appendLine("}")
            appendLine()
            appendLine("centralPublisher {")
            appendLine("    credentials {")
            appendLine("        username = project.findProperty(\"SONATYPE_USERNAME\")?.toString() ?: \"\"")
            appendLine("        password = project.findProperty(\"SONATYPE_PASSWORD\")?.toString() ?: \"\"")
            appendLine("    }")
            appendLine("    ")
            appendLine("    projectInfo {")
            appendLine("        name = \"${detectedInfo?.projectName ?: project.name}\"")
            appendLine("        description = \"Description of your project\"")
            appendLine("        url = \"$projectUrl\"")
            appendLine("        ")
            appendLine("        license {")
            appendLine("            name = \"Apache License 2.0\"")
            appendLine("            url = \"https://www.apache.org/licenses/LICENSE-2.0.txt\"")
            appendLine("        }")
            appendLine("        ")
            appendLine("        developer {")
            appendLine("            id = \"$developerId\"")
            appendLine("            name = \"$developerName\"")
            appendLine("            email = \"$developerEmail\"")
            appendLine("        }")
            appendLine("        ")
            appendLine("        scm {")
            appendLine("            url = \"$projectUrl\"")
            appendLine("            connection = \"scm:git:git://${projectUrl.removePrefix("https://")}.git\"")
            appendLine("            developerConnection = \"scm:git:ssh://${projectUrl.removePrefix("https://")}.git\"")
            appendLine("        }")
            appendLine("    }")
            appendLine("}")
        }
        buildFile.writeText(content)
    }
    
    private fun generatePropertiesFile() {
        // Only generate gradle.properties if neither credentials nor signing were auto-detected
        if (hasAutoDetectedCredentials && hasAutoDetectedSigning) {
            // Both were auto-detected, no need for local gradle.properties
            return
        }
        
        val propsFile = File(project.projectDir, "gradle.properties")
        val existingContent = if (propsFile.exists()) propsFile.readText() else ""
        
        val content = buildString {
            appendLine("# Central Publisher Configuration")
            appendLine("# WARNING: This file should NOT be committed to git!")
            appendLine("# For better security, consider using:")
            appendLine("# 1. Environment variables: export SONATYPE_USERNAME=...")
            appendLine("# 2. Global gradle.properties: ~/.gradle/gradle.properties")
            appendLine()
            
            // Only add credential placeholders if not auto-detected
            if (!hasAutoDetectedCredentials) {
                appendLine("# Credentials Configuration")
                appendLine("SONATYPE_USERNAME=your-username")
                appendLine("SONATYPE_PASSWORD=your-password")
                appendLine()
            } else {
                appendLine("# ✅ Credentials auto-detected from environment/global gradle.properties")
                appendLine("# No local credential configuration needed!")
                appendLine()
            }
            
            // Only add signing placeholders if not auto-detected
            if (!hasAutoDetectedSigning) {
                appendLine("# Signing Configuration")
                appendLine("# For better security, consider using environment variables:")
                appendLine("# export SIGNING_KEY=\"-----BEGIN PGP PRIVATE KEY BLOCK-----...\"")
                appendLine("# export SIGNING_PASSWORD=your-gpg-password")
                appendLine()
                appendLine("SIGNING_KEY=-----BEGIN PGP PRIVATE KEY BLOCK-----")
                appendLine("# ... your full PGP private key here ...")
                appendLine("# -----END PGP PRIVATE KEY BLOCK-----")
                appendLine("SIGNING_PASSWORD=your-gpg-password")
            } else {
                appendLine("# ✅ Signing configuration auto-detected from environment/global gradle.properties")
                appendLine("# No local signing configuration needed!")
            }
            
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
    
    override fun display(message: String) {
        println(message)
    }
}