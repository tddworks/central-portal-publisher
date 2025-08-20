package com.tddworks.sonatype.publish.portal.plugin.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.util.Properties

class ConfigurationSourceManagerTest {

    private lateinit var project: Project
    private lateinit var sourceManager: ConfigurationSourceManager
    private lateinit var tempPropertiesFile: File

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
        sourceManager = ConfigurationSourceManager(project)
        
        // Create temporary gradle.properties for testing
        tempPropertiesFile = File.createTempFile("gradle", ".properties")
    }
    
    @AfterEach
    fun cleanup() {
        tempPropertiesFile.delete()
        // Clean up environment variables set during tests
        System.clearProperty("SONATYPE_USERNAME")
        System.clearProperty("SONATYPE_PASSWORD")
    }

    @Test
    fun `should load configuration from DSL source`() {
        // Given
        val dslConfig = CentralPublisherConfigBuilder()
            .credentials {
                username = "dsl-user"
                password = "dsl-password"
            }
            .projectInfo {
                name = "dsl-project"
                description = "From DSL"
            }
            .withSource(ConfigurationSource.DSL)
            .build()

        // When
        val result = sourceManager.loadConfiguration(dslConfig)

        // Then
        assertThat(result.credentials.username).isEqualTo("dsl-user")
        assertThat(result.projectInfo.name).isEqualTo("dsl-project")
        assertThat(result.metadata.sources).contains(ConfigurationSource.DSL)
    }

    @Test
    fun `should load configuration from properties file`() {
        // Given
        val properties = Properties().apply {
            setProperty("SONATYPE_USERNAME", "props-user")
            setProperty("SONATYPE_PASSWORD", "props-password")
            setProperty("POM_NAME", "props-project")
            setProperty("POM_DESCRIPTION", "From Properties")
            setProperty("POM_URL", "https://github.com/example/props-project")
        }
        tempPropertiesFile.writer().use { properties.store(it, "Test properties") }

        // When
        val result = sourceManager.loadFromProperties(tempPropertiesFile.absolutePath)

        // Then
        assertThat(result.credentials.username).isEqualTo("props-user")
        assertThat(result.credentials.password).isEqualTo("props-password")
        assertThat(result.projectInfo.name).isEqualTo("props-project")
        assertThat(result.projectInfo.description).isEqualTo("From Properties")
        assertThat(result.projectInfo.url).isEqualTo("https://github.com/example/props-project")
        assertThat(result.metadata.sources).contains(ConfigurationSource.PROPERTIES)
    }

    @Test
    fun `should load configuration from environment variables`() {
        // Given
        System.setProperty("SONATYPE_USERNAME", "env-user")
        System.setProperty("SONATYPE_PASSWORD", "env-password")
        System.setProperty("SIGNING_KEY", "env-signing-key")
        System.setProperty("SIGNING_PASSWORD", "env-signing-password")

        // When
        val result = sourceManager.loadFromEnvironment()

        // Then
        assertThat(result.credentials.username).isEqualTo("env-user")
        assertThat(result.credentials.password).isEqualTo("env-password")
        assertThat(result.signing.keyId).isEqualTo("env-signing-key")
        assertThat(result.signing.password).isEqualTo("env-signing-password")
        assertThat(result.metadata.sources).contains(ConfigurationSource.ENVIRONMENT)
    }

    @Test
    fun `should merge configurations with correct precedence`() {
        // Given - Set up multiple sources
        System.setProperty("SONATYPE_USERNAME", "env-user")
        System.setProperty("SONATYPE_PASSWORD", "env-password")
        
        val properties = Properties().apply {
            setProperty("SONATYPE_USERNAME", "props-user") // Should override env
            setProperty("POM_NAME", "props-project")
        }
        tempPropertiesFile.writer().use { properties.store(it, "Test properties") }

        val dslConfig = CentralPublisherConfigBuilder()
            .credentials {
                username = "dsl-user" // Should override all others
            }
            .projectInfo {
                description = "DSL description"
            }
            .withSource(ConfigurationSource.DSL)
            .build()

        // When - Load with precedence: DSL > Properties > Environment > Defaults
        val result = sourceManager.loadConfigurationWithPrecedence(
            dslConfig = dslConfig,
            propertiesFile = tempPropertiesFile.absolutePath,
            enableAutoDetection = false
        )

        // Then
        assertThat(result.credentials.username).isEqualTo("dsl-user") // DSL wins
        assertThat(result.credentials.password).isEqualTo("env-password") // From env (props didn't have it)
        assertThat(result.projectInfo.name).isEqualTo("props-project") // From props (DSL didn't have it)
        assertThat(result.projectInfo.description).isEqualTo("DSL description") // DSL wins
        assertThat(result.metadata.sources).containsExactlyInAnyOrder(
            ConfigurationSource.DSL,
            ConfigurationSource.PROPERTIES, 
            ConfigurationSource.ENVIRONMENT,
            ConfigurationSource.SMART_DEFAULTS
        )
    }

    @Test
    fun `should handle missing properties file gracefully`() {
        // Given
        val nonExistentFile = "/path/that/does/not/exist/gradle.properties"

        // When
        val result = sourceManager.loadFromProperties(nonExistentFile)

        // Then
        assertThat(result.credentials.username).isEmpty()
        assertThat(result.credentials.password).isEmpty()
        assertThat(result.metadata.sources).contains(ConfigurationSource.PROPERTIES)
    }

    @Test
    fun `should support custom property mappings`() {
        // Given
        val properties = Properties().apply {
            // Test alternative property names
            setProperty("sonatype.username", "custom-user")
            setProperty("sonatype.password", "custom-password")
            setProperty("project.name", "custom-project")
            setProperty("project.description", "Custom description")
        }
        tempPropertiesFile.writer().use { properties.store(it, "Custom properties") }

        // When
        val result = sourceManager.loadFromProperties(
            filePath = tempPropertiesFile.absolutePath,
            propertyMappings = mapOf(
                "sonatype.username" to "credentials.username",
                "sonatype.password" to "credentials.password",
                "project.name" to "projectInfo.name",
                "project.description" to "projectInfo.description"
            )
        )

        // Then
        assertThat(result.credentials.username).isEqualTo("custom-user")
        assertThat(result.credentials.password).isEqualTo("custom-password")
        assertThat(result.projectInfo.name).isEqualTo("custom-project")
        assertThat(result.projectInfo.description).isEqualTo("Custom description")
    }

    @Test
    fun `should validate configuration sources during loading`() {
        // Given
        val properties = Properties().apply {
            setProperty("SONATYPE_USERNAME", "") // Invalid: empty username
            setProperty("SONATYPE_PASSWORD", "valid-password")
            setProperty("POM_NAME", "test-project") // Valid project name
            setProperty("POM_URL", "invalid-url") // Invalid: not HTTP/HTTPS
        }
        tempPropertiesFile.writer().use { properties.store(it, "Invalid properties") }

        // When
        val result = sourceManager.loadFromProperties(
            filePath = tempPropertiesFile.absolutePath,
            validateOnLoad = true
        )

        // Then - Should load but mark validation errors
        assertThat(result.credentials.username).isEmpty()
        assertThat(result.projectInfo.url).isEqualTo("invalid-url")
        // Validation errors should be tracked for later reporting
        assertThat(sourceManager.getValidationErrors()).hasSize(2)
        assertThat(sourceManager.getValidationErrors().map { it.message })
            .containsExactlyInAnyOrder(
                "Username cannot be empty",
                "Project URL must be a valid HTTP/HTTPS URL"
            )
    }

    @Test
    fun `should support configuration caching`() {
        // Given
        val properties = Properties().apply {
            setProperty("SONATYPE_USERNAME", "cached-user")
            setProperty("SONATYPE_PASSWORD", "cached-password")
        }
        tempPropertiesFile.writer().use { properties.store(it, "Cached properties") }

        // When - Load twice
        val result1 = sourceManager.loadFromProperties(tempPropertiesFile.absolutePath)
        val result2 = sourceManager.loadFromProperties(tempPropertiesFile.absolutePath)

        // Then
        assertThat(result1.credentials.username).isEqualTo("cached-user")
        assertThat(result2.credentials.username).isEqualTo("cached-user")
        assertThat(sourceManager.getCacheHitCount()).isEqualTo(1) // Second load was cached
    }

    @Test
    fun `should support live reloading when file changes`() {
        // Given
        var properties = Properties().apply {
            setProperty("SONATYPE_USERNAME", "original-user")
        }
        tempPropertiesFile.writer().use { properties.store(it, "Original") }

        val result1 = sourceManager.loadFromProperties(tempPropertiesFile.absolutePath)

        // When - Update file
        properties = Properties().apply {
            setProperty("SONATYPE_USERNAME", "updated-user")
        }
        tempPropertiesFile.writer().use { properties.store(it, "Updated") }

        val result2 = sourceManager.loadFromProperties(tempPropertiesFile.absolutePath)

        // Then
        assertThat(result1.credentials.username).isEqualTo("original-user")
        assertThat(result2.credentials.username).isEqualTo("updated-user")
    }

    @Test
    fun `should provide configuration source diagnostics`() {
        // Given
        System.setProperty("SONATYPE_USERNAME", "env-user")
        
        val dslConfig = CentralPublisherConfigBuilder()
            .credentials {
                username = "dsl-user"
                password = "dsl-password"
            }
            .withSource(ConfigurationSource.DSL)
            .build()

        // When
        val result = sourceManager.loadConfigurationWithPrecedence(
            dslConfig = dslConfig,
            enableAutoDetection = false
        )
        val diagnostics = sourceManager.getSourceDiagnostics()

        // Then
        assertThat(diagnostics.configurationSources).hasSize(3) // DSL + Environment + Smart Defaults
        assertThat(diagnostics.precedenceOrder).containsExactly(
            ConfigurationSource.DSL,
            ConfigurationSource.PROPERTIES,
            ConfigurationSource.ENVIRONMENT,
            ConfigurationSource.AUTO_DETECTED,
            ConfigurationSource.SMART_DEFAULTS,
            ConfigurationSource.DEFAULTS
        )
        assertThat(diagnostics.sourceValues["credentials.username"]).containsExactlyInAnyOrder(
            "dsl-user" to ConfigurationSource.DSL,
            "env-user" to ConfigurationSource.ENVIRONMENT
        )
        assertThat(diagnostics.finalValue("credentials.username")).isEqualTo("dsl-user")
    }
}