package com.tddworks.sonatype.publish.portal.plugin.wizard.steps

import com.tddworks.sonatype.publish.portal.plugin.wizard.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ReviewStepProcessorTest {

    private val mockPromptSystem = MockPromptSystem()
    private val processor = ReviewStepProcessor()

    @Test
    fun `should display configuration summary and allow user to proceed`() {
        // Given
        val context = createTestContextWithFullConfig()
        mockPromptSystem.addConfirmResponse(true) // User confirms
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.currentStep).isEqualTo(WizardStep.REVIEW)
        assertThat(mockPromptSystem.allPrompts).contains("CONFIGURATION REVIEW")
        assertThat(mockPromptSystem.allPrompts).contains("test-project")
    }

    @Test
    fun `should allow user to go back and make changes`() {
        // Given
        val context = createTestContextWithFullConfig()
        mockPromptSystem.addConfirmResponse(false) // User wants to make changes
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isFalse()
        assertThat(result.validationErrors).contains("Please make necessary changes and return to review")
    }

    @Test
    fun `should show auto-detection status in review`() {
        // Given
        val context = createTestContextWithFullConfig().copy(
            hasAutoDetectedCredentials = true,
            hasAutoDetectedSigning = true
        )
        mockPromptSystem.addConfirmResponse(true)
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
        assertThat(mockPromptSystem.allPrompts).contains("✅ Credentials: Auto-detected")
        assertThat(mockPromptSystem.allPrompts).contains("✅ Signing: Auto-detected")
    }

    @Test
    fun `should show manual configuration status in review`() {
        // Given
        val context = createTestContextWithFullConfig().copy(
            hasAutoDetectedCredentials = false,
            hasAutoDetectedSigning = false
        )
        mockPromptSystem.addConfirmResponse(true)
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
        assertThat(mockPromptSystem.allPrompts).contains("⚠️ Credentials: Manual configuration required")
        assertThat(mockPromptSystem.allPrompts).contains("⚠️ Signing: Manual configuration required")
    }

    private fun createTestContextWithFullConfig() = WizardContext(
        project = TestProjectBuilder.createProject("test-project"),
        detectedInfo = DetectedProjectInfo(
            projectName = "test-project",
            projectUrl = "https://github.com/test/test-project",
            developers = listOf(DetectedDeveloper("Test User", "test@example.com"))
        ),
        wizardConfig = TestConfigBuilder.createConfig(),
        enableGlobalGradlePropsDetection = false
    )
}