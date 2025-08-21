package com.tddworks.sonatype.publish.portal.plugin.wizard

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import uk.org.webcompere.systemstubs.jupiter.SystemStub
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension
import java.io.File
import java.nio.file.Path
import kotlin.collections.set
import kotlin.text.contains


@ExtendWith(SystemStubsExtension::class, MockitoExtension::class)
class RefactoredSetupWizardTest {

    @SystemStub
    private lateinit var environmentVariables: EnvironmentVariables

    @Mock
    private lateinit var mockPromptSystem: PromptSystem

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

        // Mock confirm method to return true for project info confirmation
        `when`(mockPromptSystem.confirm(anyString())).thenReturn(true)
        `when`(mockPromptSystem.prompt(anyString())).thenReturn("test-description")

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

        // Mock all necessary interactions for ALL steps including PROJECT_INFO
        // PROJECT_INFO step needs both confirm() and prompt() calls
        `when`(mockPromptSystem.confirm(anyString())).thenReturn(true)
        `when`(mockPromptSystem.prompt(anyString())).thenReturn("test-description")

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
        assertThat(result.stepsCompleted).containsExactly(
            WizardStep.WELCOME,
            WizardStep.PROJECT_INFO,
            WizardStep.CREDENTIALS,
            WizardStep.SIGNING,
            WizardStep.REVIEW,
            WizardStep.TEST
        )
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

        // Mock user saying yes to auto-detected credentials and signing
        `when`(mockPromptSystem.confirm(anyString())).thenReturn(true)

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

        // Mock empty key response
        `when`(mockPromptSystem.prompt(anyString())).thenReturn("")

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