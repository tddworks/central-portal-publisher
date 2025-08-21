package com.tddworks.sonatype.publish.portal.plugin.wizard

import com.tddworks.sonatype.publish.portal.plugin.config.*
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class DefaultWizardFileGeneratorTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var generator: DefaultWizardFileGenerator
    private lateinit var context: WizardContext
    private lateinit var config: CentralPublisherConfig

    @BeforeEach
    fun setup() {
        generator = DefaultWizardFileGenerator()
        
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
            
        context = WizardContext(
            project = project,
            detectedInfo = DetectedProjectInfo("test-project"),
            wizardConfig = createTestConfig(),
            enableGlobalGradlePropsDetection = false
        )
        
        config = createTestConfig()
    }

    @Test
    fun `should not generate gradle properties when credentials are auto-detected`() {
        // Given - context with auto-detected credentials
        val contextWithAutoDetected = context.copy(
            hasAutoDetectedCredentials = true,
            hasAutoDetectedSigning = true
        )

        // When
        val generatedFiles = generator.generateFiles(contextWithAutoDetected, config)

        // Then - should only generate build.gradle.kts
        assertThat(generatedFiles).containsExactly("build.gradle.kts")
        assertThat(File(tempDir.toFile(), "gradle.properties")).doesNotExist()
    }

    @Test
    fun `should not generate gradle properties when global credentials exist`() {
        // Given - create fake global gradle.properties
        val userHome = System.getProperty("user.home")
        val globalGradleDir = File(userHome, ".gradle")
        globalGradleDir.mkdirs()
        val globalGradleProps = File(globalGradleDir, "gradle.properties")
        val originalContent = if (globalGradleProps.exists()) globalGradleProps.readText() else null
        
        try {
            globalGradleProps.writeText("""
                SONATYPE_USERNAME=global-user
                SONATYPE_PASSWORD=global-password
                SIGNING_KEY=global-key
                SIGNING_PASSWORD=global-key-password
            """.trimIndent())

            // Context with manual credentials but global exists
            val contextWithManual = context.copy(
                hasAutoDetectedCredentials = false,
                hasAutoDetectedSigning = false
            )

            // When
            val generatedFiles = generator.generateFiles(contextWithManual, config)

            // Then - should only generate build.gradle.kts (no local gradle.properties)
            assertThat(generatedFiles).containsExactly("build.gradle.kts")
            assertThat(File(tempDir.toFile(), "gradle.properties")).doesNotExist()
            
        } finally {
            // Cleanup - restore original content or delete
            if (originalContent != null) {
                globalGradleProps.writeText(originalContent)
            } else {
                globalGradleProps.delete()
            }
        }
    }

    @Test
    fun `should generate gradle properties when manual credentials and no global setup`() {
        // Given - context with manual credentials and no global setup
        val contextWithManual = context.copy(
            hasAutoDetectedCredentials = false,
            hasAutoDetectedSigning = false
        )

        // Ensure no global gradle.properties with credentials
        val userHome = System.getProperty("user.home")
        val globalGradleProps = File(userHome, ".gradle/gradle.properties")
        val originalContent = if (globalGradleProps.exists()) globalGradleProps.readText() else null
        
        try {
            // Remove or clear global credentials for this test
            if (globalGradleProps.exists()) {
                val contentWithoutCreds = originalContent?.lines()?.filter { 
                    !it.startsWith("SONATYPE_") && !it.startsWith("SIGNING_")
                }?.joinToString("\n") ?: ""
                globalGradleProps.writeText(contentWithoutCreds)
            }

            // When
            val generatedFiles = generator.generateFiles(contextWithManual, config)

            // Then - should generate both files
            assertThat(generatedFiles).containsExactlyInAnyOrder("build.gradle.kts", "gradle.properties")
            
            val gradlePropsFile = File(tempDir.toFile(), "gradle.properties")
            assertThat(gradlePropsFile).exists()
            
            val content = gradlePropsFile.readText()
            assertThat(content).contains("SONATYPE_USERNAME=test-user")
            assertThat(content).contains("SONATYPE_PASSWORD=test-password")
            assertThat(content).contains("SIGNING_KEY=test-key")
            assertThat(content).contains("SIGNING_PASSWORD=test-key-password")
            
        } finally {
            // Restore original content
            if (originalContent != null) {
                globalGradleProps.writeText(originalContent)
            }
        }
    }

    @Test
    fun `should always generate build gradle kts file`() {
        // Given - any context
        
        // When
        val generatedFiles = generator.generateFiles(context, config)

        // Then - should always include build.gradle.kts
        assertThat(generatedFiles).contains("build.gradle.kts")
        
        val buildFile = File(tempDir.toFile(), "build.gradle.kts")
        assertThat(buildFile).exists()
        
        val content = buildFile.readText()
        assertThat(content).contains("centralPublisher {")
        assertThat(content).contains("credentials {")
        assertThat(content).contains("projectInfo {")
        assertThat(content).contains("name = \"test-project\"")
        assertThat(content).contains("description = \"Test Description\"")
    }

    @Test
    fun `should update existing build gradle kts without destroying content`() {
        // Given - existing build.gradle.kts with content
        val buildFile = File(tempDir.toFile(), "build.gradle.kts")
        buildFile.writeText("""
            plugins {
                kotlin("jvm") version "1.9.20"
                id("com.tddworks.central-publisher")
            }
            
            dependencies {
                implementation("some:dependency:1.0")
            }
            
            tasks.test {
                useJUnitPlatform()
            }
        """.trimIndent())

        // When
        val generatedFiles = generator.generateFiles(context, config)

        // Then - should preserve existing content and add centralPublisher block
        assertThat(generatedFiles).contains("build.gradle.kts")
        
        val content = buildFile.readText()
        assertThat(content).contains("kotlin(\"jvm\") version \"1.9.20\"") // Preserved
        assertThat(content).contains("implementation(\"some:dependency:1.0\")") // Preserved
        assertThat(content).contains("tasks.test {") // Preserved
        assertThat(content).contains("centralPublisher {") // Added
        assertThat(content).contains("name = \"test-project\"") // Added
    }

    @Test
    fun `should replace existing centralPublisher block in build gradle kts`() {
        // Given - existing build.gradle.kts with old centralPublisher block
        val buildFile = File(tempDir.toFile(), "build.gradle.kts")
        buildFile.writeText("""
            plugins {
                kotlin("jvm") version "1.9.20"
                id("com.tddworks.central-publisher")
            }
            
            centralPublisher {
                credentials {
                    username = "old-user"
                    password = "old-password"
                }
                projectInfo {
                    name = "old-name"
                    description = "old description"
                }
            }
            
            dependencies {
                implementation("some:dependency:1.0")
            }
        """.trimIndent())

        // When
        val generatedFiles = generator.generateFiles(context, config)

        // Then - should replace old centralPublisher block with new one
        assertThat(generatedFiles).contains("build.gradle.kts")
        
        val content = buildFile.readText()
        assertThat(content).contains("kotlin(\"jvm\") version \"1.9.20\"") // Preserved
        assertThat(content).contains("implementation(\"some:dependency:1.0\")") // Preserved
        assertThat(content).contains("centralPublisher {") // Updated
        assertThat(content).contains("name = \"test-project\"") // New value
        assertThat(content).contains("description = \"Test Description\"") // New value
        assertThat(content).doesNotContain("old-user") // Old value removed
        assertThat(content).doesNotContain("old-name") // Old value removed
    }

    private fun createTestConfig(): CentralPublisherConfig {
        return CentralPublisherConfigBuilder()
            .credentials {
                username = "test-user"
                password = "test-password"
            }
            .projectInfo {
                name = "test-project"
                description = "Test Description"
                url = "https://github.com/test/test-project"
                license {
                    name = "Apache License 2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                }
                developer {
                    id = "testdev"
                    name = "Test Developer"
                    email = "test@example.com"
                }

                scm {
                    url = "https://github.com/test/test-project"
                    connection = "scm:git:git://github.com/test/test-project.git"
                    developerConnection = "scm:git:ssh://github.com/test/test-project.git"
                }
            }
            .signing {
                keyId = "test-key"
                password = "test-key-password"
                secretKeyRingFile = "~/.gnupg/secring.gpg"
            }
            .build()
    }
}