package com.tddworks.sonatype.portal.api

import com.tddworks.sonatype.publish.portal.api.Authentication
import com.tddworks.sonatype.publish.portal.api.DeploymentBundle
import com.tddworks.sonatype.publish.portal.api.PublicationType
import com.tddworks.sonatype.publish.portal.api.SonatypePortalPublisher
import com.tddworks.sonatype.publish.portal.api.http.FileUploader
import com.tddworks.sonatype.publish.portal.api.http.HttpRequestBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

class SonatypePortalPublisherTest {
    private val uploader: FileUploader = mock()

    private val target = SonatypePortalPublisher(uploader)

    @Test
    fun `should return deployment id`() {
        val file = mock<File>()

        val username = "some-username"
        val password = "some-password"
        val authentication = Authentication(username, password)
        val publicationType = PublicationType.USER_MANAGED

        val httpRequestCaptor = argumentCaptor<HttpRequestBuilder.() -> Unit>()

        whenever(
            uploader.uploadFile(
                file = argThat<File> {
                    this == file
                },
                builder = httpRequestCaptor.capture()
            )
        ).thenReturn("some-deployment-id")

        val deploymentId = target.deploy(
            authentication, DeploymentBundle(
                file,
                publicationType
            )
        )

        assertEquals("some-deployment-id", deploymentId)
    }
}