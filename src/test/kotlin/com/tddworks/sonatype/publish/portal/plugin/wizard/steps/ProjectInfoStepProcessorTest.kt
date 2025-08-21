package com.tddworks.sonatype.publish.portal.plugin.wizard.steps

import com.tddworks.sonatype.publish.portal.plugin.wizard.*
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ProjectInfoStepProcessorTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `should use auto-detected values when user confirms`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()

        val detectedInfo = DetectedProjectInfo(
            projectName = "auto-project",
            projectUrl = "https://github.com/auto/project",
            developers = listOf(DetectedDeveloper("Auto Dev", "auto@dev.com"))
        )

        val context = WizardContext(
            project = project,
            detectedInfo = detectedInfo,
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false
        )

        val mockPromptSystem = MockPromptSystem()
        mockPromptSystem.addConfirmResponse(true) // Accept project name
        mockPromptSystem.addConfirmResponse(true) // Accept project URL
        mockPromptSystem.addResponse("Auto project description") // Project description
        mockPromptSystem.addConfirmResponse(true) // Accept developer

        val processor = ProjectInfoStepProcessor()

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.currentStep).isEqualTo(WizardStep.PROJECT_INFO)
        // Should update context with selected values
        assertThat(result.updatedContext).isNotNull()
        val config = result.updatedContext!!.wizardConfig
        assertThat(config.projectInfo.name).isEqualTo("auto-project")
        assertThat(config.projectInfo.url).isEqualTo("https://github.com/auto/project")
        assertThat(config.projectInfo.description).isEqualTo("Auto project description")
    }

    @Test
    fun `should prompt for manual input when user rejects auto-detected values`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()

        val detectedInfo = DetectedProjectInfo(
            projectName = "auto-project",
            projectUrl = "https://github.com/auto/project",
            developers = listOf(DetectedDeveloper("Auto Dev", "auto@dev.com"))
        )

        val context = WizardContext(
            project = project,
            detectedInfo = detectedInfo,
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false
        )

        val mockPromptSystem = MockPromptSystem()
        mockPromptSystem.addConfirmResponse(false) // Reject project name
        mockPromptSystem.addResponse("Manual Project") // Manual project name
        mockPromptSystem.addConfirmResponse(false) // Reject project URL
        mockPromptSystem.addResponse("https://github.com/manual/project") // Manual project URL
        mockPromptSystem.addResponse("A manually configured project") // Description
        mockPromptSystem.addConfirmResponse(false) // Reject developer
        mockPromptSystem.addResponse("manual-dev") // Developer ID
        mockPromptSystem.addResponse("Manual Developer") // Developer name
        mockPromptSystem.addResponse("manual@dev.com") // Developer email

        val processor = ProjectInfoStepProcessor()

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.currentStep).isEqualTo(WizardStep.PROJECT_INFO)
        
        // Should have updated context with manual input
        assertThat(result.updatedContext).isNotNull()
        val updatedWizardConfig = result.updatedContext!!.wizardConfig
        assertThat(updatedWizardConfig.projectInfo.name).isEqualTo("Manual Project")
        assertThat(updatedWizardConfig.projectInfo.description).isEqualTo("A manually configured project")
        assertThat(updatedWizardConfig.projectInfo.url).isEqualTo("https://github.com/manual/project")
        assertThat(updatedWizardConfig.projectInfo.developers).hasSize(1)
        assertThat(updatedWizardConfig.projectInfo.developers.first().name).isEqualTo("Manual Developer")
        assertThat(updatedWizardConfig.projectInfo.developers.first().email).isEqualTo("manual@dev.com")
    }

    @Test
    fun `should validate manual input and show errors for empty required fields`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()

        val context = WizardContext(
            project = project,
            detectedInfo = DetectedProjectInfo("auto-project"),
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false
        )

        val mockPromptSystem = MockPromptSystem()
        mockPromptSystem.addConfirmResponse(false) // Reject project name  
        mockPromptSystem.addResponse("") // Empty project name (will cause validation error)

        val processor = ProjectInfoStepProcessor()

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isFalse()
        assertThat(result.validationErrors).isNotEmpty()
        assertThat(result.validationErrors).contains("Project name is required")
    }

    @Test
    fun `should handle case when no project information is auto-detected`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()

        val context = WizardContext(
            project = project,
            detectedInfo = null, // No auto-detected info
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false
        )

        val mockPromptSystem = MockPromptSystem()
        // Should automatically go to manual input since nothing was detected
        mockPromptSystem.addResponse("Manual Project") // Project name
        mockPromptSystem.addResponse("A manually configured project") // Description
        mockPromptSystem.addResponse("https://github.com/manual/project") // Project URL
        mockPromptSystem.addResponse("Apache License 2.0") // License name
        mockPromptSystem.addResponse("https://www.apache.org/licenses/LICENSE-2.0.txt") // License URL
        mockPromptSystem.addResponse("dev") // Developer ID
        mockPromptSystem.addResponse("Developer") // Developer name
        mockPromptSystem.addResponse("dev@example.com") // Developer email

        val processor = ProjectInfoStepProcessor()

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.updatedContext).isNotNull()
        val updatedWizardConfig = result.updatedContext!!.wizardConfig
        assertThat(updatedWizardConfig.projectInfo.name).isEqualTo("Manual Project")
        assertThat(updatedWizardConfig.projectInfo.url).isEqualTo("https://github.com/manual/project")
    }

    @Test
    fun `should ask about each detected field individually and allow selective acceptance`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()

        val detectedInfo = DetectedProjectInfo(
            projectName = "auto-detected-project",
            projectUrl = "https://github.com/auto/detected",
            developers = listOf(DetectedDeveloper("Auto User", "auto@example.com"))
        )
        
        val context = WizardContext(
            project = project,
            detectedInfo = detectedInfo,
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false
        )
        
        val mockPromptSystem = MockPromptSystem().apply {
            // Accept project name, reject URL, accept developer
            addConfirmResponse(true)  // Accept project name
            addConfirmResponse(false) // Reject project URL 
            addConfirmResponse(true)  // Accept developer
            
            // Manual input for rejected URL
            addResponse("https://github.com/manual/url")
            addResponse("Manual project description")
        }
        
        val processor = ProjectInfoStepProcessor()
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.updatedContext).isNotNull()
        
        val config = result.updatedContext!!.wizardConfig
        assertThat(config.projectInfo.name).isEqualTo("auto-detected-project") // accepted
        assertThat(config.projectInfo.url).isEqualTo("https://github.com/manual/url") // manually entered
        assertThat(config.projectInfo.description).isEqualTo("Manual project description") // manually entered
        assertThat(config.projectInfo.developers.first().name).isEqualTo("Auto User") // accepted
        assertThat(config.projectInfo.developers.first().email).isEqualTo("auto@example.com") // accepted
        
        // Verify prompts asked about each field
        assertThat(mockPromptSystem.allPrompts).contains("Use auto-detected project name 'auto-detected-project'?")
        assertThat(mockPromptSystem.allPrompts).contains("Use auto-detected project URL 'https://github.com/auto/detected'?")
        assertThat(mockPromptSystem.allPrompts).contains("Use auto-detected developer 'Auto User <auto@example.com>'?")
    }
}