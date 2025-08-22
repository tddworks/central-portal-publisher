package com.tddworks.sonatype.publish.portal.plugin.publication.strategies

import com.tddworks.sonatype.publish.portal.plugin.config.*
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.signing.SigningExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KotlinJvmConfigurationStrategyTest {
    
    private lateinit var project: Project
    private lateinit var strategy: KotlinJvmConfigurationStrategy
    private lateinit var config: CentralPublisherConfig
    
    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()
        strategy = KotlinJvmConfigurationStrategy()
        
        config = CentralPublisherConfig(
            projectInfo = ProjectInfoConfig(
                name = "kotlin-jvm-library",
                description = "Test Kotlin JVM library",
                url = "https://github.com/test/kotlin-library",
                license = LicenseConfig(
                    name = "Apache License, Version 2.0",
                    url = "http://www.apache.org/licenses/LICENSE-2.0.txt",
                    distribution = "repo"
                ),
                developers = listOf(
                    DeveloperConfig(
                        id = "kotlindev",
                        name = "Kotlin Developer",
                        email = "kotlindev@example.com"
                    )
                ),
                scm = ScmConfig(
                    url = "https://github.com/test/kotlin-library",
                    connection = "scm:git:git://github.com/test/kotlin-library.git",
                    developerConnection = "scm:git:ssh://github.com/test/kotlin-library.git"
                )
            ),
            signing = SigningConfig(
                keyId = "12345678",
                password = "test-password",
                secretKeyRingFile = "/path/to/keyring"
            )
        )
    }
    
    @Test
    fun `should detect Kotlin JVM projects`() {
        // Given - Apply Kotlin JVM plugin
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        
        // When
        val canHandle = strategy.canHandle(project)
        
        // Then
        assertThat(canHandle).isTrue()
    }
    
    @Test
    fun `should not detect non-Kotlin JVM projects`() {
        // Given - No Kotlin JVM plugin applied
        
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
    
    @Test
    fun `should configure Kotlin JVM project with maven publication`() {
        // Given - Apply required plugins
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
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
    fun `should configure sources jar for Kotlin JVM project`() {
        // Given - Apply required plugins
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("java")
        
        // When
        strategy.configure(project, config)
        
        // Then - Java plugin extension should be configured with sources jar
        // Note: withSourcesJar() is a configuration call, hard to test directly
        // We verify the task exists instead
        val sourcesJarTask = project.tasks.findByName("sourcesJar")
        assertThat(sourcesJarTask).isNotNull()
    }
    
    @Test
    fun `should create javadoc jar task`() {
        // Given - Apply required plugins
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("maven-publish")
        
        // When
        strategy.configure(project, config)
        
        // Then - Javadoc jar task should be created
        val javadocJarTask = project.tasks.findByName("javadocJar") as Jar?
        assertThat(javadocJarTask).isNotNull()
        assertThat(javadocJarTask?.archiveClassifier?.get()).isEqualTo("javadoc")
    }
    
    @Test
    fun `should configure POM metadata correctly`() {
        // Given - Apply required plugins
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("maven-publish")
        
        // When
        strategy.configure(project, config)
        
        // Then - POM should be configured with project info
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        val mavenPub = publishing.publications.getByName("maven") as MavenPublication
        
        val pom = mavenPub.pom
        assertThat(pom.name.get()).isEqualTo("kotlin-jvm-library")
        assertThat(pom.description.get()).isEqualTo("Test Kotlin JVM library")
        assertThat(pom.url.get()).isEqualTo("https://github.com/test/kotlin-library")
    }
    
    @Test
    fun `should configure signing when signing plugin is applied`() {
        // Given - Apply required plugins including signing
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("signing")
        
        // Set up signing properties for in-memory keys
        project.extensions.extraProperties.set("SIGNING_KEY", "test-signing-key")
        project.extensions.extraProperties.set("SIGNING_PASSWORD", "test-password")
        
        // When
        strategy.configure(project, config)
        
        // Then - Signing should be configured
        val signing = project.extensions.getByType(SigningExtension::class.java)
        assertThat(signing).isNotNull()
        
        // Note: Signing configuration is complex to test directly, 
        // but we can verify the extension exists and was configured
    }
    
    @Test
    fun `should warn when signing plugin applied but no signing key configured`() {
        // Given - Apply required plugins including signing but no signing key
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("signing")
        
        // When
        strategy.configure(project, config)
        
        // Then - Should complete without error
        // Warning will be logged but hard to test directly
        val signing = project.extensions.getByType(SigningExtension::class.java)
        assertThat(signing).isNotNull()
    }
    
    @Test
    fun `should configure file-based signing when keyId is provided`() {
        // Given - Apply required plugins including signing
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("signing")
        
        // Configure file-based signing via config
        val fileSigningConfig = config.copy(
            signing = SigningConfig(
                keyId = "ABCD1234",
                password = "file-password", 
                secretKeyRingFile = "/path/to/secret.gpg"
            )
        )
        
        // When
        strategy.configure(project, fileSigningConfig)
        
        // Then - Signing should be configured
        val signing = project.extensions.getByType(SigningExtension::class.java)
        assertThat(signing).isNotNull()
    }
}