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
        // Should use display() method to show test results without waiting for input
        assertThat(mockPromptSystem.displayMessages).isNotEmpty()
        val displayedMessage = mockPromptSystem.displayMessages.joinToString("\n")
        assertThat(displayedMessage).contains("🧪 CONFIGURATION TEST")
        assertThat(displayedMessage).contains("✅ All tests passed")
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
        // Should use display() method to show test results
        assertThat(mockPromptSystem.displayMessages).isNotEmpty()
        val displayedMessage = mockPromptSystem.displayMessages.joinToString("\n")
        assertThat(displayedMessage).contains("🧪 CONFIGURATION TEST")
        assertThat(displayedMessage).contains("❌ Configuration validation failed")
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
        // Should use display() method to show test results
        assertThat(mockPromptSystem.displayMessages).isNotEmpty()
        val displayedMessage = mockPromptSystem.displayMessages.joinToString("\n")
        assertThat(displayedMessage).contains("⏭️ Credentials: Auto-detected, skipping validation")
        assertThat(displayedMessage).contains("⏭️ Signing: Auto-detected, skipping validation")
    }

    @Test
    fun `should show progress indicator in test results`() {
        // Given
        val context = createTestContextWithValidConfig()
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
        val displayedMessage = mockPromptSystem.displayMessages.joinToString("\n")
        assertThat(displayedMessage).contains("🧪 CONFIGURATION TEST (Step 6 of 6)")
    }

    @Test
    fun `should handle mixed validation failures`() {
        // Given
        val context = createTestContextWithValidConfig().copy(
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
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isFalse()
        assertThat(result.validationErrors).hasSize(2)
        assertThat(result.validationErrors).contains("Credentials validation failed: username and password are required")
        assertThat(result.validationErrors).contains("Signing validation failed: key ID and password are required")
    }

    @Test
    fun `should fail only credentials when signing is valid`() {
        // Given
        val context = createTestContextWithValidConfig().copy(
            hasAutoDetectedCredentials = false,
            hasAutoDetectedSigning = false,
            wizardConfig = TestConfigBuilder.createConfig().copy(
                credentials = TestConfigBuilder.createConfig().credentials.copy(
                    username = "",
                    password = ""
                )
                // signing config remains valid from TestConfigBuilder
            )
        )
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isFalse()
        assertThat(result.validationErrors).hasSize(1)
        assertThat(result.validationErrors).contains("Credentials validation failed: username and password are required")
        val displayedMessage = mockPromptSystem.displayMessages.joinToString("\n")
        assertThat(displayedMessage).contains("❌ Credentials validation: FAILED")
        assertThat(displayedMessage).contains("✅ Signing validation: PASSED")
    }

    @Test
    fun `should fail only signing when credentials are valid`() {
        // Given
        val context = createTestContextWithValidConfig().copy(
            hasAutoDetectedCredentials = false,
            hasAutoDetectedSigning = false,
            wizardConfig = TestConfigBuilder.createConfig().copy(
                signing = TestConfigBuilder.createConfig().signing.copy(
                    keyId = "",
                    password = ""
                )
                // credentials config remains valid from TestConfigBuilder
            )
        )
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isFalse()
        assertThat(result.validationErrors).hasSize(1)
        assertThat(result.validationErrors).contains("Signing validation failed: key ID and password are required")
        val displayedMessage = mockPromptSystem.displayMessages.joinToString("\n")
        assertThat(displayedMessage).contains("✅ Credentials validation: PASSED")
        assertThat(displayedMessage).contains("❌ Signing validation: FAILED")
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