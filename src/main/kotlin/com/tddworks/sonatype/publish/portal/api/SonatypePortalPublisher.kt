package com.tddworks.sonatype.publish.portal.api

import com.tddworks.sonatype.publish.portal.api.internal.api.FileUploader
import com.tddworks.sonatype.publish.portal.api.internal.api.http.internal.okHttpClient

class SonatypePortalPublisher(private val uploader: FileUploader = FileUploader.okHttpClient()) {
    fun deploy(authentication: Authentication, deploymentBundle: DeploymentBundle): String {
        return uploader.uploadFile(deploymentBundle.file) {
            authentication.username?.let { username ->
                authentication.password?.let { password -> addAuthorization(username, password) }
            }
            addParameter("publishingType", deploymentBundle.publicationType.name)
        }
    }

    companion object {
        fun default(): SonatypePortalPublisher {
            return SonatypePortalPublisher()
        }
    }
}
