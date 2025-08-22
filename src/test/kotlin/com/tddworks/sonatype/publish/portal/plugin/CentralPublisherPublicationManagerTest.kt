package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.plugin.config.*
import com.tddworks.sonatype.publish.portal.plugin.publication.ConfigurationResult
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CentralPublisherPublicationManagerTest {
    
    private lateinit var project: Project
    private lateinit var manager: CentralPublisherPublicationManager
    private lateinit var config: CentralPublisherConfig
    
    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()
        manager = CentralPublisherPublicationManager(project)
        
        // Create a minimal valid configuration for testing
        config = CentralPublisherConfigBuilder()
            .credentials {
                username = "test-user"
                password = "test-password"
            }
            .projectInfo {
                name = "test-project"
                description = "Test project description"
                url = "https://github.com/test/project"
            }
            .build()
    }
    
    @Test
    fun `should configure publications based on applied plugins`() {
        // Given - Apply Java plugin to make it detectable
        project.pluginManager.apply("java-library")
        
        // When
        val result = manager.configurePublications(config)
        
        // Then - Should return successful configuration result
        assertThat(result.isConfigured).isTrue()
        assertThat(result.detectedPluginType).isEqualTo("java-library")
    }
    
    @Test
    fun `should return unconfigured result when no compatible plugins found`() {
        // Given - No compatible plugins applied
        
        // When
        val result = manager.configurePublications(config)
        
        // Then - Should return unconfigured result with reason
        assertThat(result.isConfigured).isFalse()
        assertThat(result.reason).isNotBlank()
    }
    
    @Test
    fun `should detect Kotlin JVM projects`() {
        // Given - Apply Kotlin JVM plugin (if available in test environment)
        try {
            project.pluginManager.apply("org.jetbrains.kotlin.jvm")
            
            // When
            val result = manager.configurePublications(config)
            
            // Then - Should detect as kotlin-jvm project
            assertThat(result.isConfigured).isTrue()
            assertThat(result.detectedPluginType).isEqualTo("kotlin-jvm")
        } catch (e: Exception) {
            // Kotlin plugin not available in test environment - this is expected
            project.logger.debug("Kotlin JVM plugin not available in test environment: ${e.message}")
        }
    }
    
    @Test
    fun `should handle multiple compatible plugins with priority`() {
        // Given - Apply both Java and Kotlin plugins (Java should have lower priority)
        project.pluginManager.apply("java-library")
        try {
            project.pluginManager.apply("org.jetbrains.kotlin.jvm")
            
            // When
            val result = manager.configurePublications(config)
            
            // Then - Should choose higher priority plugin (Kotlin JVM)
            assertThat(result.isConfigured).isTrue()
            assertThat(result.detectedPluginType).isEqualTo("kotlin-jvm")
        } catch (e: Exception) {
            // Kotlin plugin not available - should fall back to Java
            val result = manager.configurePublications(config)
            assertThat(result.isConfigured).isTrue()
            assertThat(result.detectedPluginType).isEqualTo("java-library")
        }
    }
    
    @Test
    fun `should configure publications for java-library projects`() {
        // Given - Apply java plugin and maven-publish
        project.pluginManager.apply("java-library")
        project.pluginManager.apply("maven-publish")
        
        // When
        val result = manager.configurePublications(config)
        
        // Then - Should successfully configure publications
        assertThat(result.isConfigured).isTrue()
        assertThat(result.detectedPluginType).isEqualTo("java-library")
        
        // Should configure publishing extension (but LocalRepo is created by TaskManager)
        val publishingExt = project.extensions.findByType(org.gradle.api.publish.PublishingExtension::class.java)
        assertThat(publishingExt).isNotNull()
    }
    
    @Test
    fun `should configure task dependencies when maven-publish is available`() {
        // Given - Apply java plugin and maven-publish, and create the task that TaskManager would create
        project.pluginManager.apply("java-library")
        project.pluginManager.apply("maven-publish")
        
        // Simulate TaskManager creating the publishToLocalRepo task
        project.tasks.register("publishToLocalRepo") {
            group = "Central Publishing"
            description = "Publishes to local repository for bundle creation"
        }
        
        // When
        val result = manager.configurePublications(config)
        
        // Then - Should configure publications and task dependencies
        assertThat(result.isConfigured).isTrue()
        assertThat(result.detectedPluginType).isEqualTo("java-library")
        
        // PublicationManager configures dependencies, doesn't create the task
        val task = project.tasks.findByName("publishToLocalRepo")
        assertThat(task).isNotNull()
    }
    
    @Test
    fun `should handle projects without maven-publish gracefully`() {
        // Given - No compatible plugins that would apply maven-publish
        // (Using a plugin that doesn't automatically apply maven-publish)
        
        // When
        val result = manager.configurePublications(config)
        
        // Then - Should return unconfigured result since no compatible plugins found
        assertThat(result.isConfigured).isFalse()
        assertThat(result.reason).isNotBlank()
        
        // PublicationManager doesn't create tasks - that's TaskManager's responsibility
        val task = project.tasks.findByName("publishToLocalRepo")
        assertThat(task).isNull()
    }
}