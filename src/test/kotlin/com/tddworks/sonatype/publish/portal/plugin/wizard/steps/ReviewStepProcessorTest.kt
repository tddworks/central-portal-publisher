package com.tddworks.sonatype.publish.portal.plugin.wizard.steps

import com.tddworks.sonatype.publish.portal.plugin.wizard.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ReviewStepProcessorTest {

    @Mock
    private lateinit var mockPromptSystem: PromptSystem
    
    private val processor = ReviewStepProcessor()

    @Test
    fun `should display configuration summary and allow user to proceed`() {
        // Given
        val context = createTestContextWithFullConfig()
        whenever(mockPromptSystem.confirm("Does this configuration look correct? Proceed with setup?")).thenReturn(true)
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.currentStep).isEqualTo(WizardStep.REVIEW)
    }

    @Test
    fun `should allow user to go back and make changes`() {
        // Given
        val context = createTestContextWithFullConfig()
        whenever(mockPromptSystem.confirm("Does this configuration look correct? Proceed with setup?")).thenReturn(false)
        
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
        whenever(mockPromptSystem.confirm("Does this configuration look correct? Proceed with setup?")).thenReturn(true)
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
    }

    @Test
    fun `should show manual configuration status in review`() {
        // Given
        val context = createTestContextWithFullConfig().copy(
            hasAutoDetectedCredentials = false,
            hasAutoDetectedSigning = false
        )
        whenever(mockPromptSystem.confirm("Does this configuration look correct? Proceed with setup?")).thenReturn(true)
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
    }

    @Test
    fun `should handle non-interactive prompt system that auto-confirms`() {
        // Given
        val context = createTestContextWithFullConfig()
        whenever(mockPromptSystem.confirm("Does this configuration look correct? Proceed with setup?")).thenReturn(true)
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.currentStep).isEqualTo(WizardStep.REVIEW)
    }

    @Test
    fun `should display review summary without waiting for input before confirmation`() {
        // Given
        val context = createTestContextWithFullConfig()
        whenever(mockPromptSystem.confirm("Does this configuration look correct? Proceed with setup?")).thenReturn(true)
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
        // Verify that only confirm() was called (for the y/n question)
        // The review summary is printed directly via println() so no prompt() call
        org.mockito.kotlin.verify(mockPromptSystem, org.mockito.kotlin.never()).prompt(org.mockito.kotlin.any())
        org.mockito.kotlin.verify(mockPromptSystem).confirm("Does this configuration look correct? Proceed with setup?")
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