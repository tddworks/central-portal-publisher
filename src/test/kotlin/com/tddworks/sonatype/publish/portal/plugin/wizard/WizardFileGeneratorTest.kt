package com.tddworks.sonatype.publish.portal.plugin.wizard

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class WizardFileGeneratorTest {

    @TempDir
    lateinit var tempDir: Path

    private val fileGenerator = DefaultWizardFileGenerator()

    @Test
    fun `should generate both gradle properties and build file when neither credentials nor signing are auto-detected`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        val context = WizardContext(
            project = project,
            detectedInfo = DetectedProjectInfo("test-project", "https://github.com/test/test-project"),
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false,
            hasAutoDetectedCredentials = false,
            hasAutoDetectedSigning = false
        )
        
        val finalConfig = TestConfigBuilder.createConfig()
        
        // When
        val generatedFiles = fileGenerator.generateFiles(context, finalConfig)
        
        // Then
        assertThat(generatedFiles).hasSize(2)
        assertThat(generatedFiles).contains("gradle.properties")
        assertThat(generatedFiles).contains("build.gradle.kts")
        
        val gradlePropsFile = File(tempDir.toFile(), "gradle.properties")
        val buildFile = File(tempDir.toFile(), "build.gradle.kts")
        
        assertThat(gradlePropsFile).exists()
        assertThat(buildFile).exists()
    }

    @Test
    fun `should only generate build file when both credentials and signing are auto-detected`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        val context = WizardContext(
            project = project,
            detectedInfo = DetectedProjectInfo("test-project", "https://github.com/test/test-project"),
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false,
            hasAutoDetectedCredentials = true,
            hasAutoDetectedSigning = true
        )
        
        val finalConfig = TestConfigBuilder.createConfig()
        
        // When
        val generatedFiles = fileGenerator.generateFiles(context, finalConfig)
        
        // Then
        assertThat(generatedFiles).hasSize(1)
        assertThat(generatedFiles).contains("build.gradle.kts")
        
        val gradlePropsFile = File(tempDir.toFile(), "gradle.properties")
        val buildFile = File(tempDir.toFile(), "build.gradle.kts")
        
        assertThat(gradlePropsFile).doesNotExist()
        assertThat(buildFile).exists()
    }

    @Test
    fun `should generate gradle properties with only missing configuration sections`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        val context = WizardContext(
            project = project,
            detectedInfo = DetectedProjectInfo("test-project"),
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false,
            hasAutoDetectedCredentials = true,
            hasAutoDetectedSigning = false
        )
        
        val finalConfig = TestConfigBuilder.createConfig()
        
        // When
        val generatedFiles = fileGenerator.generateFiles(context, finalConfig)
        
        // Then
        assertThat(generatedFiles).hasSize(2)
        val gradlePropsFile = File(tempDir.toFile(), "gradle.properties")
        val content = gradlePropsFile.readText()
        
        // Should not contain credentials section since they're auto-detected
        assertThat(content).doesNotContain("# Central Publisher Credentials")
        // Should contain signing section since it's not auto-detected
        assertThat(content).contains("# Central Publisher Signing")
    }

    @Test
    fun `should use auto-detected project information in build file`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        val detectedInfo = DetectedProjectInfo(
            projectName = "auto-detected-project",
            projectUrl = "https://github.com/auto/detected",
            developers = listOf(DetectedDeveloper("Auto User", "auto@detected.com"))
        )
        
        val context = WizardContext(
            project = project,
            detectedInfo = detectedInfo,
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false
        )
        
        val finalConfig = TestConfigBuilder.createConfig()
        
        // When
        fileGenerator.generateFiles(context, finalConfig)
        
        // Then
        val buildFile = File(tempDir.toFile(), "build.gradle.kts")
        val content = buildFile.readText()
        
        assertThat(content).contains("https://github.com/auto/detected")
        assertThat(content).contains("Auto User")
        assertThat(content).contains("auto@detected.com")
        assertThat(content).contains("auto") // developer ID
    }
}