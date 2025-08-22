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
    fun `should create local repository for publishing`() {
        // Given - Apply java plugin and maven-publish
        project.pluginManager.apply("java-library")
        project.pluginManager.apply("maven-publish")
        
        // When
        manager.configurePublications(config)
        
        // Then - Should configure publishing extension with LocalRepo repository
        val publishingExt = project.extensions.findByType(org.gradle.api.publish.PublishingExtension::class.java)
        assertThat(publishingExt).isNotNull()
        
        val localRepo = publishingExt?.repositories?.findByName("LocalRepo")
        assertThat(localRepo).isNotNull()
    }
    
    @Test
    fun `should create publishToLocalRepo task when maven-publish is available`() {
        // Given - Apply java plugin and maven-publish
        project.pluginManager.apply("java-library")
        project.pluginManager.apply("maven-publish")
        
        // When
        manager.configurePublications(config)
        
        // Then - Should create publishToLocalRepo task
        val task = project.tasks.findByName("publishToLocalRepo")
        assertThat(task).isNotNull()
        assertThat(task?.group).isEqualTo("Central Publishing")
    }
    
    @Test
    fun `should not create publishToLocalRepo task when maven-publish is not available`() {
        // Given - No compatible plugins that would apply maven-publish
        // (Using a plugin that doesn't automatically apply maven-publish)
        
        // When
        manager.configurePublications(config)
        
        // Then - Should not create publishToLocalRepo task since no maven-publish
        val task = project.tasks.findByName("publishToLocalRepo")
        assertThat(task).isNull()
    }
}