package com.tddworks.sonatype.publish.portal.plugin.publication

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.plugins.signing.SigningExtension

/**
 * Configures POM metadata for Maven publications using the provided configuration.
 *
 * @param project The Gradle project
 * @param config The Central Publisher configuration containing metadata
 */
fun MavenPublication.configurePom(project: Project, config: CentralPublisherConfig) {
    pom {
        name.set(config.projectInfo.name.ifBlank { project.name })
        description.set(config.projectInfo.description)
        url.set(config.projectInfo.url)

        // Configure license
        licenses {
            license {
                name.set(config.projectInfo.license.name)
                url.set(config.projectInfo.license.url)
                distribution.set(config.projectInfo.license.distribution)
            }
        }

        // Configure developers
        developers {
            config.projectInfo.developers.forEach { dev ->
                developer {
                    id.set(dev.id)
                    name.set(dev.name)
                    email.set(dev.email)
                    organization.set(dev.organization)
                    organizationUrl.set(dev.organizationUrl)
                }
            }
        }

        // Configure SCM
        scm {
            connection.set(config.projectInfo.scm.connection)
            developerConnection.set(config.projectInfo.scm.developerConnection)
            url.set(config.projectInfo.scm.url)
        }
    }
}

/**
 * Configures signing for this Maven publication if signing credentials are available.
 *
 * This is the simplest and most intuitive approach - signing happens right where the publication is
 * created.
 *
 * @param project The Gradle project
 * @param config The Central Publisher configuration containing signing credentials
 */
fun MavenPublication.configureSigningIfAvailable(
    project: Project,
    config: CentralPublisherConfig,
    showMessages: Boolean = true,
) {
    // Check for in-memory keys first
    val signingKey = project.findProperty("SIGNING_KEY")?.toString() ?: System.getenv("SIGNING_KEY")
    val signingPassword =
        project.findProperty("SIGNING_PASSWORD")?.toString() ?: System.getenv("SIGNING_PASSWORD")

    // Check if any signing credentials are available
    val hasSigningCredentials =
        when {
            !signingKey.isNullOrBlank() -> true
            config.signing.keyId.isNotBlank() && config.signing.secretKeyRingFile.isNotBlank() ->
                true
            config.signing.keyId.isNotBlank() -> true
            else -> false
        }

    // Only apply and configure signing if we have credentials
    if (hasSigningCredentials) {
        // Apply signing plugin if not already applied
        if (!project.plugins.hasPlugin("signing")) {
            project.plugins.apply("signing")
        }

        when {
            !signingKey.isNullOrBlank() -> {
                if (showMessages) {
                    project.logger.quiet("🔐 Using in-memory GPG keys for signing")
                }
                project.configure<SigningExtension> {
                    useInMemoryPgpKeys(signingKey, signingPassword ?: "")
                }
                val signing = project.extensions.getByType(SigningExtension::class.java)
                signing.sign(this@configureSigningIfAvailable)
            }
            config.signing.keyId.isNotBlank() && config.signing.secretKeyRingFile.isNotBlank() -> {
                if (showMessages) {
                    project.logger.quiet(
                        "🔐 Using file-based signing with keyId: ${config.signing.keyId}"
                    )
                }
                val signing = project.extensions.getByType(SigningExtension::class.java)
                signing.sign(this@configureSigningIfAvailable)
            }
            config.signing.keyId.isNotBlank() -> {
                if (showMessages) {
                    project.logger.quiet("🔐 Using keyId-based signing: ${config.signing.keyId}")
                }
                val signing = project.extensions.getByType(SigningExtension::class.java)
                signing.sign(this@configureSigningIfAvailable)
            }
            else -> {
                if (showMessages) {
                    project.logger.quiet(
                        "⚠️ No signing configuration found - artifacts will be unsigned"
                    )
                }
            }
        }
    }
}
