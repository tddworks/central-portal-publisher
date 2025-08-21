package com.tddworks.sonatype.publish.portal.plugin.wizard

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import uk.org.webcompere.systemstubs.jupiter.SystemStub
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension
import java.io.File
import java.nio.file.Path

@ExtendWith(SystemStubsExtension::class)
class RefactoredSetupWizardTest {

    @SystemStub
    private lateinit var environmentVariables: EnvironmentVariables

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `should follow SOLID principles - Open Closed Principle`() {
        // Given - Create wizard with default processors
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        val wizard = RefactoredSetupWizard(project, enableGlobalGradlePropsDetection = false)
        
        // When - Get processors
        val processors = RefactoredSetupWizard.defaultStepProcessors()
        
        // Then - Should have all expected processors
        assertThat(processors).hasSize(6)
        assertThat(processors.map { it.step }).containsExactly(
            WizardStep.WELCOME,
            WizardStep.PROJECT_INFO,
            WizardStep.CREDENTIALS,
            WizardStep.SIGNING,
            WizardStep.REVIEW,
            WizardStep.TEST
        )
    }

    @Test
    fun `should start wizard and detect project information`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        val wizard = RefactoredSetupWizard(project, enableGlobalGradlePropsDetection = false)
        
        // When
        val result = wizard.start()
        
        // Then
        assertThat(result.currentStep).isEqualTo(WizardStep.WELCOME)
        assertThat(result.detectedInfo.projectName).isEqualTo("test-project")
        assertThat(wizard.currentStep).isEqualTo(WizardStep.WELCOME)
        assertThat(wizard.isComplete).isFalse()
    }

    @Test
    fun `should process individual steps using appropriate processors`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        val mockPromptSystem = MockPromptSystem()
        mockPromptSystem.addConfirmResponse(true) // For project info confirmation
        
        val wizard = RefactoredSetupWizard(
            project = project,
            promptSystem = mockPromptSystem,
            enableGlobalGradlePropsDetection = false
        )
        
        wizard.start()
        
        // When - Process welcome step
        val welcomeResult = wizard.processStep(WizardStep.WELCOME)
        
        // Then
        assertThat(welcomeResult.currentStep).isEqualTo(WizardStep.WELCOME)
        assertThat(welcomeResult.isValid).isTrue()
        
        // When - Process project info step
        val projectInfoResult = wizard.processStep(WizardStep.PROJECT_INFO)
        
        // Then
        assertThat(projectInfoResult.currentStep).isEqualTo(WizardStep.PROJECT_INFO)
        assertThat(projectInfoResult.isValid).isTrue()
    }

    @Test
    fun `should complete full wizard flow with auto-detected credentials`() {
        // Given
        environmentVariables.set("SONATYPE_USERNAME", "test-user")
        environmentVariables.set("SONATYPE_PASSWORD", "test-password")
        environmentVariables.set("SIGNING_KEY", "test-key")
        environmentVariables.set("SIGNING_PASSWORD", "test-key-password")
        
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        val mockPromptSystem = MockPromptSystem()
        // Add responses for the interactive prompts
        mockPromptSystem.addResponse("") // Welcome step (press enter)
        mockPromptSystem.addConfirmResponse(true) // Project info confirmation
        mockPromptSystem.addResponse("") // Credentials step (press enter)
        mockPromptSystem.addResponse("") // Signing step (press enter) 
        mockPromptSystem.addConfirmResponse(true) // Review confirmation
        mockPromptSystem.addResponse("") // Test step (press enter)
        
        val wizard = RefactoredSetupWizard(
            project = project,
            promptSystem = mockPromptSystem,
            enableGlobalGradlePropsDetection = false
        )
        
        // When
        val result = wizard.runComplete()
        
        // Then
        assertThat(result.isComplete).isTrue()
        assertThat(result.stepsCompleted).hasSize(6)
        assertThat(result.filesGenerated).contains("build.gradle.kts")
        assertThat(result.filesGenerated).doesNotContain("gradle.properties") // Auto-detected, so not generated
        assertThat(wizard.isComplete).isTrue()
        
        // Verify files were actually generated
        val buildFile = File(tempDir.toFile(), "build.gradle.kts")
        assertThat(buildFile).exists()
        
        val gradlePropsFile = File(tempDir.toFile(), "gradle.properties")
        assertThat(gradlePropsFile).doesNotExist() // Should not be created when auto-detected
    }

    @Test
    fun `should navigate between steps`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        val wizard = RefactoredSetupWizard(project, enableGlobalGradlePropsDetection = false)
        wizard.start()
        
        // When - Navigate to different step
        wizard.navigateToStep(WizardStep.CREDENTIALS)
        
        // Then
        assertThat(wizard.currentStep).isEqualTo(WizardStep.CREDENTIALS)
        assertThat(wizard.canNavigateBack()).isTrue()
        assertThat(wizard.canNavigateForward()).isTrue()
        
        // When - Navigate to last step
        wizard.navigateToStep(WizardStep.TEST)
        
        // Then
        assertThat(wizard.currentStep).isEqualTo(WizardStep.TEST)
        assertThat(wizard.canNavigateBack()).isTrue()
        assertThat(wizard.canNavigateForward()).isFalse()
    }

    @Test
    fun `should throw exception for unknown step processor`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        // Create wizard with empty processors list
        val wizard = RefactoredSetupWizard(
            project = project,
            stepProcessors = emptyList(),
            enableGlobalGradlePropsDetection = false
        )
        wizard.start()
        
        // When/Then - Should throw exception for missing processor
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            wizard.processStep(WizardStep.WELCOME)
        }
    }

    @Test
    fun `should integrate with auto-detection system for environment variables`() {
        // Given
        environmentVariables.set("SONATYPE_USERNAME", "env-user")
        environmentVariables.set("SONATYPE_PASSWORD", "env-password")
        environmentVariables.set("SIGNING_KEY", "env-key")
        environmentVariables.set("SIGNING_PASSWORD", "env-key-password")
        
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        val mockPromptSystem = MockPromptSystem()
        mockPromptSystem.addResponse("") // Welcome info
        mockPromptSystem.addConfirmResponse(true) // Project info confirmation
        mockPromptSystem.addResponse("") // Credentials auto-detected info  
        mockPromptSystem.addResponse("") // Signing auto-detected info
        mockPromptSystem.addConfirmResponse(true) // Review confirmation
        mockPromptSystem.addResponse("") // Test info
        
        val wizard = RefactoredSetupWizard(
            project = project,
            promptSystem = mockPromptSystem,
            enableGlobalGradlePropsDetection = false
        )
        
        // When
        wizard.start()
        val credentialsResult = wizard.processStep(WizardStep.CREDENTIALS)
        val signingResult = wizard.processStep(WizardStep.SIGNING)
        
        // Then
        assertThat(credentialsResult.isValid).isTrue()
        assertThat(credentialsResult.updatedContext?.hasAutoDetectedCredentials).isTrue()
        assertThat(signingResult.isValid).isTrue()
        assertThat(signingResult.updatedContext?.hasAutoDetectedSigning).isTrue()
    }

    @Test
    fun `should validate input at each step`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        val mockPromptSystem = MockPromptSystem()
        mockPromptSystem.addResponse("") // Empty key response
        
        val wizard = RefactoredSetupWizard(
            project = project,
            promptSystem = mockPromptSystem,
            enableGlobalGradlePropsDetection = false
        )
        
        wizard.start()
        
        // When - Process signing step with invalid input
        val result = wizard.processStep(WizardStep.SIGNING)
        
        // Then
        assertThat(result.isValid).isFalse()
        assertThat(result.validationErrors).contains("Signing key is required")
    }

    @Test
    fun `should generate extended file list including gitignore and CI workflow`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        val mockPromptSystem = MockPromptSystem()
        val extendedFileGenerator = DefaultWizardFileGeneratorWithExtras()
        
        val wizard = RefactoredSetupWizard(
            project = project,
            promptSystem = mockPromptSystem,
            fileGenerator = extendedFileGenerator,
            enableGlobalGradlePropsDetection = false
        )
        
        val detectedInfo = DetectedProjectInfo("test-project")
        val context = WizardContext(
            project = project,
            detectedInfo = detectedInfo,
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false,
            hasAutoDetectedCredentials = true,
            hasAutoDetectedSigning = true
        )
        
        // When
        val generatedFiles = extendedFileGenerator.generateFiles(context, TestConfigBuilder.createConfig())
        
        // Then - Should include additional files
        assertThat(generatedFiles).containsExactly(
            "build.gradle.kts",
            ".gitignore", 
            ".github/workflows/publish.yml"
        )
        
        // Verify files were actually created
        assertThat(File(tempDir.toFile(), ".gitignore")).exists()
        assertThat(File(tempDir.toFile(), ".github/workflows/publish.yml")).exists()
    }
}