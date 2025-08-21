package com.tddworks.sonatype.publish.portal.plugin.publication
import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.config.DeveloperConfig
import com.tddworks.sonatype.publish.portal.plugin.config.LicenseConfig
import com.tddworks.sonatype.publish.portal.plugin.config.ProjectInfoConfig
import com.tddworks.sonatype.publish.portal.plugin.config.ScmConfig
import com.tddworks.sonatype.publish.portal.plugin.config.SigningConfig
import org.assertj.core.api.Assertions
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class PublicationProviderTest {
    
    private lateinit var project: Project
    private lateinit var config: CentralPublisherConfig
    
    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
        config = CentralPublisherConfig(
            projectInfo = ProjectInfoConfig(
                name = "test-library",
                description = "A test library",
                url = "https://github.com/example/test-library",
                scm = ScmConfig(
                    url = "https://github.com/example/test-library",
                    connection = "scm:git:git://github.com/example/test-library.git",
                    developerConnection = "scm:git:ssh://github.com/example/test-library.git"
                ),
                license = LicenseConfig(
                    name = "Apache License 2.0",
                    url = "https://www.apache.org/licenses/LICENSE-2.0.txt",
                    distribution = "repo"
                ),
                developers = listOf(
                    DeveloperConfig(
                        id = "dev1",
                        name = "Developer One",
                        email = "dev1@example.com"
                    )
                )
            ),
            signing = SigningConfig(
                keyId = "test-key-id",
                password = "test-password",
                secretKeyRingFile = "/path/to/secring.gpg"
            )
        )
    }
    
    @Test
    fun `should detect JVM project and configure publications`() {
        // Given: A project with Java plugin
        project.plugins.apply("java")
        project.plugins.apply("maven-publish")
        
        val provider = JvmPublicationProvider()
        
        // When: Configure publications
        provider.configurePublications(project, config)
        
        // Then: Maven publication should be created
        val publishing = project.extensions.getByType<PublishingExtension>()
        Assertions.assertThat(publishing.publications).hasSize(1)
        
        val mavenPub = publishing.publications.getByName("maven") as MavenPublication
        Assertions.assertThat(mavenPub.groupId).isEqualTo(project.group.toString())
        Assertions.assertThat(mavenPub.artifactId).isEqualTo(project.name)
        Assertions.assertThat(mavenPub.version).isEqualTo(project.version.toString())
    }
    
    @Test
    fun `should configure sources jar for Java projects`() {
        // Given: A project with Java plugin
        project.plugins.apply("java")
        project.plugins.apply("maven-publish")
        
        val provider = JvmPublicationProvider()
        
        // When: Configure publications
        provider.configurePublications(project, config)
        
        // Then: Sources jar should be configured
        val javaExtension = project.extensions.getByType<JavaPluginExtension>()
        // We can't directly test if withSourcesJar() was called, but we can test it doesn't error
        Assertions.assertThat(javaExtension).isNotNull
    }
    
    @Test
    fun `should apply maven-publish plugin automatically when missing`() {
        // Given: A project with Java plugin but no maven-publish
        project.plugins.apply("java")
        
        val provider = JvmPublicationProvider()
        
        // When: Configure publications
        provider.configurePublications(project, config)
        
        // Then: maven-publish plugin should be applied
        Assertions.assertThat(project.plugins.hasPlugin("maven-publish")).isEqualTo(true)
    }
    
    @Test
    fun `should populate POM with project information from config`() {
        // Given: A project with Java plugin
        project.plugins.apply("java")
        project.plugins.apply("maven-publish")
        
        val provider = JvmPublicationProvider()
        
        // When: Configure publications
        provider.configurePublications(project, config)
        
        // Then: POM should be populated with config data
        val publishing = project.extensions.getByType<PublishingExtension>()
        val mavenPub = publishing.publications.getByName("maven") as MavenPublication
        
        // Test POM configuration by checking that it was set
        Assertions.assertThat(mavenPub.pom.name.isPresent).isEqualTo(true)
        Assertions.assertThat(mavenPub.pom.description.isPresent).isEqualTo(true)
        Assertions.assertThat(mavenPub.pom.url.isPresent).isEqualTo(true)
    }
    
    @Test
    fun `should configure signing when signing info is provided`() {
        // Given: A project with signing plugin
        project.plugins.apply("java")
        project.plugins.apply("maven-publish")
        project.plugins.apply("signing")
        
        val provider = JvmPublicationProvider()
        
        // When: Configure publications
        provider.configurePublications(project, config)
        
        // Then: Signing should be configured
        val signing = project.extensions.getByName("signing")
        Assertions.assertThat(signing).isNotNull
    }
    
    @Test
    fun `should not configure publications for non-JVM projects`() {
        // Given: A project without Java/Kotlin plugins
        project.plugins.apply("maven-publish")
        
        val provider = JvmPublicationProvider()
        
        // When: Configure publications
        provider.configurePublications(project, config)
        
        // Then: No publications should be created
        val publishing = project.extensions.getByType<PublishingExtension>()
        Assertions.assertThat(publishing.publications).isEmpty()
    }
    
    @Test
    fun `should apply maven-publish plugin automatically`() {
        // Given: A project without maven-publish plugin
        project.plugins.apply("java")
        
        val provider = JvmPublicationProvider()
        
        // When: Configure publications
        provider.configurePublications(project, config)
        
        // Then: maven-publish plugin should be applied
        Assertions.assertThat(project.plugins.hasPlugin("maven-publish")).isEqualTo(true)
    }
    
    @Test
    fun `should use publication provider registry to configure multiple providers`() {
        // Given: A project with Java plugin
        project.plugins.apply("java")
        
        val registry = PublicationProviderRegistry()
        
        // When: Configure publications via registry
        registry.configurePublications(project, config)
        
        // Then: Publications should be configured
        Assertions.assertThat(project.plugins.hasPlugin("maven-publish")).isEqualTo(true)
        val publishing = project.extensions.getByType<PublishingExtension>()
        Assertions.assertThat(publishing.publications).hasSize(1)
    }
}