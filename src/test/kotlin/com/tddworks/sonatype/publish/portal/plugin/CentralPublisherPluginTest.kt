package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.plugin.dsl.CentralPublisherExtension
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CentralPublisherPluginTest {
    
    private lateinit var project: Project
    
    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
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
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // Configure minimal extension to trigger afterEvaluate
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
        }
        
        // Manually trigger the plugin configuration that would happen in afterEvaluate
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        val plugin = CentralPublisherPlugin()
        // Use reflection to call private configurePlugin method
        val configureMethod = CentralPublisherPlugin::class.java.getDeclaredMethod("configurePlugin", Project::class.java, CentralPublisherExtension::class.java)
        configureMethod.isAccessible = true
        configureMethod.invoke(plugin, project, extension)
        
        // Then - All main tasks should be registered
        assertThat(project.tasks.findByName("publishToCentral")).isNotNull()
        assertThat(project.tasks.findByName("validatePublishing")).isNotNull()
        assertThat(project.tasks.findByName("bundleArtifacts")).isNotNull()
        assertThat(project.tasks.findByName("setupPublishing")).isNotNull()
    }
    
    @Test
    fun `should set correct task group and descriptions`() {
        // When
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // Configure minimal extension to trigger afterEvaluate
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
        }
        
        // Manually trigger the plugin configuration that would happen in afterEvaluate
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        val plugin = CentralPublisherPlugin()
        // Use reflection to call private configurePlugin method
        val configureMethod = CentralPublisherPlugin::class.java.getDeclaredMethod("configurePlugin", Project::class.java, CentralPublisherExtension::class.java)
        configureMethod.isAccessible = true
        configureMethod.invoke(plugin, project, extension)
        
        // Then
        val publishTask = project.tasks.findByName("publishToCentral")!!
        assertThat(publishTask.group).isEqualTo("Central Publishing")
        assertThat(publishTask.description).isEqualTo("Publishes all artifacts to Maven Central")
        
        val validateTask = project.tasks.findByName("validatePublishing")!!
        assertThat(validateTask.group).isEqualTo("Central Publishing")
        assertThat(validateTask.description).contains("Validates publishing configuration")
        
        val bundleTask = project.tasks.findByName("bundleArtifacts")!!
        assertThat(bundleTask.group).isEqualTo("Central Publishing")
        assertThat(bundleTask.description).contains("Creates deployment bundle")
        
        val setupTask = project.tasks.findByName("setupPublishing")!!
        assertThat(setupTask.group).isEqualTo("Central Publishing")
        assertThat(setupTask.description).contains("Interactive setup wizard")
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
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // Configure minimal extension to trigger afterEvaluate
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
        }
        
        // Manually trigger the plugin configuration that would happen in afterEvaluate
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        val plugin = CentralPublisherPlugin()
        // Use reflection to call private configurePlugin method
        val configureMethod = CentralPublisherPlugin::class.java.getDeclaredMethod("configurePlugin", Project::class.java, CentralPublisherExtension::class.java)
        configureMethod.isAccessible = true
        configureMethod.invoke(plugin, project, extension)
        
        // Then - Should have simple, memorable task names
        assertThat(project.tasks.names).contains(
            "publishToCentral",      // vs old: publishAllPublicationsToSonatypePortalRepository
            "bundleArtifacts",       // vs old: zipAllPublications  
            "validatePublishing",    // vs old: N/A
            "setupPublishing"        // vs old: N/A
        )
        
        // Tasks should be easy to discover
        val centralTasks = project.tasks.matching { it.group == "Central Publishing" }
        assertThat(centralTasks).hasSize(5) // publishToCentral, bundleArtifacts, validatePublishing, setupPublishing, publishToLocalRepo
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
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // When - Configure minimal extension to trigger afterEvaluate
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
        }
        
        // Manually trigger the plugin configuration that would happen in afterEvaluate
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        val plugin = CentralPublisherPlugin()
        val configureMethod = CentralPublisherPlugin::class.java.getDeclaredMethod("configurePlugin", Project::class.java, CentralPublisherExtension::class.java)
        configureMethod.isAccessible = true
        configureMethod.invoke(plugin, project, extension)
        
        // Then - Should have configured publishing extension with local repository
        val publishing = project.extensions.getByType(org.gradle.api.publish.PublishingExtension::class.java)
        assertThat(publishing.repositories).isNotEmpty()
        
        val localRepo = publishing.repositories.findByName("LocalRepo")
        assertThat(localRepo).isNotNull()
        
        // Should have publishToLocalRepo task
        val publishTask = project.tasks.findByName("publishToLocalRepo")
        assertThat(publishTask).isNotNull()
        assertThat(publishTask!!.group).isEqualTo("Central Publishing")
    }
    
    @Test
    fun `should create bundleArtifacts task with correct dependencies`() {
        // Given
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // When - Configure minimal extension to trigger afterEvaluate
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
        }
        
        // Manually trigger the plugin configuration that would happen in afterEvaluate
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        val plugin = CentralPublisherPlugin()
        val configureMethod = CentralPublisherPlugin::class.java.getDeclaredMethod("configurePlugin", Project::class.java, CentralPublisherExtension::class.java)
        configureMethod.isAccessible = true
        configureMethod.invoke(plugin, project, extension)
        
        // Then - bundleArtifacts should depend on publishToLocalRepo
        val bundleTask = project.tasks.findByName("bundleArtifacts")
        assertThat(bundleTask).isNotNull()
        
        val dependencies = bundleTask!!.dependsOn
        assertThat(dependencies).contains("publishToLocalRepo")
    }
    
    @Test
    fun `should apply maven-publish plugin and configure publications automatically`() {
        // Given - Apply java plugin which is available in test environment
        project.pluginManager.apply("java")
        project.pluginManager.apply("com.tddworks.central-publisher")
        
        // When - Configure minimal extension to trigger afterEvaluate
        project.extensions.configure(CentralPublisherExtension::class.java) {
            credentials {
                username = "test-user"
                password = "test-token"
            }
        }
        
        // Manually trigger the plugin configuration that would happen in afterEvaluate
        val extension = project.extensions.getByType(CentralPublisherExtension::class.java)
        val plugin = CentralPublisherPlugin()
        val configureMethod = CentralPublisherPlugin::class.java.getDeclaredMethod("configurePlugin", Project::class.java, CentralPublisherExtension::class.java)
        configureMethod.isAccessible = true
        configureMethod.invoke(plugin, project, extension)
        
        // Then - maven-publish plugin should be applied
        assertThat(project.plugins.hasPlugin("maven-publish")).isTrue()
        
        // Publications should be auto-configured
        val publishing = project.extensions.getByType(org.gradle.api.publish.PublishingExtension::class.java)
        assertThat(publishing.publications).isNotEmpty()
    }
    
    @Test
    fun `should handle dry run mode properly`() {
        // Given
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
}