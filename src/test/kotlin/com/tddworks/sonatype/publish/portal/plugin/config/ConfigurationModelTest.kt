package com.tddworks.sonatype.publish.portal.plugin.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy

class ConfigurationModelTest {

    private lateinit var configBuilder: CentralPublisherConfigBuilder

    @BeforeEach
    fun setup() {
        configBuilder = CentralPublisherConfigBuilder()
    }

    @Test
    fun `should create minimal configuration with defaults`() {
        // Given/When
        val config = configBuilder.build()

        // Then
        assertThat(config.publishing.autoPublish).isFalse
        assertThat(config.publishing.dryRun).isFalse  
        assertThat(config.publishing.aggregation).isTrue // Default to aggregation
        assertThat(config.validation.enabled).isTrue
        assertThat(config.validation.strictMode).isFalse
    }

    @Test
    fun `should configure credentials section`() {
        // Given
        val config = configBuilder
            .credentials {
                username = "test-user"
                password = "test-password"
                loadFromEnvironment = true
            }
            .build()

        // Then
        assertThat(config.credentials.username).isEqualTo("test-user")
        assertThat(config.credentials.password).isEqualTo("test-password")
        assertThat(config.credentials.loadFromEnvironment).isTrue
    }

    @Test
    fun `should configure project information section`() {
        // Given
        val config = configBuilder
            .projectInfo {
                name = "my-library"
                description = "An awesome library"
                url = "https://github.com/user/my-library"
                scm {
                    url = "https://github.com/user/my-library"
                    connection = "scm:git:git://github.com/user/my-library.git"
                    developerConnection = "scm:git:ssh://github.com/user/my-library.git"
                }
                license {
                    name = "MIT License"
                    url = "https://opensource.org/licenses/MIT"
                    distribution = "repo"
                }
                developer {
                    id = "johndoe"
                    name = "John Doe"
                    email = "john@example.com"
                    organization = "Example Org"
                    organizationUrl = "https://example.com"
                }
            }
            .build()

        // Then
        assertThat(config.projectInfo.name).isEqualTo("my-library")
        assertThat(config.projectInfo.description).isEqualTo("An awesome library")
        assertThat(config.projectInfo.url).isEqualTo("https://github.com/user/my-library")
        
        // SCM info
        assertThat(config.projectInfo.scm.url).isEqualTo("https://github.com/user/my-library")
        assertThat(config.projectInfo.scm.connection).isEqualTo("scm:git:git://github.com/user/my-library.git")
        
        // License info
        assertThat(config.projectInfo.license.name).isEqualTo("MIT License")
        assertThat(config.projectInfo.license.url).isEqualTo("https://opensource.org/licenses/MIT")
        
        // Developer info
        assertThat(config.projectInfo.developers).hasSize(1)
        assertThat(config.projectInfo.developers[0].name).isEqualTo("John Doe")
        assertThat(config.projectInfo.developers[0].email).isEqualTo("john@example.com")
    }

    @Test
    fun `should configure signing section`() {
        // Given
        val config = configBuilder
            .signing {
                keyId = "ABCD1234"
                password = "signing-password"
                secretKeyRingFile = "/path/to/key"
                useGpgAgent = false
                autoDetect = true
            }
            .build()

        // Then
        assertThat(config.signing.keyId).isEqualTo("ABCD1234")
        assertThat(config.signing.password).isEqualTo("signing-password")
        assertThat(config.signing.secretKeyRingFile).isEqualTo("/path/to/key")
        assertThat(config.signing.useGpgAgent).isFalse
        assertThat(config.signing.autoDetect).isTrue
    }

    @Test
    fun `should configure publishing section`() {
        // Given
        val config = configBuilder
            .publishing {
                autoPublish = true
                dryRun = false
                aggregation = false
                publications = listOf("maven", "kotlinMultiplatform") 
                excludeModules = listOf("test-module")
            }
            .build()

        // Then
        assertThat(config.publishing.autoPublish).isTrue
        assertThat(config.publishing.dryRun).isFalse
        assertThat(config.publishing.aggregation).isFalse
        assertThat(config.publishing.publications).containsExactly("maven", "kotlinMultiplatform")
        assertThat(config.publishing.excludeModules).containsExactly("test-module")
    }

    @Test
    fun `should validate required configuration`() {
        // Given
        val invalidConfig = configBuilder
            .credentials {
                username = ""  // Invalid: empty username
                password = "test-password"
            }
            .build()

        // When/Then
        assertThatThrownBy { invalidConfig.validate() }
            .isInstanceOf(ConfigurationException::class.java)
            .hasMessageContaining("Username cannot be empty")
    }

    @Test
    fun `should support configuration serialization`() {
        // Given
        val originalConfig = configBuilder
            .credentials {
                username = "test-user"
                password = "test-password"
            }
            .projectInfo {
                name = "test-project"
                description = "Test description"
            }
            .build()

        // When
        val serialized = originalConfig.serialize()
        val deserializedConfig = CentralPublisherConfig.deserialize(serialized)

        // Then
        assertThat(deserializedConfig.credentials.username).isEqualTo("test-user")
        assertThat(deserializedConfig.projectInfo.name).isEqualTo("test-project")
        assertThat(deserializedConfig.projectInfo.description).isEqualTo("Test description")
    }

    @Test
    fun `should support configuration merging`() {
        // Given
        val baseConfig = configBuilder
            .credentials {
                username = "base-user"
                password = "base-password"
            }
            .publishing {
                autoPublish = false
            }
            .build()

        val overrideConfig = configBuilder
            .credentials {
                username = "override-user"
                // password not specified - should keep base value
            }
            .publishing {
                autoPublish = true
                dryRun = true
            }
            .build()

        // When
        val mergedConfig = baseConfig.mergeWith(overrideConfig)

        // Then
        assertThat(mergedConfig.credentials.username).isEqualTo("override-user")
        assertThat(mergedConfig.credentials.password).isEqualTo("base-password") // Kept from base
        assertThat(mergedConfig.publishing.autoPublish).isTrue // Overridden
        assertThat(mergedConfig.publishing.dryRun).isTrue // New from override
    }

    @Test
    fun `should track configuration sources`() {
        // Given
        val config = configBuilder
            .credentials {
                username = "test-user"
                password = "test-password"
            }
            .withSource(ConfigurationSource.DSL)
            .build()

        // Then
        assertThat(config.metadata.sources).contains(ConfigurationSource.DSL)
        assertThat(config.metadata.lastModified).isNotNull
        assertThat(config.metadata.version).isEqualTo("1.0.0") // Current config version
    }

    @Test
    fun `should support auto-detection settings`() {
        // Given
        val config = configBuilder
            .autoDetection {
                projectInfo = true
                credentials = false
                signing = true
                gitInfo = true
            }
            .build()

        // Then
        assertThat(config.autoDetection.projectInfo).isTrue
        assertThat(config.autoDetection.credentials).isFalse
        assertThat(config.autoDetection.signing).isTrue
        assertThat(config.autoDetection.gitInfo).isTrue
    }
}