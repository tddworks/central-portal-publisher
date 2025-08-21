package com.tddworks.sonatype.publish.portal.plugin.dsl

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class CentralPublisherExtensionTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var extension: CentralPublisherExtension

    @BeforeEach
    fun setup() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-project")
            .build()
            
        extension = CentralPublisherExtension(project)
    }

    @Test
    fun `should return false for hasExplicitConfiguration initially`() {
        // Given - new extension with no configuration

        // Then
        assertThat(extension.hasExplicitConfiguration()).isFalse()
    }

    @Test
    fun `should return true after configuring credentials`() {
        // Given - extension with credentials configuration
        
        // When
        extension.credentials {
            username = "test-user"
            password = "test-password"
        }

        // Then
        assertThat(extension.hasExplicitConfiguration()).isTrue()
    }

    @Test
    fun `should return true after configuring project info`() {
        // Given - extension with project info configuration
        
        // When
        extension.projectInfo {
            name = "test-project"
            description = "Test Description"
            url = "https://github.com/test/test-project"
        }

        // Then
        assertThat(extension.hasExplicitConfiguration()).isTrue()
    }

    @Test
    fun `should return true after configuring signing`() {
        // Given - extension with signing configuration
        
        // When
        extension.signing {
            keyId = "test-key"
            password = "test-password"
            secretKeyRingFile = "~/.gnupg/secring.gpg"
        }

        // Then
        assertThat(extension.hasExplicitConfiguration()).isTrue()
    }

    @Test
    fun `should return true after configuring publishing`() {
        // Given - extension with publishing configuration
        
        // When
        extension.publishing {
            autoPublish = false
            aggregation = true
            dryRun = false
        }

        // Then
        assertThat(extension.hasExplicitConfiguration()).isTrue()
    }

    @Test
    fun `should return true after any configuration method is called`() {
        // Given - extension with any configuration
        
        // When - configure credentials first
        extension.credentials {
            username = "user1"
            password = "pass1"
        }
        
        // Then
        assertThat(extension.hasExplicitConfiguration()).isTrue()
        
        // When - configure additional sections
        extension.projectInfo {
            name = "project"
        }
        
        extension.signing {
            keyId = "key"
        }
        
        // Then - still true
        assertThat(extension.hasExplicitConfiguration()).isTrue()
    }

    @Test
    fun `should build configuration correctly with explicit values`() {
        // Given - extension with explicit configuration
        extension.credentials {
            username = "test-user"
            password = "test-password"
        }
        
        extension.projectInfo {
            name = "my-project"
            description = "My Project Description"
            url = "https://github.com/me/my-project"
            
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
            }
            
            developer {
                id = "devid"
                name = "Developer Name"
                email = "dev@example.com"
            }
            
            scm {
                url = "https://github.com/me/my-project"
                connection = "scm:git:git://github.com/me/my-project.git"
                developerConnection = "scm:git:ssh://github.com/me/my-project.git"
            }
        }
        
        extension.signing {
            keyId = "signing-key"
            password = "signing-password"
            secretKeyRingFile = "/path/to/keyring"
        }
        
        extension.publishing {
            autoPublish = true
            aggregation = false
            dryRun = true
        }

        // When
        val config = extension.build()

        // Then - explicit configuration should be present
        assertThat(extension.hasExplicitConfiguration()).isTrue()
        
        // And configuration should contain explicit values
        assertThat(config.credentials.username).isEqualTo("test-user")
        assertThat(config.credentials.password).isEqualTo("test-password")
        assertThat(config.projectInfo.name).isEqualTo("my-project")
        assertThat(config.projectInfo.description).isEqualTo("My Project Description")
        assertThat(config.projectInfo.url).isEqualTo("https://github.com/me/my-project")
        assertThat(config.projectInfo.license.name).isEqualTo("MIT License")
        assertThat(config.projectInfo.developers).hasSize(1)
        assertThat(config.projectInfo.developers[0].id).isEqualTo("devid")
        assertThat(config.projectInfo.developers[0].name).isEqualTo("Developer Name")
        assertThat(config.projectInfo.developers[0].email).isEqualTo("dev@example.com")
        assertThat(config.signing.keyId).isEqualTo("signing-key")
        assertThat(config.signing.password).isEqualTo("signing-password")
        assertThat(config.signing.secretKeyRingFile).isEqualTo("/path/to/keyring")
        assertThat(config.publishing.autoPublish).isTrue()
        assertThat(config.publishing.aggregation).isFalse()
        assertThat(config.publishing.dryRun).isTrue()
    }

    @Test
    fun `should work with empty configuration blocks`() {
        // Given - extension with empty configuration blocks
        
        // When
        extension.credentials {
            // Empty block, no properties set
        }

        // Then - should still be considered explicit configuration
        assertThat(extension.hasExplicitConfiguration()).isTrue()
        
        // And configuration should still build successfully
        val config = extension.build()
        assertThat(config).isNotNull
    }

    @Test
    fun `should work with partial configuration`() {
        // Given - extension with partial configuration
        
        // When
        extension.projectInfo {
            name = "partial-project"
            // Only set name, leave other fields with defaults
        }

        // Then
        assertThat(extension.hasExplicitConfiguration()).isTrue()
        
        val config = extension.build()
        assertThat(config.projectInfo.name).isEqualTo("partial-project")
        // Other fields should have default values
        assertThat(config.projectInfo.description).isNotNull
        assertThat(config.projectInfo.url).isNotNull
    }
}