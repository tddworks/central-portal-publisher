package com.tddworks.sonatype.publish.portal.plugin.publication.strategies

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import org.gradle.api.Project
import org.gradle.api.plugins.PluginContainer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.*

class KotlinJvmConfigurationStrategyTest {
    
    private lateinit var project: Project
    private lateinit var pluginContainer: PluginContainer
    private lateinit var config: CentralPublisherConfig
    private lateinit var strategy: KotlinJvmConfigurationStrategy
    
    @BeforeEach
    fun setUp() {
        project = mock(Project::class.java)
        pluginContainer = mock(PluginContainer::class.java)
        config = mock(CentralPublisherConfig::class.java)
        
        `when`(project.plugins).thenReturn(pluginContainer)
        
        strategy = KotlinJvmConfigurationStrategy()
    }
    
    @Test
    fun `should detect Kotlin JVM projects`() {
        // Given
        `when`(pluginContainer.hasPlugin("org.jetbrains.kotlin.jvm")).thenReturn(true)
        
        // When
        val canHandle = strategy.canHandle(project)
        
        // Then
        assertThat(canHandle).isTrue()
    }
    
    @Test
    fun `should not detect non-Kotlin JVM projects`() {
        // Given
        `when`(pluginContainer.hasPlugin("org.jetbrains.kotlin.jvm")).thenReturn(false)
        
        // When
        val canHandle = strategy.canHandle(project)
        
        // Then
        assertThat(canHandle).isFalse()
    }
    
    @Test
    fun `should return correct plugin type identifier`() {
        // When
        val pluginType = strategy.getPluginType()
        
        // Then
        assertThat(pluginType).isEqualTo("kotlin-jvm")
    }
    
    @Test
    fun `should return correct priority`() {
        // When
        val priority = strategy.getPriority()
        
        // Then
        assertThat(priority).isEqualTo(10)
    }
}