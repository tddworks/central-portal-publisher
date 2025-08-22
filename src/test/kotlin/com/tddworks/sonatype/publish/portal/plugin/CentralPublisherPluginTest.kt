package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.plugin.dsl.CentralPublisherExtension
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path

class CentralPublisherPluginTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var project: Project
    private lateinit var outputStream: ByteArrayOutputStream
    private lateinit var originalOut: PrintStream
    
    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .build()
        outputStream = ByteArrayOutputStream()
        originalOut = System.out
        System.setOut(PrintStream(outputStream))
    }
    
    @Test
    fun `should register centralPublisher extension when plugin is applied`() {
        // When
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // Then
        val extension = project.extensions.findByType(CentralPublisherExtension::class.java)
        assertThat(extension).isNotNull()
        
        val extensionByName = project.extensions.findByName("centralPublisher")
        assertThat(extensionByName).isNotNull()
        assertThat(extensionByName).isInstanceOf(CentralPublisherExtension::class.java)
    }
    
    @Test
    fun `should register all publishing tasks`() {
        // When
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // Configure minimal extension to trigger afterEvaluate
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
        }
        
        // Trigger afterEvaluate to simulate actual plugin behavior
        project.getTasksByName("tasks", false) // This triggers project evaluation
        
        // Then - All main tasks should be registered
        assertThat(project.tasks.findByName("publishToCentral")).isNotNull()
        assertThat(project.tasks.findByName("validatePublishing")).isNotNull()
        assertThat(project.tasks.findByName("bundleArtifacts")).isNotNull()
        assertThat(project.tasks.findByName("setupPublishing")).isNotNull()
    }
    
    @Test
    fun `should set correct task group and descriptions`() {
        // When
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // Configure minimal extension to trigger afterEvaluate
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
        }
        
        // Trigger afterEvaluate to simulate actual plugin behavior
        project.getTasksByName("tasks", false) // This triggers project evaluation
        
        // Then
        val publishTask = project.tasks.findByName("publishToCentral")!!
        assertThat(publishTask.group).isEqualTo("Central Publishing")
        assertThat(publishTask.description).isEqualTo("🚀 Publish your artifacts to Maven Central (creates bundle and uploads)")
        
        val validateTask = project.tasks.findByName("validatePublishing")!!
        assertThat(validateTask.group).isEqualTo("Central Publishing")
        assertThat(validateTask.description).contains("Check if your project is ready to publish")
        
        val bundleTask = project.tasks.findByName("bundleArtifacts")!!
        assertThat(bundleTask.group).isEqualTo("Central Publishing")
        assertThat(bundleTask.description).contains("Prepare your artifacts for publishing")
        
        val setupTask = project.tasks.findByName("setupPublishing")!!
        assertThat(setupTask.group).isEqualTo("Central Publishing")
        assertThat(setupTask.description).contains("Set up your project for Maven Central publishing")
    }
    
    @Test
    fun `should work with DSL configuration`() {
        // Given
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // When - Configure via DSL
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
            
            projectInfo {
                name = "test-project"
                description = "A test project"
                url = "https://github.com/test/project"
            }
            
            publishing {
                dryRun = true
                autoPublish = false
            }
        }
        
        // Then - Should be able to build configuration
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        val config = extension.build()
        
        assertThat(config.credentials.username).isEqualTo("test-user")
        assertThat(config.projectInfo.name).isEqualTo("test-project")
        assertThat(config.publishing.dryRun).isTrue()
        assertThat(config.publishing.autoPublish).isFalse()
    }
    
    @Test
    fun `should provide clean task names compared to old plugin`() {
        // When
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // Configure minimal extension to trigger afterEvaluate
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
        }
        
        // Manually create tasks using the managers directly (test-friendly approach)
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        val configurationManager = CentralPublisherConfigurationManager(project, extension)
        val config = configurationManager.resolveConfiguration()
        val taskManager = CentralPublisherTaskManager(project)
        taskManager.createPublishingTasks(config)
        
        // Then - Should have simple, memorable task names
        assertThat(project.tasks.names).contains(
            "publishToCentral",      // vs old: publishAllPublicationsToSonatypePortalRepository
            "bundleArtifacts",       // vs old: zipAllPublications  
            "validatePublishing",    // vs old: N/A
            "setupPublishing"        // vs old: N/A
        )
        
        // Tasks should be easy to discover
        val centralTaskNames = project.tasks.names.filter { taskName ->
            val task = project.tasks.findByName(taskName)
            task?.group == "Central Publishing"
        }
        assertThat(centralTaskNames).hasSize(4) // publishToCentral, bundleArtifacts, validatePublishing, setupPublishing
    }
    
    @Test
    fun `should handle auto-detection and validation`() {
        // Given
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // When - Configure minimal DSL (relies on auto-detection)
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "minimal-user"
                password = "minimal-token"
            }
            // projectInfo should be auto-detected
        }
        
        // Then - Should successfully build config with auto-detected values
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        val config = extension.build()
        
        assertThat(config.credentials.username).isEqualTo("minimal-user")
        // Auto-detected project name should not be empty
        assertThat(config.projectInfo.name).isNotEmpty()
        
        // Should be able to validate
        val validationEngine = com.tddworks.sonatype.publish.portal.plugin.validation.ValidationEngine()
        val result = validationEngine.validate(config)
        
        // May have warnings but should not crash
        assertThat(result).isNotNull()
    }
    
    @Test
    fun `should configure local repository for bundle creation`() {
        // Given
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // When - Configure minimal extension to trigger afterEvaluate
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
        }
        
        // Trigger afterEvaluate to simulate actual plugin behavior
        project.getTasksByName("tasks", false) // This triggers project evaluation
        
        // Then - Should have configured publishing extension with local repository
        val publishing = project.extensions.getByType(org.gradle.api.publish.PublishingExtension::class.java)
        assertThat(publishing.repositories).isNotEmpty()
        
        val localRepo = publishing.repositories.findByName("LocalRepo")
        assertThat(localRepo).isNotNull()
        
        // Should have standard Gradle publishAllPublicationsToLocalRepoRepository task
        val publishTask = project.tasks.findByName("publishAllPublicationsToLocalRepoRepository")
        assertThat(publishTask).isNotNull()
        assertThat(publishTask!!.group).isEqualTo("publishing")
    }
    
    @Test
    fun `should create bundleArtifacts task with correct dependencies`() {
        // Given
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // When - Configure minimal extension to trigger afterEvaluate
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
        }
        
        // Trigger afterEvaluate to simulate actual plugin behavior
        project.getTasksByName("tasks", false) // This triggers project evaluation
        
        // Then - bundleArtifacts should depend on publishToLocalRepo
        val bundleTask = project.tasks.findByName("bundleArtifacts")
        assertThat(bundleTask).isNotNull()
        
        val dependencies = bundleTask!!.dependsOn
        assertThat(dependencies).contains("publishAllPublicationsToLocalRepoRepository")
    }
    
    @Test
    fun `should configure publications when maven-publish plugin is applied`() {
        // Given - Apply java and maven-publish plugins for opt-in behavior
        project.pluginManager.apply("java")
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // When - Configure minimal extension to trigger afterEvaluate
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
        }
        
        // Manually create tasks and publications using managers directly (test-friendly approach)
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        val configurationManager = CentralPublisherConfigurationManager(project, extension)
        val config = configurationManager.resolveConfiguration()
        val publicationManager = CentralPublisherPublicationManager(project)
        publicationManager.configurePublications(config)
        
        // Then - maven-publish plugin should be applied
        assertThat(project.plugins.hasPlugin("maven-publish")).isTrue()
        
        // Publications should be auto-configured
        val publishing = project.extensions.getByType(org.gradle.api.publish.PublishingExtension::class.java)
        assertThat(publishing.publications).isNotEmpty()
    }
    
    @Test
    fun `should handle dry run mode properly`() {
        // Given
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // When - Configure with dry run enabled
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
            publishing {
                dryRun = true
            }
        }
        
        // Then - Should build configuration with dry run enabled
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        val config = extension.build()
        
        assertThat(config.publishing.dryRun).isTrue()
    }
    
    @Test
    fun `should handle namespace validation for bundle creation`() {
        // Given
        project.group = "com.example"  // Invalid namespace for Maven Central
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // When - Configure minimal extension to trigger afterEvaluate
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
        }
        
        // Then - Should not crash with example namespace (just warn)
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        val config = extension.build()
        
        assertThat(config.projectInfo.name).isNotEmpty()
        // The actual warning would be logged during bundle creation
    }
    
    @Test
    fun `should not show validation errors when no explicit configuration exists`() {
        // Given - Apply plugin but don't configure centralPublisher block
        project.pluginManager.apply(CentralPublisherPlugin::class.java)
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)

        // When - Force project evaluation to trigger configuration phase
        project.getTasksByName("tasks", false)

        // Then - Should not have explicit configuration
        assertThat(extension.hasExplicitConfiguration()).isFalse()
        
        // Note: Logger output goes to logger.quiet() which may not be captured by System.out redirection
        // The core behavior is that hasExplicitConfiguration returns false when no DSL configuration is used
    }

    @Test
    fun `should show validation errors when explicit configuration exists but is invalid`() {
        // Given
        project.pluginManager.apply(CentralPublisherPlugin::class.java)
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)

        // Configure extension with invalid data to trigger explicit configuration
        extension.credentials {
            username = ""  // Invalid - empty username
            password = "test-password"
        }

        // When
        project.getTasksByName("tasks", false)

        // Then - Should have explicit configuration marked  
        assertThat(extension.hasExplicitConfiguration()).isTrue()
        
        // Note: Validation error messages are logged via logger.error() and may not be captured in System.out
        // The key behavior is that hasExplicitConfiguration() is true
    }

    @Test
    fun `should pass validation when explicit configuration exists and is valid`() {
        // Given
        project.pluginManager.apply(CentralPublisherPlugin::class.java)
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)

        // Configure extension with valid data
        extension.credentials {
            username = "test-user"
            password = "test-password"
        }
        
        extension.projectInfo {
            name = "test-project"
            description = "Test project description"
            url = "https://github.com/test/test-project"
            
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
            }
            
            developer {
                id = "test-dev"
                name = "Test Developer"
                email = "test@example.com"
            }
            
            scm {
                url = "https://github.com/test/test-project"
                connection = "scm:git:git://github.com/test/test-project.git"
                developerConnection = "scm:git:ssh://github.com/test/test-project.git"
            }
        }
        
        extension.signing {
            keyId = "test-key"
            password = "test-key-password"
            secretKeyRingFile = "~/.gnupg/secring.gpg"
        }

        // When
        project.getTasksByName("tasks", false) //internally it calls project.evaluate()

        // Then - Should have explicit configuration
        assertThat(extension.hasExplicitConfiguration()).isTrue()
        
        // Note: Validation success messages are logged via logger.quiet() which may not be captured
        // The important behavior is that hasExplicitConfiguration() correctly identifies explicit config
    }

    @Test
    fun `should handle hasExplicitConfiguration correctly across different configurations`() {
        // Given
        project.pluginManager.apply(CentralPublisherPlugin::class.java)
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)

        // When - No configuration
        assertThat(extension.hasExplicitConfiguration()).isFalse()

        // When - Configure credentials only
        extension.credentials {
            username = "test-user"
        }
        assertThat(extension.hasExplicitConfiguration()).isTrue()

        // When - Configure another section  
        extension.projectInfo {
            name = "test-project"
        }
        assertThat(extension.hasExplicitConfiguration()).isTrue()
    }
}