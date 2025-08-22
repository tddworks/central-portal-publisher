package com.tddworks.sonatype.publish.portal.plugin

import com.tddworks.sonatype.publish.portal.plugin.config.*
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CentralPublisherTaskManagerTest {
    
    private lateinit var project: Project
    private lateinit var manager: CentralPublisherTaskManager
    private lateinit var config: CentralPublisherConfig
    
    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()
        manager = CentralPublisherTaskManager(project)
        
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
    fun `should create all required publishing tasks`() {
        // When
        manager.createPublishingTasks(config)
        
        // Then - Should create all the expected tasks
        assertThat(project.tasks.findByName("publishToCentral")).isNotNull()
        assertThat(project.tasks.findByName("validatePublishing")).isNotNull()
        assertThat(project.tasks.findByName("bundleArtifacts")).isNotNull()
        assertThat(project.tasks.findByName("setupPublishing")).isNotNull()
    }
    
    @Test
    fun `should set correct task groups`() {
        // When
        manager.createPublishingTasks(config)
        
        // Then - All tasks should be in the Central Publishing group
        val publishTask = project.tasks.getByName("publishToCentral")
        val validateTask = project.tasks.getByName("validatePublishing")
        val bundleTask = project.tasks.getByName("bundleArtifacts")
        val setupTask = project.tasks.getByName("setupPublishing")
        
        assertThat(publishTask.group).isEqualTo("Central Publishing")
        assertThat(validateTask.group).isEqualTo("Central Publishing")
        assertThat(bundleTask.group).isEqualTo("Central Publishing")
        assertThat(setupTask.group).isEqualTo("Central Publishing")
    }
    
    @Test
    fun `should set correct task descriptions`() {
        // When
        manager.createPublishingTasks(config)
        
        // Then - Tasks should have meaningful descriptions
        val publishTask = project.tasks.getByName("publishToCentral")
        val validateTask = project.tasks.getByName("validatePublishing")
        val bundleTask = project.tasks.getByName("bundleArtifacts")
        val setupTask = project.tasks.getByName("setupPublishing")
        
        assertThat(publishTask.description).isEqualTo("🚀 Publish your artifacts to Maven Central (creates bundle and uploads)")
        assertThat(validateTask.description).isEqualTo("✅ Check if your project is ready to publish (no upload, safe to run)")
        assertThat(bundleTask.description).isEqualTo("📦 Prepare your artifacts for publishing (signs, validates, bundles)")
        assertThat(setupTask.description).isEqualTo("🧙 Set up your project for Maven Central publishing (interactive guide)")
    }
    
    @Test
    fun `should set up correct task dependencies`() {
        // When
        manager.createPublishingTasks(config)
        
        // Then - publishToCentral should depend on bundleArtifacts
        val publishTask = project.tasks.getByName("publishToCentral")
        val bundleTask = project.tasks.getByName("bundleArtifacts")
        
        assertThat(publishTask.taskDependencies.getDependencies(publishTask))
            .contains(bundleTask)
    }
    
    @Test
    fun `should handle dry run mode configuration`() {
        // Given - Configuration with dry run enabled
        val dryRunConfig = CentralPublisherConfigBuilder()
            .credentials {
                username = "test-user"
                password = "test-password"
            }
            .projectInfo {
                name = "test-project"
                description = "Test project description"
                url = "https://github.com/test/project"
            }
            .publishing {
                dryRun = true
            }
            .build()
        
        // When
        manager.createPublishingTasks(dryRunConfig)
        
        // Then - Tasks should be created (dry run is handled in task execution, not creation)
        assertThat(project.tasks.findByName("publishToCentral")).isNotNull()
    }
    
    @Test
    fun `should not create duplicate tasks when called multiple times`() {
        // Given - Tasks already created
        manager.createPublishingTasks(config)
        val initialTaskCount = project.tasks.size
        
        // When - Called again
        manager.createPublishingTasks(config)
        
        // Then - Should not create duplicate tasks
        assertThat(project.tasks.size).isEqualTo(initialTaskCount)
    }
}