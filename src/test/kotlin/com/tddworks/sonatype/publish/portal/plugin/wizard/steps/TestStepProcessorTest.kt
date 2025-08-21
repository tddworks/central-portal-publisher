package com.tddworks.sonatype.publish.portal.plugin.wizard.steps

import com.tddworks.sonatype.publish.portal.plugin.wizard.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestStepProcessorTest {

    private val mockPromptSystem = MockPromptSystem()
    private val processor = TestStepProcessor()

    @Test
    fun `should run basic configuration validation tests`() {
        // Given
        val context = createTestContextWithValidConfig()
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.currentStep).isEqualTo(WizardStep.TEST)
        assertThat(mockPromptSystem.allPrompts).contains("CONFIGURATION TEST")
        assertThat(mockPromptSystem.allPrompts).contains("✅ All tests passed")
    }

    @Test
    fun `should detect missing required configuration`() {
        // Given
        val context = createTestContextWithMissingConfig()
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isFalse()
        assertThat(result.validationErrors).isNotEmpty()
        assertThat(mockPromptSystem.allPrompts).contains("❌ Configuration validation failed")
    }

    @Test
    fun `should validate credentials when not auto-detected`() {
        // Given
        val context = createTestContextWithValidConfig().copy(
            hasAutoDetectedCredentials = false,
            wizardConfig = TestConfigBuilder.createConfig().copy(
                credentials = TestConfigBuilder.createConfig().credentials.copy(
                    username = "",
                    password = ""
                )
            )
        )
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isFalse()
        assertThat(result.validationErrors).contains("Credentials validation failed: username and password are required")
    }

    @Test
    fun `should validate signing configuration when not auto-detected`() {
        // Given
        val context = createTestContextWithValidConfig().copy(
            hasAutoDetectedSigning = false,
            wizardConfig = TestConfigBuilder.createConfig().copy(
                signing = TestConfigBuilder.createConfig().signing.copy(
                    keyId = "",
                    password = ""
                )
            )
        )
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isFalse()
        assertThat(result.validationErrors).contains("Signing validation failed: key ID and password are required")
    }

    @Test
    fun `should skip credential validation when auto-detected`() {
        // Given
        val context = createTestContextWithValidConfig().copy(
            hasAutoDetectedCredentials = true,
            hasAutoDetectedSigning = true
        )
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
        assertThat(mockPromptSystem.allPrompts).contains("⏭️ Credentials: Auto-detected, skipping validation")
        assertThat(mockPromptSystem.allPrompts).contains("⏭️ Signing: Auto-detected, skipping validation")
    }

    private fun createTestContextWithValidConfig() = WizardContext(
        project = TestProjectBuilder.createProject("test-project"),
        detectedInfo = DetectedProjectInfo("test-project", "https://github.com/test/test-project"),
        wizardConfig = TestConfigBuilder.createConfig(),
        enableGlobalGradlePropsDetection = false
    )

    private fun createTestContextWithMissingConfig() = WizardContext(
        project = TestProjectBuilder.createProject("test-project"),
        detectedInfo = DetectedProjectInfo("", ""), // Missing project name
        wizardConfig = TestConfigBuilder.createConfig(),
        enableGlobalGradlePropsDetection = false
    )
}