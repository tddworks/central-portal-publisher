package com.tddworks.sonatype.publish.portal.plugin.defaults

import com.tddworks.sonatype.publish.portal.plugin.config.*
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GenericProjectDefaultProviderTest {

    private lateinit var provider: GenericProjectDefaultProvider
    private lateinit var project: Project

    @TempDir lateinit var tempDir: File

    @BeforeEach
    fun setup() {
        provider = GenericProjectDefaultProvider()
        project = ProjectBuilder.builder().withProjectDir(tempDir).build()
    }

    @Test
    fun `should have lowest priority and generic name`() {
        // Then
        assertThat(provider.name).isEqualTo("GenericProjectDefaults")
        assertThat(provider.priority).isEqualTo(10) // Lowest priority
    }

    @Test
    fun `should always be able to provide defaults`() {
        // When
        val canProvide = provider.canProvideDefaults(project)

        // Then
        assertThat(canProvide).isTrue()
    }

    @Test
    fun `should provide conservative defaults for unknown projects`() {
        // Given
        val baseConfig = createEmptyConfig()

        // When
        val defaults = provider.provideDefaults(project, baseConfig)

        // Then - Conservative, safe defaults
        assertThat(defaults.publishing.autoPublish).isFalse() // Very conservative
        assertThat(defaults.publishing.aggregation).isTrue() // Standard default
        assertThat(defaults.publishing.dryRun).isFalse()
    }

    @Test
    fun `should provide generic license default`() {
        // Given
        val baseConfig = createEmptyConfig()

        // When
        val defaults = provider.provideDefaults(project, baseConfig)

        // Then - Most common open source license
        assertThat(defaults.projectInfo.license.name).isEqualTo("Apache License 2.0")
        assertThat(defaults.projectInfo.license.url)
            .isEqualTo("https://www.apache.org/licenses/LICENSE-2.0.txt")
        assertThat(defaults.projectInfo.license.distribution).isEqualTo("repo")
    }

    @Test
    fun `should infer basic project name from directory`() {
        // Given - Project with directory name
        val namedProject =
            ProjectBuilder.builder().withName("my-generic-project").withProjectDir(tempDir).build()

        val baseConfig = createEmptyConfig()

        // When
        val defaults = provider.provideDefaults(namedProject, baseConfig)

        // Then
        assertThat(defaults.projectInfo.name).isEqualTo("my-generic-project")
    }

    @Test
    fun `should provide generic project description`() {
        // Given
        val baseConfig = createEmptyConfig()

        // When
        val defaults = provider.provideDefaults(project, baseConfig)

        // Then
        assertThat(defaults.projectInfo.description)
            .isEqualTo("A library for publishing to Maven Central")
    }

    @Test
    fun `should provide default GPG keyring path`() {
        // Given
        val baseConfig = createEmptyConfig()

        // When
        val defaults = provider.provideDefaults(project, baseConfig)

        // Then
        val expectedPath = "${System.getProperty("user.home")}/.gnupg/secring.gpg"
        assertThat(defaults.signing.secretKeyRingFile).isEqualTo(expectedPath)
    }

    @Test
    fun `should leave credentials empty for security`() {
        // Given
        val baseConfig = createEmptyConfig()

        // When
        val defaults = provider.provideDefaults(project, baseConfig)

        // Then - Never provide default credentials
        assertThat(defaults.credentials.username).isEmpty()
        assertThat(defaults.credentials.password).isEmpty()
        assertThat(defaults.signing.keyId).isEmpty()
        assertThat(defaults.signing.password).isEmpty()
    }

    @Test
    fun `should handle multi-module project structure`() {
        // Given - Multi-module setup
        val rootProject = ProjectBuilder.builder().withName("my-root-project").build()

        val subProject =
            ProjectBuilder.builder()
                .withName("sub-module")
                .withParent(rootProject)
                .withProjectDir(tempDir)
                .build()

        val baseConfig = createEmptyConfig()

        // When
        val defaults = provider.provideDefaults(subProject, baseConfig)

        // Then
        assertThat(defaults.projectInfo.name).isEqualTo("my-root-project-sub-module")
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
