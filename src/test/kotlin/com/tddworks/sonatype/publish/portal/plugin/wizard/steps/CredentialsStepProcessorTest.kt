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
class CredentialsStepProcessorTest {

    @SystemStub
    private lateinit var environmentVariables: EnvironmentVariables

    @Mock
    private lateinit var mockPromptSystem: PromptSystem

    @TempDir
    lateinit var tempDir: Path

    private lateinit var processor: CredentialsStepProcessor
    private lateinit var context: WizardContext

    @BeforeEach
    fun setup() {
        processor = CredentialsStepProcessor()
        
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
        environmentVariables.set("SONATYPE_USERNAME", "test-user")
        environmentVariables.set("SONATYPE_PASSWORD", "test-password")
        
        // Mock user saying yes to auto-detected credentials
        `when`(mockPromptSystem.confirm(anyString())).thenReturn(true)

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.updatedContext?.wizardConfig?.credentials?.username).isEqualTo("test-user")
        assertThat(result.updatedContext?.wizardConfig?.credentials?.password).isEqualTo("test-password")
        assertThat(result.updatedContext?.hasAutoDetectedCredentials).isTrue()
    }

    @Test
    fun `should allow user to reject auto-detected credentials and input manually`() {
        // Given
        environmentVariables.set("SONATYPE_USERNAME", "auto-detected-user")
        environmentVariables.set("SONATYPE_PASSWORD", "auto-detected-password")
        
        // Mock user saying no to auto-detected, then providing manual input
        `when`(mockPromptSystem.confirm(anyString())).thenReturn(false)
        `when`(mockPromptSystem.prompt(anyString())).thenReturn("manual-user", "manual-password")

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.updatedContext?.wizardConfig?.credentials?.username).isEqualTo("manual-user")
        assertThat(result.updatedContext?.wizardConfig?.credentials?.password).isEqualTo("manual-password")
        assertThat(result.updatedContext?.hasAutoDetectedCredentials).isFalse()
    }

    @Test
    fun `should prompt for manual input when no auto-detection found`() {
        // Given - no environment variables
        `when`(mockPromptSystem.prompt(anyString())).thenReturn("manual-user", "manual-password")

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.updatedContext?.wizardConfig?.credentials?.username).isEqualTo("manual-user")
        assertThat(result.updatedContext?.wizardConfig?.credentials?.password).isEqualTo("manual-password")
        assertThat(result.updatedContext?.hasAutoDetectedCredentials).isFalse()
    }

    @Test
    fun `should validate required username when manually entering`() {
        // Given - empty username
        `when`(mockPromptSystem.prompt(anyString())).thenReturn("", "")

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isFalse()
        assertThat(result.validationErrors).contains("Username is required")
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
            SONATYPE_USERNAME=global-user
            SONATYPE_PASSWORD=global-password
        """.trimIndent())
        
        try {
            // Mock user confirming use of global properties
            `when`(mockPromptSystem.confirm(anyString())).thenReturn(true)

            // When
            val result = processor.process(contextWithGlobal, mockPromptSystem)

            // Then
            assertThat(result.isValid).isTrue()
            assertThat(result.updatedContext?.wizardConfig?.credentials?.username).isEqualTo("global-user")
            assertThat(result.updatedContext?.wizardConfig?.credentials?.password).isEqualTo("global-password")
            assertThat(result.updatedContext?.hasAutoDetectedCredentials).isTrue()
        } finally {
            gradleProps.delete()
        }
    }

    @Test
    fun `should show step progress indicator when auto-detecting environment variables`() {
        // Given
        environmentVariables.set("SONATYPE_USERNAME", "test-user")
        environmentVariables.set("SONATYPE_PASSWORD", "test-password")
        `when`(mockPromptSystem.confirm(anyString())).thenReturn(true)

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        verify(mockPromptSystem).confirm(argThat { message ->
            message.contains("CREDENTIALS SETUP - AUTO-DETECTED! (Step 3 of 6)")
        })
    }

    @Test
    fun `should mask password properly in environment variable confirmation`() {
        // Given
        environmentVariables.set("SONATYPE_USERNAME", "test-user")
        environmentVariables.set("SONATYPE_PASSWORD", "very-long-password-should-be-masked")
        `when`(mockPromptSystem.confirm(anyString())).thenReturn(true)

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        verify(mockPromptSystem).confirm(argThat { message ->
            message.contains("• SONATYPE_PASSWORD: ********")
        })
    }

    @Test
    fun `should prefer environment variables over global properties when both exist`() {
        // Given
        environmentVariables.set("SONATYPE_USERNAME", "env-user")
        environmentVariables.set("SONATYPE_PASSWORD", "env-password")
        
        val contextWithGlobal = context.copy(enableGlobalGradlePropsDetection = true)
        val gradleDir = File(System.getProperty("user.home"), ".gradle")
        gradleDir.mkdirs()
        val gradleProps = File(gradleDir, "gradle.properties")
        gradleProps.writeText("""
            SONATYPE_USERNAME=global-user
            SONATYPE_PASSWORD=global-password
        """.trimIndent())
        
        try {
            `when`(mockPromptSystem.confirm(anyString())).thenReturn(true)

            // When
            val result = processor.process(contextWithGlobal, mockPromptSystem)

            // Then
            assertThat(result.isValid).isTrue()
            assertThat(result.updatedContext?.wizardConfig?.credentials?.username).isEqualTo("env-user")
            assertThat(result.updatedContext?.wizardConfig?.credentials?.password).isEqualTo("env-password")
            // Should show environment variables prompt, not global properties
            verify(mockPromptSystem).confirm(argThat { message ->
                message.contains("Found existing environment variables:")
            })
        } finally {
            gradleProps.delete()
        }
    }

    @Test
    fun `should validate empty password when username is provided`() {
        // Given - username but empty password
        `when`(mockPromptSystem.prompt(anyString())).thenReturn("test-user", "")

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then - should still be valid (password can be empty)
        assertThat(result.isValid).isTrue()
        assertThat(result.updatedContext?.wizardConfig?.credentials?.username).isEqualTo("test-user")
        assertThat(result.updatedContext?.wizardConfig?.credentials?.password).isEqualTo("")
    }

    @Test
    fun `should show manual input message when user rejects auto-detection`() {
        // Given
        environmentVariables.set("SONATYPE_USERNAME", "auto-user")
        environmentVariables.set("SONATYPE_PASSWORD", "auto-password")
        `when`(mockPromptSystem.confirm(anyString())).thenReturn(false)
        `when`(mockPromptSystem.prompt(anyString())).thenReturn("manual-user", "manual-password")

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        verify(mockPromptSystem).display(argThat { message ->
            message.contains("You chose to configure credentials manually.")
        })
    }
}