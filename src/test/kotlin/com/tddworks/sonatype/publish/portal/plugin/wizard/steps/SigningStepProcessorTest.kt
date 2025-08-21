package com.tddworks.sonatype.publish.portal.plugin.wizard.steps

import com.tddworks.sonatype.publish.portal.plugin.wizard.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import uk.org.webcompere.systemstubs.jupiter.SystemStub
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension
import java.io.File
import java.nio.file.Files

@ExtendWith(SystemStubsExtension::class)
class SigningStepProcessorTest {

    @SystemStub
    private lateinit var environmentVariables: EnvironmentVariables

    private val mockPromptSystem = MockPromptSystem()
    private val processor = SigningStepProcessor()

    @Test
    fun `should auto-detect signing from environment variables`() {
        // Given
        environmentVariables.set("SIGNING_KEY", "test-key")
        environmentVariables.set("SIGNING_PASSWORD", "test-password")
        
        val context = createTestContext()
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.updatedContext?.hasAutoDetectedSigning).isTrue()
        assertThat(mockPromptSystem.allPrompts).contains("AUTO-DETECTED")
        assertThat(mockPromptSystem.allPrompts).contains("environment variables")
    }

    @Test
    fun `should auto-detect signing from global gradle properties`() {
        // Given
        val tempHomeDir = Files.createTempDirectory("user-home").toFile()
        val gradleDir = File(tempHomeDir, ".gradle")
        gradleDir.mkdirs()
        val gradleProps = File(gradleDir, "gradle.properties")
        gradleProps.writeText("""
            SIGNING_KEY=global-key
            SIGNING_PASSWORD=global-password
        """.trimIndent())
        
        val context = createTestContext(enableGlobalGradlePropsDetection = true)
        
        System.setProperty("user.home", tempHomeDir.absolutePath)
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.updatedContext?.hasAutoDetectedSigning).isTrue()
        assertThat(mockPromptSystem.allPrompts).contains("AUTO-DETECTED")
        assertThat(mockPromptSystem.allPrompts).contains("global gradle.properties")
        
        // Cleanup
        tempHomeDir.deleteRecursively()
    }

    @Test
    fun `should prompt for manual signing configuration when not auto-detected`() {
        // Given
        val context = createTestContext()
        mockPromptSystem.addResponse("") // Info display - Press Enter to continue
        mockPromptSystem.addResponse("test-key")
        mockPromptSystem.addResponse("test-password")
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.updatedContext?.hasAutoDetectedSigning).isFalse()
        assertThat(mockPromptSystem.prompts).hasSize(3) // Info + key + password
        assertThat(result.updatedContext?.wizardConfig?.signing?.keyId).isEqualTo("test-key")
    }

    @Test
    fun `should return validation error for empty signing key`() {
        // Given
        val context = createTestContext()
        mockPromptSystem.addResponse("") // Empty key
        
        // When
        val result = processor.process(context, mockPromptSystem)
        
        // Then
        assertThat(result.isValid).isFalse()
        assertThat(result.validationErrors).contains("Signing key is required")
    }

    private fun createTestContext(enableGlobalGradlePropsDetection: Boolean = false) = WizardContext(
        project = TestProjectBuilder.createProject(),
        detectedInfo = DetectedProjectInfo("test-project"),
        wizardConfig = TestConfigBuilder.createConfig(),
        enableGlobalGradlePropsDetection = enableGlobalGradlePropsDetection
    )
}