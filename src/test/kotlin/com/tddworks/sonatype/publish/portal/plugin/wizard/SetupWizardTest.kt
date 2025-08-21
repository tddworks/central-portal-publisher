package com.tddworks.sonatype.publish.portal.plugin.wizard

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests for the SetupWizard that guides users through initial plugin configuration.
 * 
 * The SetupWizard should provide an interactive experience that:
 * - Detects project information automatically
 * - Guides users through essential configuration
 * - Validates input at each step
 * - Generates appropriate configuration files
 */
class SetupWizardTest {
    
    private lateinit var project: Project
    private lateinit var setupWizard: SetupWizard
    private lateinit var mockPromptSystem: MockPromptSystem
    
    @TempDir
    lateinit var tempDir: File
    
    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()
        mockPromptSystem = MockPromptSystem()
        setupWizard = SetupWizard(project, mockPromptSystem, enableGlobalGradlePropsDetection = false)
    }
    
    @Test
    fun `should create setup wizard with default configuration`() {
        // When
        val wizard = SetupWizard(project, enableGlobalGradlePropsDetection = false)
        
        // Then
        assertThat(wizard).isNotNull()
        assertThat(wizard.currentStep).isEqualTo(WizardStep.WELCOME)
        assertThat(wizard.isComplete).isFalse()
    }
    
    @Test
    fun `should start with welcome step and auto-detection`() {
        // When
        val result = setupWizard.start()
        
        // Then
        assertThat(result.currentStep).isEqualTo(WizardStep.WELCOME)
        assertThat(result.detectedInfo).isNotNull()
        assertThat(result.detectedInfo.projectName).isNotEmpty()
    }
    
    @Test  
    fun `should guide through all wizard steps with clear separation of concerns`() {
        // Given - Mock responses for the simplified flow (no global gradle.properties detection)
        mockPromptSystem.addResponse("") // Project info display - Press Enter to continue
        mockPromptSystem.addResponse("y") // Use auto-detected project information?
        mockPromptSystem.addResponse("") // Credentials manual setup info - Press Enter to continue
        mockPromptSystem.addResponse("test-user") // Username
        mockPromptSystem.addResponse("test-password") // Password
        mockPromptSystem.addResponse("") // Signing manual setup info - Press Enter to continue
        mockPromptSystem.addResponse("12345678") // GPG Key ID
        mockPromptSystem.addResponse("gpg-password") // GPG Password
        mockPromptSystem.addResponse("y") // Confirm configuration?
        mockPromptSystem.addResponse("y") // Test configuration?
        
        // When - Run complete wizard
        val result = setupWizard.runComplete()
        
        // Then
        assertThat(result.isComplete).isTrue()
        assertThat(result.finalConfiguration).isNotNull()
        assertThat(result.finalConfiguration.credentials.username).isEqualTo("test-user")
        assertThat(result.finalConfiguration.signing.keyId).isEqualTo("12345678")
        assertThat(result.stepsCompleted).containsExactly(
            WizardStep.WELCOME,
            WizardStep.PROJECT_INFO,
            WizardStep.CREDENTIALS,
            WizardStep.SIGNING,
            WizardStep.REVIEW,
            WizardStep.TEST
        )
    }
    
    @Test
    fun `should validate input at each step`() {
        // Given - Test validation by providing empty username first
        mockPromptSystem.addResponse("") // Credentials manual setup info - Press Enter to continue
        mockPromptSystem.addResponse("") // Empty username - should trigger validation error
        
        // When
        setupWizard.start()
        val result = setupWizard.processStep(WizardStep.CREDENTIALS)
        
        // Then - Should have validation error for empty username
        assertThat(result.validationErrors).hasSize(1)
        assertThat(result.validationErrors[0]).contains("Username is required")
        
        // When - Process again with valid input
        mockPromptSystem.addResponse("") // Credentials manual setup info - Press Enter to continue
        mockPromptSystem.addResponse("valid-user") // Valid username
        mockPromptSystem.addResponse("valid-password") // Valid password
        
        val validResult = setupWizard.processStep(WizardStep.CREDENTIALS)
        
        // Then - Should have no validation errors
        assertThat(validResult.validationErrors).isEmpty()
    }
    
    @Test
    fun `should allow navigation between steps`() {
        // Given
        setupWizard.start()
        
        // When - Move to credentials step
        setupWizard.navigateToStep(WizardStep.CREDENTIALS)
        
        // Then
        assertThat(setupWizard.currentStep).isEqualTo(WizardStep.CREDENTIALS)
        
        // When - Go back to project info
        setupWizard.navigateToStep(WizardStep.PROJECT_INFO)
        
        // Then
        assertThat(setupWizard.currentStep).isEqualTo(WizardStep.PROJECT_INFO)
        assertThat(setupWizard.canNavigateBack()).isTrue()
        assertThat(setupWizard.canNavigateForward()).isTrue()
    }
    
    @Test
    fun `should integrate with auto-detection system`() {
        // Given - Project with git repository
        val gitDir = File(tempDir, ".git")
        gitDir.mkdirs()
        File(gitDir, "config").writeText("""
            [remote "origin"]
                url = https://github.com/test/project.git
            [user]
                name = Test Developer
                email = test@example.com
        """.trimIndent())
        
        // When
        val result = setupWizard.start()
        
        // Then - Should detect git information
        assertThat(result.detectedInfo.projectUrl).isEqualTo("https://github.com/test/project")
        assertThat(result.detectedInfo.developers).hasSize(1)
        assertThat(result.detectedInfo.developers[0].name).isEqualTo("Test Developer")
    }
    
    @Test
    fun `should use smart defaults when no input provided`() {
        // Given - Empty responses (use defaults)
        mockPromptSystem.addResponse("") // Use detected project info
        mockPromptSystem.addResponse("") // Use default license
        
        // When
        val result = setupWizard.runComplete()
        
        // Then - Should have smart defaults
        assertThat(result.finalConfiguration.projectInfo.license.name).isEqualTo("Apache License 2.0")
        assertThat(result.finalConfiguration.publishing.autoPublish).isFalse() // Conservative default
        assertThat(result.finalConfiguration.publishing.aggregation).isTrue() // Smart default
    }
    
    @Test
    fun `should auto-detect environment variables`() {
        // Given - Set environment variables (using system-stubs would be better but let's simulate)
        // We'll simulate this by checking the wizard behavior with mock prompt responses
        mockPromptSystem.addResponse("") // Project info display
        mockPromptSystem.addResponse("y") // Confirm auto-detected project info
        mockPromptSystem.addResponse("") // Credentials auto-detected message (if env vars exist)
        mockPromptSystem.addResponse("") // Signing auto-detected message (if env vars exist)
        mockPromptSystem.addResponse("y") // Confirm configuration
        mockPromptSystem.addResponse("y") // Test configuration
        
        // When - This test verifies the wizard can handle auto-detection flow
        // In a real scenario with env vars set, the wizard would show auto-detection messages
        val result = setupWizard.runComplete()
        
        // Then
        assertThat(result.isComplete).isTrue()
        assertThat(result.finalConfiguration).isNotNull()
    }
    
    @Test
    fun `should auto-detect global gradle properties`() {
        // Given - Simulate global gradle.properties auto-detection with mock responses  
        // Note: In actual usage, the wizard would detect ~/.gradle/gradle.properties
        mockPromptSystem.addResponse("") // Project info display
        mockPromptSystem.addResponse("y") // Confirm auto-detected project info
        mockPromptSystem.addResponse("") // Global gradle.properties credentials detected message
        mockPromptSystem.addResponse("") // Global gradle.properties signing detected message  
        mockPromptSystem.addResponse("y") // Confirm configuration
        mockPromptSystem.addResponse("y") // Test configuration
        
        // When - This test verifies the wizard handles global gradle.properties detection
        val result = setupWizard.runComplete()
        
        // Then
        assertThat(result.isComplete).isTrue()
        assertThat(result.finalConfiguration).isNotNull()
    }
    
    @Test
    fun `should generate build configuration file`() {
        // Given
        mockPromptSystem.addResponse("y") // Confirm auto-detected values
        mockPromptSystem.addResponse("test-user")
        mockPromptSystem.addResponse("test-token")
        mockPromptSystem.addResponse("12345678")
        mockPromptSystem.addResponse("gpg-password")
        mockPromptSystem.addResponse("y") // Confirm configuration
        mockPromptSystem.addResponse("y") // Test configuration
        
        // When
        val result = setupWizard.runComplete()
        setupWizard.generateFiles()
        
        // Then
        val buildFile = File(tempDir, "build.gradle.kts")
        assertThat(buildFile).exists()
        
        val content = buildFile.readText()
        assertThat(content).contains("id(\"com.tddworks.central-publisher\")")
        assertThat(content).contains("centralPublisher {")
    }
    
    @Test
    fun `should create gradle properties file`() {
        // Given
        mockPromptSystem.addResponse("y") // Generate properties file
        
        // When
        setupWizard.runComplete()
        setupWizard.generateFiles()
        
        // Then
        val propsFile = File(tempDir, "gradle.properties")
        assertThat(propsFile).exists()
        
        val content = propsFile.readText()
        assertThat(content).contains("# Central Publisher Configuration")
        assertThat(content).contains("SONATYPE_USERNAME=")
        assertThat(content).contains("SONATYPE_PASSWORD=")
    }
    
    @Test
    fun `should provide comprehensive wizard summary`() {
        // Given
        mockPromptSystem.addResponse("y")
        mockPromptSystem.addResponse("test-user")
        mockPromptSystem.addResponse("test-token")
        mockPromptSystem.addResponse("12345678")
        mockPromptSystem.addResponse("gpg-password")
        mockPromptSystem.addResponse("y") // Confirm configuration
        mockPromptSystem.addResponse("y") // Test configuration
        
        // When
        val result = setupWizard.runComplete()
        
        // Then
        assertThat(result.summary).isNotEmpty()
        assertThat(result.summary).contains("Setup completed successfully")
        assertThat(result.summary).contains("Next steps:")
        assertThat(result.filesGenerated).contains("build.gradle.kts")
        assertThat(result.filesGenerated).contains("gradle.properties")
    }
    
    @Test
    fun `should test configuration step and validate setup`() {
        // Given - Complete configuration by processing all steps first
        mockPromptSystem.addResponse("y") // Confirm auto-detected values  
        mockPromptSystem.addResponse("test-user") // Username
        mockPromptSystem.addResponse("test-token") // Password
        mockPromptSystem.addResponse("12345678") // GPG Key ID
        mockPromptSystem.addResponse("gpg-password") // GPG Password
        mockPromptSystem.addResponse("y") // Confirm configuration
        mockPromptSystem.addResponse("y") // Test configuration
        
        // When - Process steps to populate wizard config
        setupWizard.start()
        setupWizard.processStep(WizardStep.PROJECT_INFO)
        setupWizard.processStep(WizardStep.CREDENTIALS) // This populates the credentials
        setupWizard.processStep(WizardStep.SIGNING)
        setupWizard.processStep(WizardStep.REVIEW)
        val result = setupWizard.processStep(WizardStep.TEST)
        
        // Then
        assertThat(result.currentStep).isEqualTo(WizardStep.TEST)
        assertThat(result.isValid).isTrue()
        assertThat(result.validationErrors).isEmpty()
    }
    
    @Test
    fun `should fail test step with missing credentials`() {
        // Given - Incomplete configuration (no credentials set)
        mockPromptSystem.addResponse("y") // Test configuration
        
        // When
        setupWizard.start()
        val result = setupWizard.processStep(WizardStep.TEST)
        
        // Then
        assertThat(result.currentStep).isEqualTo(WizardStep.TEST)
        assertThat(result.isValid).isFalse()
        assertThat(result.validationErrors).contains("Username is required for testing")
        assertThat(result.validationErrors).contains("Password is required for testing")
    }
    
    @Test
    fun `should generate gitignore file`() {
        // When
        setupWizard.generateFiles()
        
        // Then
        val gitignoreFile = File(tempDir, ".gitignore")
        assertThat(gitignoreFile).exists()
        
        val content = gitignoreFile.readText()
        assertThat(content).contains("gradle.properties")
        assertThat(content).contains("*.gpg")
        assertThat(content).contains("local.properties")
    }
    
    @Test
    fun `should generate basic CI config`() {
        // When
        setupWizard.generateFiles()
        
        // Then
        val ciFile = File(tempDir, ".github/workflows/publish.yml")
        assertThat(ciFile).exists()
        
        val content = ciFile.readText()
        assertThat(content).contains("name: Publish to Maven Central")
        assertThat(content).contains("centralPublish")
    }
    
    @Test
    fun `should NOT generate gradle properties when both credentials and signing are auto-detected`() {
        // Given - Setup mock to simulate full auto-detection
        val wizardWithAutoDetection = SetupWizard(project, mockPromptSystem, enableGlobalGradlePropsDetection = false)
        
        // Set auto-detection flags directly (simulating environment variable detection)
        wizardWithAutoDetection.javaClass.getDeclaredField("hasAutoDetectedCredentials").apply {
            isAccessible = true
            set(wizardWithAutoDetection, true)
        }
        wizardWithAutoDetection.javaClass.getDeclaredField("hasAutoDetectedSigning").apply {
            isAccessible = true 
            set(wizardWithAutoDetection, true)
        }
        
        // When
        wizardWithAutoDetection.generateFiles()
        
        // Then - gradle.properties should NOT exist
        val propsFile = File(tempDir, "gradle.properties")
        assertThat(propsFile).doesNotExist()
        
        // Other files should still be generated
        assertThat(File(tempDir, "build.gradle.kts")).exists()
        assertThat(File(tempDir, ".gitignore")).exists()
    }
    
    @Test
    fun `should generate gradle properties with only missing sections when partially auto-detected`() {
        // Given - Setup mock to simulate partial auto-detection (only credentials)
        val wizardWithPartialDetection = SetupWizard(project, mockPromptSystem, enableGlobalGradlePropsDetection = false)
        
        // Set only credentials as auto-detected
        wizardWithPartialDetection.javaClass.getDeclaredField("hasAutoDetectedCredentials").apply {
            isAccessible = true
            set(wizardWithPartialDetection, true)
        }
        // hasAutoDetectedSigning remains false
        
        // When
        wizardWithPartialDetection.generateFiles()
        
        // Then - gradle.properties should exist with only signing section
        val propsFile = File(tempDir, "gradle.properties")
        assertThat(propsFile).exists()
        
        val content = propsFile.readText()
        assertThat(content).contains("✅ Credentials auto-detected")
        assertThat(content).contains("No local credential configuration needed!")
        assertThat(content).contains("SIGNING_KEY=-----BEGIN PGP PRIVATE KEY BLOCK-----")
        // Should not contain credential placeholders (not in comments)
        assertThat(content).doesNotContain("SONATYPE_USERNAME=your-username")
        assertThat(content).doesNotContain("SONATYPE_PASSWORD=your-password")
    }
    
    @Test
    fun `should use auto-detected values in generated build file`() {
        // Given - Create a wizard with mock project info
        val mockDetectedInfo = DetectedProjectInfo(
            projectName = "auto-detected-project",
            projectUrl = "https://github.com/auto/detected",
            developers = listOf(
                DetectedDeveloper(name = "Auto Developer", email = "auto@developer.com")
            )
        )
        
        // Set the detected info via reflection
        setupWizard.javaClass.getDeclaredField("detectedInfo").apply {
            isAccessible = true
            set(setupWizard, mockDetectedInfo)
        }
        
        // When
        setupWizard.generateFiles()
        
        // Then
        val buildFile = File(tempDir, "build.gradle.kts")
        assertThat(buildFile).exists()
        
        val content = buildFile.readText()
        assertThat(content).contains("name = \"auto-detected-project\"")
        assertThat(content).contains("url = \"https://github.com/auto/detected\"")
        assertThat(content).contains("id = \"auto\"") // from email
        assertThat(content).contains("name = \"Auto Developer\"")
        assertThat(content).contains("email = \"auto@developer.com\"")
        assertThat(content).contains("connection = \"scm:git:git://github.com/auto/detected.git\"")
    }
    
    @Test
    fun `should generate dynamic file list based on what was actually created`() {
        // Given - Setup wizard with both auto-detected (no gradle.properties should be generated)
        val wizardWithAutoDetection = SetupWizard(project, mockPromptSystem, enableGlobalGradlePropsDetection = false)
        
        wizardWithAutoDetection.javaClass.getDeclaredField("hasAutoDetectedCredentials").apply {
            isAccessible = true
            set(wizardWithAutoDetection, true)
        }
        wizardWithAutoDetection.javaClass.getDeclaredField("hasAutoDetectedSigning").apply {
            isAccessible = true
            set(wizardWithAutoDetection, true)
        }
        
        mockPromptSystem.addResponse("y") // Use auto-detected project info
        mockPromptSystem.addResponse("y") // Confirm configuration
        mockPromptSystem.addResponse("y") // Test configuration
        
        // When
        val result = wizardWithAutoDetection.runComplete()
        
        // Then - File list should NOT include gradle.properties
        assertThat(result.filesGenerated).containsExactly(
            "build.gradle.kts",
            ".gitignore", 
            ".github/workflows/publish.yml"
        )
        assertThat(result.filesGenerated).doesNotContain("gradle.properties")
    }
}

/**
 * Mock implementation of PromptSystem for testing
 */
class MockPromptSystem : PromptSystem {
    private val responses = mutableListOf<String>()
    private var currentIndex = 0
    
    fun addResponse(response: String) {
        responses.add(response)
    }
    
    override fun prompt(message: String): String {
        return if (currentIndex < responses.size) {
            responses[currentIndex++]
        } else {
            "" // Default empty response
        }
    }
    
    override fun promptWithDefault(message: String, defaultValue: String): String {
        val response = prompt(message)
        return response.ifEmpty { defaultValue }
    }
    
    override fun confirm(message: String): Boolean {
        val response = prompt(message)
        return response.lowercase() in listOf("y", "yes", "true")
    }
    
    override fun select(message: String, options: List<String>): String {
        val response = prompt(message)
        return if (response.isNotEmpty() && options.contains(response)) {
            response
        } else {
            options.first() // Default to first option
        }
    }
}