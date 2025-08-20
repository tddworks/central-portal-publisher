package com.tddworks.sonatype.publish.portal.plugin.dsl

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CentralPublisherDSLTest {
    
    private lateinit var project: Project
    
    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
    }
    
    @Test
    fun `should create basic DSL extension`() {
        // Given - Register the extension
        val extension = project.extensions.create("centralPublisher", CentralPublisherExtension::class.java, project)
        
        // When
        val foundExtension = project.extensions.findByName("centralPublisher")
        
        // Then - Extension should be available
        assertThat(foundExtension).isNotNull()
        assertThat(foundExtension).isSameAs(extension)
    }
    
    @Test
    fun `should support credentials DSL block`() {
        // Given - This is the DSL we want to support
        val config = project.run {
            centralPublisher {
                credentials {
                    username = "testuser"
                    password = "testpass"
                }
            }
        }
        
        // Then
        assertThat(config).isNotNull()
        assertThat(config.credentials.username).isEqualTo("testuser")
        assertThat(config.credentials.password).isEqualTo("testpass")
    }
    
    @Test
    fun `should support projectInfo DSL block`() {
        // Given - This is the DSL we want to support
        val config = project.run {
            centralPublisher {
                projectInfo {
                    name = "test-project"
                    description = "A test project"
                    url = "https://github.com/test/project"
                    
                    scm {
                        url = "https://github.com/test/project"
                        connection = "scm:git:https://github.com/test/project.git"
                        developerConnection = "scm:git:https://github.com/test/project.git"
                    }
                    
                    license {
                        name = "Apache License 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                        distribution = "repo"
                    }
                    
                    developer {
                        id = "dev1"
                        name = "Test Developer"
                        email = "dev@example.com"
                        organization = "Test Org"
                        organizationUrl = "https://test-org.com"
                    }
                }
            }
        }
        
        // Then
        assertThat(config.projectInfo.name).isEqualTo("test-project")
        assertThat(config.projectInfo.description).isEqualTo("A test project")
        assertThat(config.projectInfo.url).isEqualTo("https://github.com/test/project")
        assertThat(config.projectInfo.scm.url).isEqualTo("https://github.com/test/project")
        assertThat(config.projectInfo.license.name).isEqualTo("Apache License 2.0")
        assertThat(config.projectInfo.developers).hasSize(1)
        assertThat(config.projectInfo.developers[0].name).isEqualTo("Test Developer")
    }
    
    @Test
    fun `should support signing DSL block`() {
        // Given
        val config = project.run {
            centralPublisher {
                signing {
                    keyId = "12345678"
                    password = "signingpass"
                    secretKeyRingFile = "/path/to/keyring.gpg"
                }
            }
        }
        
        // Then
        assertThat(config.signing.keyId).isEqualTo("12345678")
        assertThat(config.signing.password).isEqualTo("signingpass")
        assertThat(config.signing.secretKeyRingFile).isEqualTo("/path/to/keyring.gpg")
    }
    
    @Test
    fun `should support publishing DSL block`() {
        // Given
        val config = project.run {
            centralPublisher {
                publishing {
                    autoPublish = true
                    aggregation = false
                    dryRun = true
                }
            }
        }
        
        // Then
        assertThat(config.publishing.autoPublish).isTrue()
        assertThat(config.publishing.aggregation).isFalse()
        assertThat(config.publishing.dryRun).isTrue()
    }
    
    @Test
    fun `should support nested DSL blocks`() {
        // Given - Complete DSL configuration
        val config = project.run {
            centralPublisher {
                credentials {
                    username = "fulluser"
                    password = "fullpass"
                }
                
                projectInfo {
                    name = "full-project"
                    description = "A complete project"
                    url = "https://github.com/full/project"
                    
                    scm {
                        url = "https://github.com/full/project"
                        connection = "scm:git:https://github.com/full/project.git"
                    }
                    
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                    }
                    
                    developer {
                        name = "Full Developer"
                        email = "full@example.com"
                    }
                }
                
                signing {
                    keyId = "ABCD1234"
                    password = "fullsigning"
                }
                
                publishing {
                    autoPublish = false
                    dryRun = false
                }
            }
        }
        
        // Then - All sections should be configured
        assertThat(config.credentials.username).isEqualTo("fulluser")
        assertThat(config.projectInfo.name).isEqualTo("full-project")
        assertThat(config.projectInfo.scm.connection).isEqualTo("scm:git:https://github.com/full/project.git")
        assertThat(config.projectInfo.license.name).isEqualTo("MIT License")
        assertThat(config.projectInfo.developers).hasSize(1)
        assertThat(config.projectInfo.developers[0].name).isEqualTo("Full Developer")
        assertThat(config.signing.keyId).isEqualTo("ABCD1234")
        assertThat(config.publishing.autoPublish).isFalse()
    }
    
    @Test
    fun `should provide type-safe DSL with compile-time checking`() {
        // Given - This should compile and be type-safe
        project.centralPublisher {
            // The IDE should provide auto-completion here
            credentials {
                username = "typed-user"
                // password = 123 // This should NOT compile (type mismatch)
                password = "typed-pass" // This should compile
            }
            
            publishing {
                autoPublish = true // Boolean
                // autoPublish = "true" // This should NOT compile (type mismatch)
                dryRun = false
            }
        }
        
        // Then - If this test compiles, our DSL is type-safe
        assertThat(true).isTrue() // Test passes if it compiles
    }
}

// Extension function to make the DSL work in tests
// This will be moved to the actual extension implementation
private fun Project.centralPublisher(configure: CentralPublisherExtension.() -> Unit): CentralPublisherConfig {
    val extension = extensions.findByType(CentralPublisherExtension::class.java) 
        ?: extensions.create("centralPublisher", CentralPublisherExtension::class.java, this)
    extension.configure()
    return extension.build()
}