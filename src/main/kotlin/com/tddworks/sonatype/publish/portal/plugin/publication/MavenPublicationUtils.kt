package com.tddworks.sonatype.publish.portal.plugin.publication

import com.tddworks.sonatype.publish.portal.plugin.config.CentralPublisherConfig
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication

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