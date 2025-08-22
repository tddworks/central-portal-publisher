package com.tddworks.sonatype.publish.portal.plugin.publication

import com.tddworks.sonatype.publish.portal.plugin.config.*
import com.tddworks.sonatype.publish.portal.plugin.publication.strategies.JavaLibraryConfigurationStrategy
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class PluginBasedConfiguratorTest {
    
    private lateinit var project: Project
    private lateinit var config: CentralPublisherConfig
    private lateinit var configurator: PluginBasedConfigurator
    
    @BeforeEach
    fun setUp() {
        // Use a real project for integration testing (like other passing tests)
        project = ProjectBuilder.builder().build()
        
        // Create real configuration
        config = CentralPublisherConfig(
            projectInfo = ProjectInfoConfig(
                name = "test-project",
                description = "Test project description",
                url = "https://github.com/test/project",
                license = LicenseConfig(
                    name = "Apache License, Version 2.0",
                    url = "http://www.apache.org/licenses/LICENSE-2.0.txt",
                    distribution = "repo"
                ),
                developers = listOf(
                    DeveloperConfig(
                        id = "dev1",
                        name = "Developer One",
                        email = "dev1@example.com"
                    )
                ),
                scm = ScmConfig(
                    connection = "scm:git:git://github.com/test/project.git",
                    developerConnection = "scm:git:ssh://github.com:test/project.git",
                    url = "https://github.com/test/project"
                )
            ),
            credentials = CredentialsConfig(
                username = "test-user",
                password = "test-token"
            )
        )
        
        configurator = PluginBasedConfigurator()
    }
    
    @Test
    fun `should detect and configure Java Library projects using real strategy`() {
        // Given - Apply required plugins to the real project
        project.pluginManager.apply("java-library")
        project.pluginManager.apply("maven-publish")
        
        val javaLibraryStrategy = JavaLibraryConfigurationStrategy()
        configurator.registerStrategy(javaLibraryStrategy)
        
        // When
        val result = configurator.configureBasedOnAppliedPlugins(project, config)
        
        // Then
        assertThat(result.isConfigured).isTrue()
        assertThat(result.detectedPluginType).isEqualTo("java-library")
    }
    
    @Test
    fun `should return unconfigured result when no compatible plugins found`() {
        // Given - no plugins applied to the project (default empty state)
        
        // When
        val result = configurator.configureBasedOnAppliedPlugins(project, config)
        
        // Then
        assertThat(result.isConfigured).isFalse()
        assertThat(result.detectedPluginType).isNull()
        assertThat(result.reason).contains("No compatible plugin found")
    }
    
    @Test
    fun `should support registering multiple strategies`() {
        // Given
        val strategy1 = JavaLibraryConfigurationStrategy()
        val strategy2 = JavaLibraryConfigurationStrategy()
        
        configurator.registerStrategy(strategy1)
        configurator.registerStrategy(strategy2)
        
        // When
        val strategies = configurator.getRegisteredStrategies()
        
        // Then
        assertThat(strategies).hasSize(2)
    }
    
    @Test
    fun `should clear all strategies`() {
        // Given
        configurator.registerStrategy(JavaLibraryConfigurationStrategy())
        
        // When
        configurator.clearStrategies()
        
        // Then
        assertThat(configurator.getRegisteredStrategies()).isEmpty()
    }
}