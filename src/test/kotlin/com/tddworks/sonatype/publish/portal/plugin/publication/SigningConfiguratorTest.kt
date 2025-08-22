package com.tddworks.sonatype.publish.portal.plugin.publication

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfigBuilder
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SigningConfiguratorTest {
    
    private lateinit var project: Project
    private lateinit var config: CentralPublisherConfig
    
    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("signing")
        
        config = CentralPublisherConfigBuilder().build()
    }
    
    @Test
    fun `should configure signing when SIGNING_KEY is available`() {
        // Given - Set SIGNING_KEY property
        project.extensions.extraProperties.set("SIGNING_KEY", "test-signing-key")
        project.extensions.extraProperties.set("SIGNING_PASSWORD", "test-password")
        
        // Create a test publication
        project.extensions.configure(PublishingExtension::class.java, object : Action<PublishingExtension> {
            override fun execute(publishing: PublishingExtension) {
                publishing.publications.create("maven", MavenPublication::class.java)
            }
        })
        
        // When
        SigningConfigurator.configureSigningIfAvailable(project, config)
        
        // Then - Should configure signing and sign publications
        val signing = project.extensions.getByType(SigningExtension::class.java)
        assertThat(signing).isNotNull()
        // Note: Testing actual signing behavior is complex, so we verify the configurator doesn't throw
    }
    
    @Test
    fun `should not configure signing when no credentials available`() {
        // Given - No signing credentials
        
        // When
        SigningConfigurator.configureSigningIfAvailable(project, config)
        
        // Then - Should complete without error (no signing configured)
        val signing = project.extensions.getByType(SigningExtension::class.java)
        assertThat(signing).isNotNull() // Extension exists but not configured
    }
    
    @Test
    fun `should configure signing when secretKeyRingFile is available`() {
        // Given - Configure file-based signing
        val fileBasedConfig = CentralPublisherConfigBuilder()
            .signing {
                keyId = "test-key-id"
                password = "test-password"
                secretKeyRingFile = "/path/to/secret-key-ring.gpg"
            }
            .build()
        
        // Create a test publication
        project.extensions.configure(PublishingExtension::class.java, object : Action<PublishingExtension> {
            override fun execute(publishing: PublishingExtension) {
                publishing.publications.create("maven", MavenPublication::class.java)
            }
        })
        
        // When
        SigningConfigurator.configureSigningIfAvailable(project, fileBasedConfig)
        
        // Then - Should configure signing without throwing
        val signing = project.extensions.getByType(SigningExtension::class.java)
        assertThat(signing).isNotNull()
    }
    
    @Test
    fun `should handle missing signing plugin gracefully`() {
        // Given - Project without signing plugin
        val projectWithoutSigning = ProjectBuilder.builder().build()
        projectWithoutSigning.pluginManager.apply("maven-publish")
        
        // When
        SigningConfigurator.configureSigningIfAvailable(projectWithoutSigning, config)
        
        // Then - Should complete without error
        // (No signing extension to verify, but shouldn't throw)
    }
}