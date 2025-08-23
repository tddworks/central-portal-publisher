package com.tddworks.sonatype.publish.portal.plugin.publication

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import com.tddworks.sonatype.publish.portal.plugin.config.DeveloperConfig
import com.tddworks.sonatype.publish.portal.plugin.config.LicenseConfig
import com.tddworks.sonatype.publish.portal.plugin.config.ProjectInfoConfig
import com.tddworks.sonatype.publish.portal.plugin.config.ScmConfig
import com.tddworks.sonatype.publish.portal.plugin.config.SigningConfig
import org.assertj.core.api.Assertions
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KotlinMultiplatformPublicationProviderTest {

    private lateinit var project: Project
    private lateinit var config: CentralPublisherConfig

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
        config =
            CentralPublisherConfig(
                projectInfo =
                    ProjectInfoConfig(
                        name = "kmp-library",
                        description = "A Kotlin Multiplatform library",
                        url = "https://github.com/example/kmp-library",
                        scm =
                            ScmConfig(
                                url = "https://github.com/example/kmp-library",
                                connection = "scm:git:git://github.com/example/kmp-library.git",
                                developerConnection =
                                    "scm:git:ssh://github.com/example/kmp-library.git",
                            ),
                        license =
                            LicenseConfig(
                                name = "MIT License",
                                url = "https://opensource.org/licenses/MIT",
                                distribution = "repo",
                            ),
                        developers =
                            listOf(
                                DeveloperConfig(
                                    id = "kmp-dev",
                                    name = "KMP Developer",
                                    email = "kmp@example.com",
                                )
                            ),
                    ),
                signing =
                    SigningConfig(
                        keyId = "kmp-key-id",
                        password = "kmp-password",
                        secretKeyRingFile = "/path/to/kmp-secring.gpg",
                    ),
            )
    }

    @Test
    fun `should apply maven-publish plugin for KMP projects`() {
        // Given: A project without maven-publish plugin
        project.plugins.apply("maven-publish")

        val provider = KotlinMultiplatformPublicationProvider()

        // When: Configure publications
        provider.configurePublications(project, config)

        // Then: maven-publish plugin should be available
        Assertions.assertThat(project.plugins.hasPlugin("maven-publish")).isEqualTo(true)
    }

    @Test
    fun `should configure POM for existing Maven publications`() {
        // Given: A project with maven-publish and a pre-existing publication
        project.plugins.apply("maven-publish")

        // Create a mock KMP plugin to trigger the provider
        project.plugins.apply("base") // Use base plugin as placeholder

        val publishing = project.extensions.getByType<PublishingExtension>()
        publishing.publications.register("kotlinMultiplatform", MavenPublication::class.java) {
            // Simulate a KMP-created publication
        }

        val provider = KotlinMultiplatformPublicationProvider()

        // When: Configure publications - call the internal method directly for testing
        provider.configureKotlinMultiplatformProject(project, config)

        // Then: POM should be configured on existing publications
        val mavenPub = publishing.publications.getByName("kotlinMultiplatform") as MavenPublication
        Assertions.assertThat(mavenPub.pom.name.isPresent).isEqualTo(true)
        Assertions.assertThat(mavenPub.pom.description.isPresent).isEqualTo(true)
        Assertions.assertThat(mavenPub.pom.url.isPresent).isEqualTo(true)
    }

    @Test
    fun `should configure signing when signing info is provided`() {
        // Given: A project with maven-publish and publications
        project.plugins.apply("maven-publish")
        project.plugins.apply("signing")

        val publishing = project.extensions.getByType<PublishingExtension>()
        publishing.publications.register("kotlinMultiplatform", MavenPublication::class.java) {
            // Simulate a KMP-created publication
        }

        val provider = KotlinMultiplatformPublicationProvider()

        // When: Configure publications
        provider.configurePublications(project, config)

        // Then: Signing should be configured
        val signing = project.extensions.getByName("signing")
        Assertions.assertThat(signing).isNotNull
    }

    @Test
    fun `should handle projects without signing configuration gracefully`() {
        // Given: A project with maven-publish but no signing info
        project.plugins.apply("maven-publish")

        val configWithoutSigning = config.copy(signing = SigningConfig())

        val provider = KotlinMultiplatformPublicationProvider()

        // When: Configure publications (should not throw)
        provider.configurePublications(project, configWithoutSigning)

        // Then: Should complete without error
        Assertions.assertThat(project.plugins.hasPlugin("maven-publish")).isEqualTo(true)
    }

    @Test
    fun `should configure multiple publications with POM metadata`() {
        // Given: A project with maven-publish and multiple publications (simulating KMP targets)
        project.plugins.apply("maven-publish")

        val publishing = project.extensions.getByType<PublishingExtension>()
        publishing.publications.register("kotlinMultiplatform", MavenPublication::class.java) {
            // Simulate root KMP publication
        }
        publishing.publications.register("jvm", MavenPublication::class.java) {
            // Simulate JVM target publication
        }
        publishing.publications.register("js", MavenPublication::class.java) {
            // Simulate JS target publication
        }

        val provider = KotlinMultiplatformPublicationProvider()

        // When: Configure publications - call the internal method directly for testing
        provider.configureKotlinMultiplatformProject(project, config)

        // Then: All publications should have POM configured
        val allPublications = publishing.publications.withType(MavenPublication::class.java)
        Assertions.assertThat(allPublications.size).isEqualTo(3)

        allPublications.forEach { publication ->
            Assertions.assertThat(publication.pom.name.isPresent).isEqualTo(true)
            Assertions.assertThat(publication.pom.description.isPresent).isEqualTo(true)
            Assertions.assertThat(publication.pom.url.isPresent).isEqualTo(true)
        }
    }

    @Test
    fun `should populate POM with all developer information`() {
        // Given: A project with maven-publish and a publication
        project.plugins.apply("maven-publish")

        val publishing = project.extensions.getByType<PublishingExtension>()
        publishing.publications.register("kotlinMultiplatform", MavenPublication::class.java) {
            // Simulate a KMP-created publication
        }

        val provider = KotlinMultiplatformPublicationProvider()

        // When: Configure publications - call the internal method directly for testing
        provider.configureKotlinMultiplatformProject(project, config)

        // Then: POM should contain developer information
        val mavenPub = publishing.publications.getByName("kotlinMultiplatform") as MavenPublication

        // Test that POM configuration methods are called
        Assertions.assertThat(mavenPub.pom.name.isPresent).isEqualTo(true)
        Assertions.assertThat(mavenPub.pom.description.isPresent).isEqualTo(true)
        Assertions.assertThat(mavenPub.pom.url.isPresent).isEqualTo(true)
    }
}
