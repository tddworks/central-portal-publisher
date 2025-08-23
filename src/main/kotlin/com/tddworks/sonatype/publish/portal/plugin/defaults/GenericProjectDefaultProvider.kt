package com.tddworks.sonatype.publish.portal.plugin.defaults

import com.tddworks.sonatype.publish.portal.plugin.config.*
import org.gradle.api.Project

/**
 * Provides generic smart defaults for any project.
 *
 * This is the fallback provider that works for any project type. It provides conservative, safe
 * defaults that work in most scenarios. Has the lowest priority so other providers can override its
 * values.
 */
class GenericProjectDefaultProvider : SmartDefaultProvider {

    override val name = "GenericProjectDefaults"
    override val priority = 10 // Lowest priority - fallback defaults

    override fun canProvideDefaults(project: Project): Boolean {
        // Always can provide generic defaults
        return true
    }

    override fun provideDefaults(
        project: Project,
        existingConfig: CentralPublisherConfig,
    ): CentralPublisherConfig {
        return CentralPublisherConfig(
            credentials =
                CredentialsConfig(
                    username = "", // Never provide default credentials for security
                    password = "",
                ),
            projectInfo =
                ProjectInfoConfig(
                    name = inferProjectName(project),
                    description =
                        "A library for publishing to Maven Central", // Generic description
                    url = "",
                    scm = ScmConfig(url = "", connection = "", developerConnection = ""),
                    license =
                        LicenseConfig(
                            name = "Apache License 2.0", // Most common open source license
                            url = "https://www.apache.org/licenses/LICENSE-2.0.txt",
                            distribution = "repo",
                        ),
                    developers = emptyList(),
                ),
            signing =
                SigningConfig(
                    keyId = "", // Never provide default signing credentials
                    password = "",
                    secretKeyRingFile = getDefaultKeyRingPath(),
                ),
            publishing =
                PublishingConfig(
                    autoPublish = false, // Very conservative - manual approval
                    aggregation = true, // Standard default
                    dryRun = false,
                ),
        )
    }

    private fun inferProjectName(project: Project): String {
        val rootName = project.rootProject.name
        val projectName = project.name

        return when {
            // Multi-module project
            projectName != "root" && projectName != rootName -> "$rootName-$projectName"

            // Single module
            rootName.isNotBlank() -> rootName

            // Fallback to directory name
            else -> project.projectDir.name
        }
    }

    private fun getDefaultKeyRingPath(): String {
        val userHome = System.getProperty("user.home")
        return "$userHome/.gnupg/secring.gpg"
    }
}
