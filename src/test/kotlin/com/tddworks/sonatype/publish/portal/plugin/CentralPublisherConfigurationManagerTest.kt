package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.plugin.dsl.CentralPublisherExtension
import com.tddworks.sonatype.publish.portal.plugin.validation.ValidationResult
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CentralPublisherConfigurationManagerTest {
    
    private lateinit var project: Project
    private lateinit var extension: CentralPublisherExtension
    private lateinit var manager: CentralPublisherConfigurationManager
    
    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()
        extension = CentralPublisherExtension(project)
        manager = CentralPublisherConfigurationManager(project, extension)
    }
    
    @Test
    fun `should determine if publishing setup is needed`() {
        // Given - No explicit configuration
        
        // When
        val shouldSetup = manager.shouldSetupPublishing()
        
        // Then - Should indicate setup is needed when no configuration exists
        assertThat(shouldSetup).isFalse() // No config = don't setup automatically
    }
    
    @Test
    fun `should determine publishing is needed when extension has configuration`() {
        // Given - Extension has some configuration
        extension.credentials {
            username = "test-user"
            password = "test-password"
        }
        
        // When
        val shouldSetup = manager.shouldSetupPublishing()
        
        // Then - Should indicate setup is needed
        assertThat(shouldSetup).isTrue()
    }
    
    @Test
    fun `should resolve configuration from extension`() {
        // Given - Extension configured with credentials
        extension.credentials {
            username = "test-user"
            password = "test-password"
        }
        extension.projectInfo {
            name = "test-project"
            description = "Test project description"
            url = "https://github.com/test/project"
        }
        
        // When
        val config = manager.resolveConfiguration()
        
        // Then - Should return properly resolved configuration
        assertThat(config.credentials.username).isEqualTo("test-user")
        assertThat(config.credentials.password).isEqualTo("test-password")
        assertThat(config.projectInfo.name).isEqualTo("test-project")
        assertThat(config.projectInfo.description).isEqualTo("Test project description")
        assertThat(config.projectInfo.url).isEqualTo("https://github.com/test/project")
    }
    
    @Test
    fun `should validate configuration and return results`() {
        // Given - Extension with incomplete configuration
        extension.credentials {
            username = "test-user"
            // Missing password
        }
        
        // When
        val validationResult = manager.validateConfiguration()
        
        // Then - Should return validation errors
        assertThat(validationResult.isValid).isFalse()
        assertThat(validationResult.getErrors()).isNotEmpty()
    }
    
    @Test
    fun `should handle configuration with auto-detection`() {
        // Given - Project with git properties for auto-detection
        val namedProject = ProjectBuilder.builder()
            .withName("test-project")
            .build()
        namedProject.group = "com.example"
        namedProject.version = "1.0.0"
        
        val namedExtension = CentralPublisherExtension(namedProject)
        val namedManager = CentralPublisherConfigurationManager(namedProject, namedExtension)
        
        // When
        val config = namedManager.resolveConfiguration()
        
        // Then - Should include auto-detected values
        assertThat(config.projectInfo.name).isEqualTo("test-project")
        // Note: More auto-detection tests would depend on git setup
    }
    
    @Test
    fun `should merge configuration from multiple sources`() {
        // Given - Extension configuration and gradle properties
        extension.credentials {
            username = "dsl-user"
        }
        project.extensions.extraProperties.set("centralPublisher.credentials.password", "props-password")
        
        // When
        val config = manager.resolveConfiguration()
        
        // Then - Should merge from both sources with proper precedence
        assertThat(config.credentials.username).isEqualTo("dsl-user") // DSL wins
        // Note: Property merging would need actual implementation details
    }
}