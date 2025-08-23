package com.tddworks.sonatype.publish.portal.plugin.wizard.steps

import com.tddworks.sonatype.publish.portal.plugin.wizard.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WelcomeStepProcessorTest {

    private val mockPromptSystem = MockPromptSystem()
    private val processor = WelcomeStepProcessor()

    @Test
    fun `should display welcome message with progress indicator`() {
        // Given
        val context = createTestContextWithDetectedInfo()
        mockPromptSystem.addResponse("") // User presses Enter

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.currentStep).isEqualTo(WizardStep.WELCOME)

        // Should display welcome message
        assertThat(mockPromptSystem.displayMessages).isNotEmpty()
        val displayedMessage = mockPromptSystem.displayMessages.joinToString("\n")
        assertThat(displayedMessage).contains("🧙 WELCOME TO CENTRAL PUBLISHER SETUP (Step 1 of 6)")
        assertThat(displayedMessage)
            .contains("This wizard will help you configure publishing to Maven Central")
    }

    @Test
    fun `should show detected project information in welcome message`() {
        // Given
        val context = createTestContextWithDetectedInfo()
        mockPromptSystem.addResponse("") // User presses Enter

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()

        val displayedMessage = mockPromptSystem.displayMessages.joinToString("\n")
        assertThat(displayedMessage).contains("🔍 Auto-detected information:")
        assertThat(displayedMessage).contains("• Project name: test-project")
        assertThat(displayedMessage).contains("• Project URL: https://github.com/test/test-project")
        assertThat(displayedMessage).contains("• Developers: Test User <test@example.com>")
    }

    @Test
    fun `should handle context with no detected information`() {
        // Given
        val context = createTestContextWithoutDetectedInfo()
        mockPromptSystem.addResponse("") // User presses Enter

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        assertThat(result.currentStep).isEqualTo(WizardStep.WELCOME)

        val displayedMessage = mockPromptSystem.displayMessages.joinToString("\n")
        assertThat(displayedMessage).contains("🧙 WELCOME TO CENTRAL PUBLISHER SETUP (Step 1 of 6)")
        // Should not contain auto-detected section when no info is detected
        assertThat(displayedMessage).doesNotContain("🔍 Auto-detected information:")
    }

    @Test
    fun `should show partial detected information when some fields are missing`() {
        // Given
        val context = createTestContextWithPartialDetectedInfo()
        mockPromptSystem.addResponse("") // User presses Enter

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()

        val displayedMessage = mockPromptSystem.displayMessages.joinToString("\n")
        assertThat(displayedMessage).contains("🔍 Auto-detected information:")
        assertThat(displayedMessage).contains("• Project name: test-project")
        // Should not show empty URL or developers
        assertThat(displayedMessage).doesNotContain("• Project URL:")
        assertThat(displayedMessage).doesNotContain("• Developers:")
    }

    @Test
    fun `should prompt user to continue after displaying welcome message`() {
        // Given
        val context = createTestContextWithDetectedInfo()
        mockPromptSystem.addResponse("") // User presses Enter

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()
        // Should prompt user to continue
        assertThat(mockPromptSystem.prompts).contains("Press Enter to continue...")
    }

    @Test
    fun `should show configuration overview in welcome message`() {
        // Given
        val context = createTestContextWithDetectedInfo()
        mockPromptSystem.addResponse("") // User presses Enter

        // When
        val result = processor.process(context, mockPromptSystem)

        // Then
        assertThat(result.isValid).isTrue()

        val displayedMessage = mockPromptSystem.displayMessages.joinToString("\n")
        assertThat(displayedMessage).contains("What we'll configure:")
        assertThat(displayedMessage)
            .contains("• Project information (name, URL, description, developers)")
        assertThat(displayedMessage).contains("• Sonatype credentials for Maven Central")
        assertThat(displayedMessage).contains("• GPG signing configuration")
        assertThat(displayedMessage).contains("• Review and test your configuration")
        assertThat(displayedMessage).contains("Let's get started! 🚀")
    }

    private fun createTestContextWithDetectedInfo() =
        WizardContext(
            project = TestProjectBuilder.createProject("test-project"),
            detectedInfo =
                DetectedProjectInfo(
                    projectName = "test-project",
                    projectUrl = "https://github.com/test/test-project",
                    developers = listOf(DetectedDeveloper("Test User", "test@example.com")),
                ),
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false,
        )

    private fun createTestContextWithoutDetectedInfo() =
        WizardContext(
            project = TestProjectBuilder.createProject("test-project"),
            detectedInfo = null,
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false,
        )

    private fun createTestContextWithPartialDetectedInfo() =
        WizardContext(
            project = TestProjectBuilder.createProject("test-project"),
            detectedInfo =
                DetectedProjectInfo(
                    projectName = "test-project",
                    projectUrl = "", // Missing URL
                    developers = emptyList(), // No developers
                ),
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false,
        )
}
