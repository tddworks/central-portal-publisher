package com.tddworks.sonatype.publish.portal.plugin.wizard.steps

import com.tddworks.sonatype.publish.portal.plugin.wizard.DetectedDeveloper
import com.tddworks.sonatype.publish.portal.plugin.wizard.DetectedProjectInfo
import com.tddworks.sonatype.publish.portal.plugin.wizard.PromptSystem
import com.tddworks.sonatype.publish.portal.plugin.wizard.TestConfigBuilder
import com.tddworks.sonatype.publish.portal.plugin.wizard.TestProjectBuilder
import com.tddworks.sonatype.publish.portal.plugin.wizard.WizardContext
import com.tddworks.sonatype.publish.portal.plugin.wizard.WizardStep
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify
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
        // Verify that display() was called for the review summary (no waiting for input)
        // and confirm() was called for the y/n question (waits for input)
        verify(mockPromptSystem).display(argThat { message ->
            message.contains("📋 CONFIGURATION REVIEW") 
        })
        verify(mockPromptSystem).confirm("Does this configuration look correct? Proceed with setup?")
    }

    @Test
    fun `should show progress indicator in review summary`() {
        // Given
        val context = createTestContextWithFullConfig()
        whenever(mockPromptSystem.confirm("Does this configuration look correct? Proceed with setup?")).thenReturn(true)
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
        verify(mockPromptSystem).display(argThat { message ->
            message.contains("📋 CONFIGURATION REVIEW (Step 5 of 6)") 
        })
    }

    @Test
    fun `should display security status correctly for configured credentials and signing`() {
        // Given
        val context = createTestContextWithFullConfig().copy(
            hasAutoDetectedCredentials = false,
            hasAutoDetectedSigning = false,
            wizardConfig = TestConfigBuilder.createConfig().copy(
                credentials = TestConfigBuilder.createConfig().credentials.copy(
                    username = "test-user",
                    password = "test-pass"
                ),
                signing = TestConfigBuilder.createConfig().signing.copy(
                    keyId = "test-key",
                    password = "test-key-pass"
                )
            )
        )
        whenever(mockPromptSystem.confirm("Does this configuration look correct? Proceed with setup?")).thenReturn(true)
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
        verify(mockPromptSystem).display(argThat { message ->
            message.contains("✅ Credentials: Configured") && message.contains("✅ Signing: Configured")
        })
    }

    @Test
    fun `should display security status correctly for missing credentials and signing`() {
        // Given
        val context = createTestContextWithFullConfig().copy(
            hasAutoDetectedCredentials = false,
            hasAutoDetectedSigning = false,
            wizardConfig = TestConfigBuilder.createConfig().copy(
                credentials = TestConfigBuilder.createConfig().credentials.copy(
                    username = "",
                    password = ""
                ),
                signing = TestConfigBuilder.createConfig().signing.copy(
                    keyId = "",
                    password = ""
                )
            )
        )
        whenever(mockPromptSystem.confirm("Does this configuration look correct? Proceed with setup?")).thenReturn(true)
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
        verify(mockPromptSystem).display(argThat { message ->
            message.contains("⚠️ Credentials: Manual configuration required") && 
            message.contains("⚠️ Signing: Manual configuration required")
        })
    }

    @Test
    fun `should display project information correctly when missing some fields`() {
        // Given
        val context = createTestContextWithFullConfig().copy(
            detectedInfo = DetectedProjectInfo(
                projectName = "test-project",
                projectUrl = "", // Missing URL
                developers = emptyList() // No developers
            ),
            wizardConfig = TestConfigBuilder.createConfig().copy(
                projectInfo = TestConfigBuilder.createConfig().projectInfo.copy(
                    url = "", // Missing URL in config too
                    description = "", // Missing description
                    license = TestConfigBuilder.createConfig().projectInfo.license.copy(
                        name = "" // Missing license
                    )
                )
            )
        )
        whenever(mockPromptSystem.confirm("Does this configuration look correct? Proceed with setup?")).thenReturn(true)
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
        // Verify that display was called with project information
        verify(mockPromptSystem).display(argThat { message ->
            message.contains("📋 CONFIGURATION REVIEW") && 
            message.contains("• Name: test-project")
        })
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