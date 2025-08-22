package com.tddworks.sonatype.publish.portal.plugin.wizard.steps

import com.tddworks.sonatype.publish.portal.plugin.wizard.*
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argThat
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import uk.org.webcompere.systemstubs.jupiter.SystemStub
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension
import java.io.File
import java.nio.file.Path

@ExtendWith(SystemStubsExtension::class, MockitoExtension::class)
class SigningStepProcessorTest {

    @SystemStub
    private lateinit var environmentVariables: EnvironmentVariables

    @Mock
    private lateinit var mockPromptSystem: PromptSystem

    @TempDir
    lateinit var tempDir: Path

    private lateinit var processor: SigningStepProcessor
    private lateinit var context: WizardContext

    @BeforeEach
    fun setup() {
        processor = SigningStepProcessor()
        
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
            
        context = WizardContext(
            project = project,
            detectedInfo = DetectedProjectInfo("test-project"),
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false
        )
    }

    @Test
    fun `should auto-detect environment variables and ask user if they want to use them`() {
        // Given
        environmentVariables.set("SIGNING_KEY", "test-signing-key")
        environmentVariables.set("SIGNING_PASSWORD", "test-signing-password")
        
        // Mock user saying yes to auto-detected signing
        `when`(mockPromptSystem.confirm(anyString())).thenReturn(true)

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.updatedContext?.wizardConfig?.signing?.keyId).isEqualTo("test-signing-key")
        assertThat(result.updatedContext?.wizardConfig?.signing?.password).isEqualTo("test-signing-password")
        assertThat(result.updatedContext?.hasAutoDetectedSigning).isTrue()
    }

    @Test
    fun `should allow user to reject auto-detected signing and input manually`() {
        // Given
        environmentVariables.set("SIGNING_KEY", "auto-detected-key")
        environmentVariables.set("SIGNING_PASSWORD", "auto-detected-password")
        
        // Mock user saying no to auto-detected, then providing manual input
        `when`(mockPromptSystem.confirm(anyString())).thenReturn(false)
        `when`(mockPromptSystem.prompt(anyString())).thenReturn("manual-key", "manual-password")

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.updatedContext?.wizardConfig?.signing?.keyId).isEqualTo("manual-key")
        assertThat(result.updatedContext?.wizardConfig?.signing?.password).isEqualTo("manual-password")
        assertThat(result.updatedContext?.hasAutoDetectedSigning).isFalse()
    }

    @Test
    fun `should prompt for manual input when no auto-detection found`() {
        // Given - no environment variables
        `when`(mockPromptSystem.prompt(anyString())).thenReturn("manual-key", "manual-password")

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.updatedContext?.wizardConfig?.signing?.keyId).isEqualTo("manual-key")
        assertThat(result.updatedContext?.wizardConfig?.signing?.password).isEqualTo("manual-password")
        assertThat(result.updatedContext?.hasAutoDetectedSigning).isFalse()
    }

    @Test
    fun `should validate required signing key when manually entering`() {
        // Given - empty signing key
        `when`(mockPromptSystem.prompt(anyString())).thenReturn("", "")

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isFalse()
        assertThat(result.validationErrors).contains("Signing key is required")
    }

    @Test
    fun `should handle global gradle properties detection when enabled`() {
        // Given
        val contextWithGlobal = context.copy(enableGlobalGradlePropsDetection = true)
        
        // Create mock gradle.properties file
        val gradleDir = File(System.getProperty("user.home"), ".gradle")
        gradleDir.mkdirs()
        val gradleProps = File(gradleDir, "gradle.properties")
        gradleProps.writeText("""
            SIGNING_KEY=global-key
            SIGNING_PASSWORD=global-password
        """.trimIndent())
        
        try {
            // Mock user confirming use of global properties
            `when`(mockPromptSystem.confirm(anyString())).thenReturn(true)

            // When
            val result = processor.process(contextWithGlobal, mockPromptSystem)

            // Then
            assertThat(result.isValid).isTrue()
            assertThat(result.updatedContext?.wizardConfig?.signing?.keyId).isEqualTo("global-key")
            assertThat(result.updatedContext?.wizardConfig?.signing?.password).isEqualTo("global-password")
            assertThat(result.updatedContext?.hasAutoDetectedSigning).isTrue()
        } finally {
            gradleProps.delete()
        }
    }

    @Test
    fun `should mask signing key properly in confirmation prompt`() {
        // Given
        environmentVariables.set("SIGNING_KEY", "very-long-signing-key-that-should-be-masked")
        environmentVariables.set("SIGNING_PASSWORD", "test-password")
        
        // Mock user saying yes
        `when`(mockPromptSystem.confirm(anyString())).thenReturn(true)

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then - The key should be masked but still work
        assertThat(result.isValid).isTrue()
        assertThat(result.updatedContext?.wizardConfig?.signing?.keyId)
            .isEqualTo("very-long-signing-key-that-should-be-masked")
    }

    @Test
    fun `should show step progress indicator when auto-detecting environment variables`() {
        // Given
        environmentVariables.set("SIGNING_KEY", "test-key")
        environmentVariables.set("SIGNING_PASSWORD", "test-password")
        `when`(mockPromptSystem.confirm(anyString())).thenReturn(true)

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        verify(mockPromptSystem).confirm(argThat { message ->
            message.contains("SIGNING SETUP - AUTO-DETECTED! (Step 4 of 6)")
        })
    }

    @Test
    fun `should mask short keys completely`() {
        // Given
        environmentVariables.set("SIGNING_KEY", "shortkey")
        environmentVariables.set("SIGNING_PASSWORD", "test-password")
        `when`(mockPromptSystem.confirm(anyString())).thenReturn(true)

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        verify(mockPromptSystem).confirm(argThat { message ->
            message.contains("• SIGNING_KEY: ********")
        })
    }

    @Test
    fun `should mask long keys with first and last 4 characters visible`() {
        // Given
        environmentVariables.set("SIGNING_KEY", "very-long-signing-key-that-should-be-partially-masked")
        environmentVariables.set("SIGNING_PASSWORD", "test-password")
        `when`(mockPromptSystem.confirm(anyString())).thenReturn(true)

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        verify(mockPromptSystem).confirm(argThat { message ->
            message.contains("• SIGNING_KEY: very*********************************************sked")
        })
    }

    @Test
    fun `should prefer environment variables over global properties when both exist`() {
        // Given
        environmentVariables.set("SIGNING_KEY", "env-key")
        environmentVariables.set("SIGNING_PASSWORD", "env-password")
        
        val contextWithGlobal = context.copy(enableGlobalGradlePropsDetection = true)
        val gradleDir = File(System.getProperty("user.home"), ".gradle")
        gradleDir.mkdirs()
        val gradleProps = File(gradleDir, "gradle.properties")
        gradleProps.writeText("""
            SIGNING_KEY=global-key
            SIGNING_PASSWORD=global-password
        """.trimIndent())
        
        try {
            `when`(mockPromptSystem.confirm(anyString())).thenReturn(true)

            // When
            val result = processor.process(contextWithGlobal, mockPromptSystem)

            // Then
            assertThat(result.isValid).isTrue()
            assertThat(result.updatedContext?.wizardConfig?.signing?.keyId).isEqualTo("env-key")
            assertThat(result.updatedContext?.wizardConfig?.signing?.password).isEqualTo("env-password")
            // Should show environment variables prompt, not global properties
            verify(mockPromptSystem).confirm(argThat { message ->
                message.contains("Found existing environment variables:")
            })
        } finally {
            gradleProps.delete()
        }
    }

    @Test
    fun `should allow empty password when key is provided in manual input`() {
        // Given - key but empty password
        `when`(mockPromptSystem.prompt(anyString())).thenReturn("test-key", "")

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then - should still be valid (password can be empty)
        assertThat(result.isValid).isTrue()
        assertThat(result.updatedContext?.wizardConfig?.signing?.keyId).isEqualTo("test-key")
        assertThat(result.updatedContext?.wizardConfig?.signing?.password).isEqualTo("")
    }

    @Test
    fun `should show manual input message when user rejects auto-detection`() {
        // Given
        environmentVariables.set("SIGNING_KEY", "auto-key")
        environmentVariables.set("SIGNING_PASSWORD", "auto-password")
        `when`(mockPromptSystem.confirm(anyString())).thenReturn(false)
        `when`(mockPromptSystem.prompt(anyString())).thenReturn("manual-key", "manual-password")

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        verify(mockPromptSystem).display(argThat { message ->
            message.contains("You chose to configure signing credentials manually.")
        })
    }

    @Test
    fun `should show different message when no auto-detection found`() {
        // Given - no environment variables or global properties
        `when`(mockPromptSystem.prompt(anyString())).thenReturn("manual-key", "manual-password")

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        verify(mockPromptSystem).display(argThat { message ->
            message.contains("No signing credentials detected. Manual configuration needed.")
        })
    }

    @Test
    fun `should handle missing global gradle properties file`() {
        // Given
        val contextWithGlobal = context.copy(enableGlobalGradlePropsDetection = true)
        // Ensure no global gradle.properties file exists
        val gradleProps = File(System.getProperty("user.home"), ".gradle/gradle.properties")
        if (gradleProps.exists()) {
            gradleProps.delete()
        }
        
        `when`(mockPromptSystem.prompt(anyString())).thenReturn("manual-key", "manual-password")

        // When
        val result = processor.process(contextWithGlobal, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.updatedContext?.wizardConfig?.signing?.keyId).isEqualTo("manual-key")
        assertThat(result.updatedContext?.wizardConfig?.signing?.password).isEqualTo("manual-password")
        assertThat(result.updatedContext?.hasAutoDetectedSigning).isFalse()
    }

    @Test
    fun `should handle global properties file with missing signing key`() {
        // Given
        val contextWithGlobal = context.copy(enableGlobalGradlePropsDetection = true)
        val gradleDir = File(System.getProperty("user.home"), ".gradle")
        gradleDir.mkdirs()
        val gradleProps = File(gradleDir, "gradle.properties")
        gradleProps.writeText("""
            # Missing SIGNING_KEY
            SIGNING_PASSWORD=global-password
            OTHER_PROPERTY=value
        """.trimIndent())
        
        try {
            `when`(mockPromptSystem.prompt(anyString())).thenReturn("manual-key", "manual-password")

            // When
            val result = processor.process(contextWithGlobal, mockPromptSystem)

            // Then
            assertThat(result.isValid).isTrue()
            assertThat(result.updatedContext?.wizardConfig?.signing?.keyId).isEqualTo("manual-key")
            assertThat(result.updatedContext?.hasAutoDetectedSigning).isFalse()
        } finally {
            gradleProps.delete()
        }
    }

    @Test
    fun `should handle global properties file with missing signing password`() {
        // Given
        val contextWithGlobal = context.copy(enableGlobalGradlePropsDetection = true)
        val gradleDir = File(System.getProperty("user.home"), ".gradle")
        gradleDir.mkdirs()
        val gradleProps = File(gradleDir, "gradle.properties")
        gradleProps.writeText("""
            SIGNING_KEY=global-key
            # Missing SIGNING_PASSWORD
            OTHER_PROPERTY=value
        """.trimIndent())
        
        try {
            `when`(mockPromptSystem.prompt(anyString())).thenReturn("manual-key", "manual-password")

            // When
            val result = processor.process(contextWithGlobal, mockPromptSystem)

            // Then
            assertThat(result.isValid).isTrue()
            assertThat(result.updatedContext?.wizardConfig?.signing?.keyId).isEqualTo("manual-key")
            assertThat(result.updatedContext?.hasAutoDetectedSigning).isFalse()
        } finally {
            gradleProps.delete()
        }
    }

    @Test
    fun `should handle global properties file with empty values`() {
        // Given
        val contextWithGlobal = context.copy(enableGlobalGradlePropsDetection = true)
        val gradleDir = File(System.getProperty("user.home"), ".gradle")
        gradleDir.mkdirs()
        val gradleProps = File(gradleDir, "gradle.properties")
        gradleProps.writeText("""
            SIGNING_KEY=
            SIGNING_PASSWORD=
        """.trimIndent())
        
        try {
            `when`(mockPromptSystem.prompt(anyString())).thenReturn("manual-key", "manual-password")

            // When
            val result = processor.process(contextWithGlobal, mockPromptSystem)

            // Then
            assertThat(result.isValid).isTrue()
            assertThat(result.updatedContext?.wizardConfig?.signing?.keyId).isEqualTo("manual-key")
            assertThat(result.updatedContext?.hasAutoDetectedSigning).isFalse()
        } finally {
            gradleProps.delete()
        }
    }

    @Test
    fun `should handle user rejecting global properties auto-detection`() {
        // Given
        val contextWithGlobal = context.copy(enableGlobalGradlePropsDetection = true)
        val gradleDir = File(System.getProperty("user.home"), ".gradle")
        gradleDir.mkdirs()
        val gradleProps = File(gradleDir, "gradle.properties")
        gradleProps.writeText("""
            SIGNING_KEY=global-key
            SIGNING_PASSWORD=global-password
        """.trimIndent())
        
        try {
            // Mock user rejecting global properties, then providing manual input
            `when`(mockPromptSystem.confirm(anyString())).thenReturn(false)
            `when`(mockPromptSystem.prompt(anyString())).thenReturn("manual-key", "manual-password")

            // When
            val result = processor.process(contextWithGlobal, mockPromptSystem)

            // Then
            assertThat(result.isValid).isTrue()
            assertThat(result.updatedContext?.wizardConfig?.signing?.keyId).isEqualTo("manual-key")
            assertThat(result.updatedContext?.wizardConfig?.signing?.password).isEqualTo("manual-password")
            assertThat(result.updatedContext?.hasAutoDetectedSigning).isFalse()
            verify(mockPromptSystem).display(argThat { message ->
                message.contains("You chose to configure signing credentials manually.")
            })
        } finally {
            gradleProps.delete()
        }
    }
}