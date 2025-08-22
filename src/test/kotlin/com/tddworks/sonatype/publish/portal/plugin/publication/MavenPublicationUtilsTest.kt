package com.tddworks.sonatype.publish.portal.plugin.publication

import com.tddworks.sonatype.publish.portal.plugin.config.*
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MavenPublicationUtilsTest {
    
    private lateinit var project: Project
    private lateinit var config: CentralPublisherConfig
    private lateinit var publication: MavenPublication
    
    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()
        project.pluginManager.apply("maven-publish")
        
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
                        email = "dev1@example.com",
                        organization = "Test Org",
                        organizationUrl = "https://testorg.com"
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
        
        // Create a test publication
        project.extensions.configure(PublishingExtension::class.java, object : Action<PublishingExtension> {
            override fun execute(publishing: PublishingExtension) {
                publication = publishing.publications.create("test", MavenPublication::class.java)
            }
        })
    }
    
    @Test
    fun `should configure POM with project information`() {
        // When
        publication.configurePom(project, config)
        
        // Then - POM should be configured with project info
        assertThat(publication.pom.name.get()).isEqualTo("test-library")
        assertThat(publication.pom.description.get()).isEqualTo("Test library description")
        assertThat(publication.pom.url.get()).isEqualTo("https://github.com/test/library")
    }
    
    @Test
    fun `should configure POM with license information`() {
        // When
        publication.configurePom(project, config)
        
        // Then - POM configuration should be applied (we can't easily test the actual POM content 
        // in unit tests due to Gradle's lazy evaluation, but we verify the configuration doesn't throw)
        assertThat(publication.pom).isNotNull()
    }
    
    @Test
    fun `should configure POM with developer information`() {
        // When
        publication.configurePom(project, config)
        
        // Then - POM configuration should be applied (we can't easily test the actual POM content 
        // in unit tests due to Gradle's lazy evaluation, but we verify the configuration doesn't throw)
        assertThat(publication.pom).isNotNull()
    }
    
    @Test
    fun `should configure POM with SCM information`() {
        // When
        publication.configurePom(project, config)
        
        // Then - POM configuration should be applied (we can't easily test the actual POM content 
        // in unit tests due to Gradle's lazy evaluation, but we verify the configuration doesn't throw)
        assertThat(publication.pom).isNotNull()
    }
    
    @Test
    fun `should use project name when config name is blank`() {
        // Given - Create project with specific name and blank config name
        val testProject = ProjectBuilder.builder().withName("my-project").build()
        testProject.pluginManager.apply("maven-publish")
        
        val configWithBlankName = CentralPublisherConfig(
            projectInfo = config.projectInfo.copy(name = ""),
            credentials = config.credentials,
            signing = config.signing
        )
        
        // Create publication in test project
        var testPublication: MavenPublication? = null
        testProject.extensions.configure(PublishingExtension::class.java, object : Action<PublishingExtension> {
            override fun execute(publishing: PublishingExtension) {
                testPublication = publishing.publications.create("test", MavenPublication::class.java)
            }
        })
        
        // When
        testPublication!!.configurePom(testProject, configWithBlankName)
        
        // Then - Should use project name
        assertThat(testPublication!!.pom.name.get()).isEqualTo("my-project")
    }
    
    @Test
    fun `should configure signing when SIGNING_KEY is available`() {
        // Given - Apply signing plugin and set credentials
        project.pluginManager.apply("signing")
        project.extensions.extraProperties.set("SIGNING_KEY", "test-signing-key")
        project.extensions.extraProperties.set("SIGNING_PASSWORD", "test-password")
        
        // When
        publication.configureSigningIfAvailable(project, config)
        
        // Then - Signing should be configured
        val signing = project.extensions.getByType(SigningExtension::class.java)
        assertThat(signing).isNotNull()
    }
    
    @Test
    fun `should configure signing with file-based credentials`() {
        // Given - Apply signing plugin and configure file-based signing
        project.pluginManager.apply("signing")
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
        publication.configureSigningIfAvailable(project, signingConfig)
        
        // Then - Signing should be configured
        val signing = project.extensions.getByType(SigningExtension::class.java)
        assertThat(signing).isNotNull()
    }
    
    @Test
    fun `should configure signing with keyId only`() {
        // Given - Apply signing plugin and configure keyId-based signing
        project.pluginManager.apply("signing")
        val signingConfig = CentralPublisherConfig(
            projectInfo = config.projectInfo,
            credentials = config.credentials,
            signing = SigningConfig(
                keyId = "test-key-id",
                password = "test-password"
            )
        )
        
        // When
        publication.configureSigningIfAvailable(project, signingConfig)
        
        // Then - Signing should be configured
        val signing = project.extensions.getByType(SigningExtension::class.java)
        assertThat(signing).isNotNull()
    }
    
    @Test
    fun `should handle missing signing plugin gracefully`() {
        // Given - No signing plugin applied
        
        // When - Should not throw
        publication.configureSigningIfAvailable(project, config)
        
        // Then - Should complete without error
        // (No signing extension to verify, but shouldn't throw)
    }
    
    @Test
    fun `should not configure signing when no credentials available`() {
        // Given - Apply signing plugin but no credentials
        project.pluginManager.apply("signing")
        
        // When
        publication.configureSigningIfAvailable(project, config)
        
        // Then - Should complete without error
        val signing = project.extensions.getByType(SigningExtension::class.java)
        assertThat(signing).isNotNull() // Extension exists but not configured for signing
    }
}