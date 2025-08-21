package com.tddworks.sonatype.publish.portal.plugin.wizard

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
        
        val finalConfig = TestConfigBuilder.createConfig().copy(
            projectInfo = TestConfigBuilder.createConfig().projectInfo.copy(
                name = "", // Empty so auto-detected "auto-detected-project" is used
                url = "", // Empty so auto-detected "https://github.com/auto/detected" is used
                developers = emptyList() // Empty so auto-detected developers are used
            )
        )
        
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

    @Test
    fun `should generate dynamic file list based on what was actually created`() {
        // Given - Both credentials and signing are auto-detected (no gradle.properties should be generated)
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
            hasAutoDetectedSigning = true
        )
        
        val finalConfig = TestConfigBuilder.createConfig()
        
        // When
        val generatedFiles = fileGenerator.generateFiles(context, finalConfig)
        
        // Then - Should only include files that were actually created
        assertThat(generatedFiles).containsExactly("build.gradle.kts")
        assertThat(generatedFiles).doesNotContain("gradle.properties")
        
        // Verify actual files on disk
        val buildFile = File(tempDir.toFile(), "build.gradle.kts")
        val gradlePropsFile = File(tempDir.toFile(), "gradle.properties")
        
        assertThat(buildFile).exists()
        assertThat(gradlePropsFile).doesNotExist()
    }

    @Test
    fun `should generate gitignore file`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        val context = WizardContext(
            project = project,
            detectedInfo = DetectedProjectInfo("test-project"),
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false
        )
        
        val finalConfig = TestConfigBuilder.createConfig()
        
        // Create a file generator that includes gitignore generation
        val extendedFileGenerator = DefaultWizardFileGeneratorWithExtras()
        
        // When
        val generatedFiles = extendedFileGenerator.generateFiles(context, finalConfig)
        
        // Then
        assertThat(generatedFiles).contains(".gitignore")
        
        val gitignoreFile = File(tempDir.toFile(), ".gitignore")
        assertThat(gitignoreFile).exists()
        
        val content = gitignoreFile.readText()
        assertThat(content).contains("gradle.properties")
        assertThat(content).contains("*.gpg")
        assertThat(content).contains("local.properties")
    }

    @Test
    fun `should generate CI workflow file`() {
        // Given
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        val context = WizardContext(
            project = project,
            detectedInfo = DetectedProjectInfo("test-project"),
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false
        )
        
        val finalConfig = TestConfigBuilder.createConfig()
        
        // Create a file generator that includes CI generation
        val extendedFileGenerator = DefaultWizardFileGeneratorWithExtras()
        
        // When
        val generatedFiles = extendedFileGenerator.generateFiles(context, finalConfig)
        
        // Then
        assertThat(generatedFiles).contains(".github/workflows/publish.yml")
        
        val ciFile = File(tempDir.toFile(), ".github/workflows/publish.yml")
        assertThat(ciFile).exists()
        
        val content = ciFile.readText()
        assertThat(content).contains("name: Publish to Maven Central")
        assertThat(content).contains("centralPublish")
    }

    @Test
    fun `should preserve existing build file content when updating`() {
        // Given - Create existing build.gradle.kts with user content
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        val buildFile = File(tempDir.toFile(), "build.gradle.kts")
        buildFile.writeText("""
            plugins {
                kotlin("jvm") version "2.2.0"
                `maven-publish`
            }
            
            group = "com.example"
            version = "1.0.0"
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation(kotlin("stdlib"))
            }
            
            // Custom user task
            tasks.register("customTask") {
                doLast { println("Custom task") }
            }
        """.trimIndent())
        
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
        fileGenerator.generateFiles(context, finalConfig)
        
        // Then
        val updatedContent = buildFile.readText()
        
        // Should preserve original content
        assertThat(updatedContent).contains("kotlin(\"jvm\") version \"2.2.0\"")
        assertThat(updatedContent).contains("group = \"com.example\"")
        assertThat(updatedContent).contains("version = \"1.0.0\"")
        assertThat(updatedContent).contains("implementation(kotlin(\"stdlib\"))")
        assertThat(updatedContent).contains("tasks.register(\"customTask\")")
        assertThat(updatedContent).contains("Custom task")
        
        // Should add plugin
        assertThat(updatedContent).contains("id(\"com.tddworks.central-publisher\")")
        
        // Should add centralPublisher block
        assertThat(updatedContent).contains("centralPublisher {")
        assertThat(updatedContent).contains("credentials {")
        assertThat(updatedContent).contains("projectInfo {")
    }

    @Test
    fun `should update existing centralPublisher block without duplicating`() {
        // Given - Build file already has centralPublisher block
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        val buildFile = File(tempDir.toFile(), "build.gradle.kts")
        buildFile.writeText("""
            plugins {
                id("com.tddworks.central-publisher")
            }
            
            centralPublisher {
                credentials {
                    username = "old-username"
                    password = "old-password"
                }
                projectInfo {
                    name = "old-project"
                    description = "Old description"
                }
            }
        """.trimIndent())
        
        val context = WizardContext(
            project = project,
            detectedInfo = DetectedProjectInfo("new-project", "https://github.com/new/project"),
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false,
            hasAutoDetectedCredentials = true,
            hasAutoDetectedSigning = true
        )
        
        val finalConfig = TestConfigBuilder.createConfig().copy(
            projectInfo = TestConfigBuilder.createConfig().projectInfo.copy(
                name = "", // Empty so auto-detected "new-project" is used
                url = "" // Empty so auto-detected "https://github.com/new/project" is used
            )
        )
        
        // When
        fileGenerator.generateFiles(context, finalConfig)
        
        // Then
        val updatedContent = buildFile.readText()
        
        // Should have updated centralPublisher block
        assertThat(updatedContent).contains("name = \"new-project\"")
        assertThat(updatedContent).contains("https://github.com/new/project")
        
        // Should not contain old values
        assertThat(updatedContent).doesNotContain("old-username")
        assertThat(updatedContent).doesNotContain("old-project")
        assertThat(updatedContent).doesNotContain("Old description")
        
        // Should have only one centralPublisher block
        assertThat(updatedContent.split("centralPublisher \\{".toRegex())).hasSize(2) // Split creates 2 parts for 1 occurrence
    }

    @Test
    fun `should add plugin and block to build file without plugins block`() {
        // Given - Build file without plugins block
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        val buildFile = File(tempDir.toFile(), "build.gradle.kts")
        buildFile.writeText("""
            group = "com.example"
            version = "1.0.0"
            
            repositories {
                mavenCentral()
            }
        """.trimIndent())
        
        val context = WizardContext(
            project = project,
            detectedInfo = DetectedProjectInfo("test-project"),
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false,
            hasAutoDetectedCredentials = true,
            hasAutoDetectedSigning = true
        )
        
        val finalConfig = TestConfigBuilder.createConfig()
        
        // When
        fileGenerator.generateFiles(context, finalConfig)
        
        // Then
        val updatedContent = buildFile.readText()
        
        // Should add plugins block at the beginning
        assertThat(updatedContent).startsWith("plugins {")
        assertThat(updatedContent).contains("id(\"com.tddworks.central-publisher\")")
        
        // Should preserve existing content
        assertThat(updatedContent).contains("group = \"com.example\"")
        assertThat(updatedContent).contains("repositories {")
        
        // Should add centralPublisher block
        assertThat(updatedContent).contains("centralPublisher {")
    }

    @Test
    fun `should use manually entered project information in build file instead of auto-detected values`() {
        // Given - Auto-detected info different from manual input
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
        
        val detectedInfo = DetectedProjectInfo(
            projectName = "auto-detected-project",
            projectUrl = "https://github.com/auto/detected",
            developers = listOf(DetectedDeveloper("Auto User", "auto@detected.com"))
        )
        
        // Manual input in finalConfig should override auto-detected values
        val finalConfig = TestConfigBuilder.createConfig().copy(
            projectInfo = TestConfigBuilder.createConfig().projectInfo.copy(
                name = "manually-entered-project",
                description = "Manually entered description", 
                url = "https://github.com/manual/entered",
                developers = listOf(
                    com.tddworks.sonatype.publish.portal.plugin.config.DeveloperConfig(
                        id = "manual-user",
                        name = "Manual User",
                        email = "manual@entered.com"
                    )
                )
            )
        )
        
        val context = WizardContext(
            project = project,
            detectedInfo = detectedInfo,
            wizardConfig = TestConfigBuilder.createConfig(),
            enableGlobalGradlePropsDetection = false
        )
        
        // When
        fileGenerator.generateFiles(context, finalConfig)
        
        // Then
        val buildFile = File(tempDir.toFile(), "build.gradle.kts")
        val content = buildFile.readText()
        
        // Should use manually entered values from finalConfig, not auto-detected values
        assertThat(content).contains("name = \"manually-entered-project\"")
        assertThat(content).contains("description = \"Manually entered description\"")
        assertThat(content).contains("url = \"https://github.com/manual/entered\"")
        assertThat(content).contains("id = \"manual-user\"")
        assertThat(content).contains("name = \"Manual User\"")
        assertThat(content).contains("email = \"manual@entered.com\"")
        
        // Should NOT contain auto-detected values
        assertThat(content).doesNotContain("auto-detected-project")
        assertThat(content).doesNotContain("https://github.com/auto/detected")
        assertThat(content).doesNotContain("Auto User")
        assertThat(content).doesNotContain("auto@detected.com")
    }
}