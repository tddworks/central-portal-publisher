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
        mockPromptSystem.addResponse("") // Press enter to continue
        mockPromptSystem.addConfirmResponse(true) // Confirm auto-detected values

        val processor = ProjectInfoStepProcessor()

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.currentStep).isEqualTo(WizardStep.PROJECT_INFO)
        // Should not update context since we're using auto-detected values
        assertThat(result.updatedContext).isNull()
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
        mockPromptSystem.addResponse("") // Press enter to continue
        mockPromptSystem.addConfirmResponse(false) // Reject auto-detected values
        
        // Manual input responses
        mockPromptSystem.addResponse("Manual Project") // Project name
        mockPromptSystem.addResponse("A manually configured project") // Description
        mockPromptSystem.addResponse("https://github.com/manual/project") // Project URL
        mockPromptSystem.addResponse("MIT License") // License name
        mockPromptSystem.addResponse("https://opensource.org/licenses/MIT") // License URL
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
        assertThat(updatedWizardConfig.projectInfo.license.name).isEqualTo("MIT License")
        assertThat(updatedWizardConfig.projectInfo.license.url).isEqualTo("https://opensource.org/licenses/MIT")
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
        mockPromptSystem.addResponse("") // Press enter to continue
        mockPromptSystem.addConfirmResponse(false) // Reject auto-detected values
        
        // Empty manual input (invalid)
        mockPromptSystem.addResponse("") // Empty project name
        mockPromptSystem.addResponse("") // Empty description
        mockPromptSystem.addResponse("") // Empty project URL
        mockPromptSystem.addResponse("") // Empty license name
        mockPromptSystem.addResponse("") // Empty license URL
        mockPromptSystem.addResponse("") // Empty developer ID
        mockPromptSystem.addResponse("") // Empty developer name
        mockPromptSystem.addResponse("") // Empty developer email

        val processor = ProjectInfoStepProcessor()

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isFalse()
        assertThat(result.validationErrors).isNotEmpty()
        assertThat(result.validationErrors).contains("Project name is required")
        assertThat(result.validationErrors).contains("Project URL is required")
        assertThat(result.validationErrors).contains("Developer name is required")
        assertThat(result.validationErrors).contains("Developer email is required")
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
}