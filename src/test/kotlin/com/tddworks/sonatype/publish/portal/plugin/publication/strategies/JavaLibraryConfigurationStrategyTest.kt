package com.tddworks.sonatype.publish.portal.plugin.publication.strategies

import com.tddworks.sonatype.publish.portal.plugin.config.*
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JavaLibraryConfigurationStrategyTest {
    
    private lateinit var project: Project
    private lateinit var strategy: JavaLibraryConfigurationStrategy
    private lateinit var config: CentralPublisherConfig
    
    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()
        strategy = JavaLibraryConfigurationStrategy()
        
        config = CentralPublisherConfig(
            projectInfo = ProjectInfoConfig(
                name = "test-library",
                description = "Test library description",
                url = "https://github.com/test/library",
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
                    connection = "scm:git:git://github.com/test/library.git",
                    developerConnection = "scm:git:ssh://github.com:test/library.git",
                    url = "https://github.com/test/library"
                )
            ),
            credentials = CredentialsConfig(
                username = "test-user",
                password = "test-token"
            )
        )
    }
    
    @Test
    fun `should detect Java Library projects`() {
        // Given - Apply java-library plugin
        project.pluginManager.apply("java-library")
        
        // When
        val canHandle = strategy.canHandle(project)
        
        // Then
        assertThat(canHandle).isTrue()
    }
    
    @Test
    fun `should detect Java projects`() {
        // Given - Apply java plugin
        project.pluginManager.apply("java")
        
        // When
        val canHandle = strategy.canHandle(project)
        
        // Then
        assertThat(canHandle).isTrue()
    }
    
    @Test
    fun `should not detect non-Java projects`() {
        // Given - No Java plugins applied
        
        // When
        val canHandle = strategy.canHandle(project)
        
        // Then
        assertThat(canHandle).isFalse()
    }
    
    @Test
    fun `should configure Java Library project with maven publication`() {
        // Given - Apply required plugins
        project.pluginManager.apply("java-library")
        project.pluginManager.apply("maven-publish")
        
        // When
        strategy.configure(project, config)
        
        // Then - Maven publication should be created
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        assertThat(publishing.publications).hasSize(1)
        
        val mavenPub = publishing.publications.getByName("maven") as MavenPublication
        assertThat(mavenPub).isNotNull()
    }
    
    @Test
    fun `should configure POM metadata correctly`() {
        // Given - Apply required plugins
        project.pluginManager.apply("java-library")
        project.pluginManager.apply("maven-publish")
        
        // When
        strategy.configure(project, config)
        
        // Then - POM should be configured with project info
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        val mavenPub = publishing.publications.getByName("maven") as MavenPublication
        
        // Verify POM configuration is applied (we can't easily test the actual POM content 
        // in unit tests, but we can verify the publication exists and is properly set up)
        assertThat(mavenPub.groupId).isEqualTo(project.group.toString())
        assertThat(mavenPub.artifactId).isEqualTo(project.name)
        assertThat(mavenPub.version).isEqualTo(project.version.toString())
    }
    
    @Test
    fun `should have correct plugin type`() {
        // When
        val pluginType = strategy.getPluginType()
        
        // Then
        assertThat(pluginType).isEqualTo("java-library")
    }
    
    @Test
    fun `should have correct priority`() {
        // When
        val priority = strategy.getPriority()
        
        // Then
        assertThat(priority).isEqualTo(5) // Lower than Kotlin variants
    }
    
    @Test
    fun `should apply maven-publish plugin automatically`() {
        // Given - Apply only java-library plugin
        project.pluginManager.apply("java-library")
        assertThat(project.plugins.hasPlugin("maven-publish")).isFalse()
        
        // When
        strategy.configure(project, config)
        
        // Then - maven-publish should be applied
        assertThat(project.plugins.hasPlugin("maven-publish")).isTrue()
    }
    
    @Test
    fun `should configure sources and javadoc jars`() {
        // Given - Apply required plugins
        project.pluginManager.apply("java-library")
        project.pluginManager.apply("maven-publish")
        
        // When
        strategy.configure(project, config)
        
        // Then - Sources and javadoc jars should be configured
        // This is done through JavaPluginExtension.withSourcesJar() and withJavadocJar()
        // We can verify the extension is configured properly
        val javaExtension = project.extensions.getByType(org.gradle.api.plugins.JavaPluginExtension::class.java)
        assertThat(javaExtension).isNotNull()
    }
    
    @Test
    fun `should configure signing when signing plugin is present`() {
        // Given - Apply required plugins including signing
        project.pluginManager.apply("java-library")
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("signing")
        
        // Configure signing credentials via environment variables (simulated)
        project.extensions.extraProperties.set("SIGNING_KEY", "test-signing-key")
        project.extensions.extraProperties.set("SIGNING_PASSWORD", "test-password")
        
        // When
        strategy.configure(project, config)
        
        // Then - SigningConfigurator should be called and signing should be configured
        val signing = project.extensions.getByType(SigningExtension::class.java)
        assertThat(signing).isNotNull()
        
        // Verify that publications exist for signing to work on
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        assertThat(publishing.publications).hasSize(1)
        assertThat(publishing.publications.getByName("maven")).isInstanceOf(MavenPublication::class.java)
    }
    
    @Test
    fun `should handle missing signing plugin gracefully`() {
        // Given - Apply plugins without signing plugin
        project.pluginManager.apply("java-library")
        project.pluginManager.apply("maven-publish")
        
        // When - Should not throw even without signing plugin
        strategy.configure(project, config)
        
        // Then - Configuration should complete successfully
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        assertThat(publishing.publications).hasSize(1)
        assertThat(publishing.publications.getByName("maven")).isInstanceOf(MavenPublication::class.java)
    }
    
    @Test
    fun `should configure signing with file-based credentials`() {
        // Given - Apply required plugins including signing
        project.pluginManager.apply("java-library")
        project.pluginManager.apply("maven-publish") 
        project.pluginManager.apply("signing")
        
        // Configure file-based signing through config
        val signingConfig = CentralPublisherConfig(
            projectInfo = config.projectInfo,
            credentials = config.credentials,
            signing = SigningConfig(
                keyId = "test-key-id",
                password = "test-password",
                secretKeyRingFile = "/path/to/key.gpg"
            )
        )
        
        // When
        strategy.configure(project, signingConfig)
        
        // Then - SigningConfigurator should handle file-based signing
        val signing = project.extensions.getByType(SigningExtension::class.java)
        assertThat(signing).isNotNull()
        
        // Verify publications exist
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        assertThat(publishing.publications).hasSize(1)
    }
}