package com.tddworks.sonatype.publish.portal.plugin.defaults

import com.tddworks.sonatype.publish.portal.plugin.config.*
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SmartDefaultProviderTest {

    private lateinit var project: Project

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
    }

    @Test
    fun `should create SmartDefaultManager with standard providers`() {
        // When
        val manager = SmartDefaultManager(project)

        // Then
        val providers = manager.getActiveProviders(project)
        assertThat(providers).isNotEmpty()

        // Should include generic provider at minimum
        assertThat(providers.map { it.name }).contains("GenericProjectDefaults")
    }

    @Test
    fun `should apply defaults in priority order`() {
        // Given
        val highPriorityProvider =
            object : SmartDefaultProvider {
                override val name = "HighPriority"
                override val priority = 100

                override fun canProvideDefaults(project: Project) = true

                override fun provideDefaults(
                    project: Project,
                    existingConfig: CentralPublisherConfig,
                ): CentralPublisherConfig {
                    return existingConfig.copy(
                        projectInfo =
                            existingConfig.projectInfo.copy(
                                description = "High priority description"
                            )
                    )
                }
            }

        val lowPriorityProvider =
            object : SmartDefaultProvider {
                override val name = "LowPriority"
                override val priority = 10

                override fun canProvideDefaults(project: Project) = true

                override fun provideDefaults(
                    project: Project,
                    existingConfig: CentralPublisherConfig,
                ): CentralPublisherConfig {
                    return existingConfig.copy(
                        projectInfo =
                            existingConfig.projectInfo.copy(
                                description = "Low priority description"
                            )
                    )
                }
            }

        val manager = SmartDefaultManager(listOf(lowPriorityProvider, highPriorityProvider))
        val baseConfig = createEmptyConfig()

        // When
        val result = manager.applySmartDefaults(project, baseConfig)

        // Then - High priority should win
        assertThat(result.projectInfo.description).isEqualTo("High priority description")
    }

    @Test
    fun `should not override existing values`() {
        // Given
        val provider =
            object : SmartDefaultProvider {
                override val name = "TestProvider"
                override val priority = 50

                override fun canProvideDefaults(project: Project) = true

                override fun provideDefaults(
                    project: Project,
                    existingConfig: CentralPublisherConfig,
                ): CentralPublisherConfig {
                    return existingConfig.copy(
                        projectInfo =
                            existingConfig.projectInfo.copy(
                                name = "Default Name",
                                description = "Default Description",
                            )
                    )
                }
            }

        val manager = SmartDefaultManager(listOf(provider))
        val baseConfig =
            createEmptyConfig()
                .copy(
                    projectInfo =
                        createEmptyConfig()
                            .projectInfo
                            .copy(
                                name = "Existing Name" // This should NOT be overridden
                            )
                )

        // When
        val result = manager.applySmartDefaults(project, baseConfig)

        // Then
        assertThat(result.projectInfo.name).isEqualTo("Existing Name") // Preserved
        assertThat(result.projectInfo.description).isEqualTo("Default Description") // Applied
    }

    @Test
    fun `should skip providers that cannot provide defaults`() {
        // Given
        val inactiveProvider =
            object : SmartDefaultProvider {
                override val name = "InactiveProvider"
                override val priority = 100

                override fun canProvideDefaults(project: Project) = false

                override fun provideDefaults(
                    project: Project,
                    existingConfig: CentralPublisherConfig,
                ): CentralPublisherConfig {
                    throw AssertionError("Should not be called")
                }
            }

        val activeProvider =
            object : SmartDefaultProvider {
                override val name = "ActiveProvider"
                override val priority = 50

                override fun canProvideDefaults(project: Project) = true

                override fun provideDefaults(
                    project: Project,
                    existingConfig: CentralPublisherConfig,
                ): CentralPublisherConfig {
                    return existingConfig.copy(
                        projectInfo =
                            existingConfig.projectInfo.copy(
                                description = "Active provider description"
                            )
                    )
                }
            }

        val manager = SmartDefaultManager(listOf(inactiveProvider, activeProvider))
        val baseConfig = createEmptyConfig()

        // When
        val result = manager.applySmartDefaults(project, baseConfig)

        // Then
        assertThat(result.projectInfo.description).isEqualTo("Active provider description")
        assertThat(manager.getActiveProviders(project)).hasSize(1)
        assertThat(manager.getActiveProviders(project)[0].name).isEqualTo("ActiveProvider")
    }

    @Test
    fun `should merge configurations correctly`() {
        // Given
        val provider =
            object : SmartDefaultProvider {
                override val name = "MergeTestProvider"
                override val priority = 50

                override fun canProvideDefaults(project: Project) = true

                override fun provideDefaults(
                    project: Project,
                    existingConfig: CentralPublisherConfig,
                ): CentralPublisherConfig {
                    return CentralPublisherConfig(
                        credentials =
                            CredentialsConfig(username = "default-user", password = "default-pass"),
                        projectInfo =
                            ProjectInfoConfig(
                                name = "default-name",
                                description = "default-description",
                                url = "default-url",
                                scm =
                                    ScmConfig(
                                        url = "default-scm-url",
                                        connection = "default-connection",
                                        developerConnection = "default-dev-connection",
                                    ),
                                license =
                                    LicenseConfig(
                                        name = "Default License",
                                        url = "default-license-url",
                                        distribution = "repo",
                                    ),
                                developers =
                                    listOf(
                                        DeveloperConfig(
                                            id = "default-dev",
                                            name = "Default Developer",
                                            email = "default@example.com",
                                            organization = "",
                                            organizationUrl = "",
                                        )
                                    ),
                            ),
                        signing =
                            SigningConfig(
                                keyId = "default-key",
                                password = "default-signing-pass",
                                secretKeyRingFile = "default-keyring",
                            ),
                        publishing =
                            PublishingConfig(autoPublish = true, aggregation = false, dryRun = true),
                    )
                }
            }

        val manager = SmartDefaultManager(listOf(provider))
        val baseConfig =
            createEmptyConfig()
                .copy(
                    credentials = createEmptyConfig().credentials.copy(username = "existing-user"),
                    projectInfo = createEmptyConfig().projectInfo.copy(name = "existing-name"),
                )

        // When
        val result = manager.applySmartDefaults(project, baseConfig)

        // Then - Existing values preserved, missing values filled
        assertThat(result.credentials.username).isEqualTo("existing-user")
        assertThat(result.credentials.password).isEqualTo("default-pass")
        assertThat(result.projectInfo.name).isEqualTo("existing-name")
        assertThat(result.projectInfo.description).isEqualTo("default-description")
        assertThat(result.signing.keyId).isEqualTo("default-key")
        assertThat(result.publishing.autoPublish).isTrue()
    }

    private fun createEmptyConfig(): CentralPublisherConfig {
        return CentralPublisherConfig(
            credentials = CredentialsConfig("", ""),
            projectInfo =
                ProjectInfoConfig(
                    name = "",
                    description = "",
                    url = "",
                    scm = ScmConfig("", "", ""),
                    license = LicenseConfig("", "", ""),
                    developers = emptyList(),
                ),
            signing = SigningConfig("", "", ""),
            publishing = PublishingConfig(false, true, false),
        )
    }
}
