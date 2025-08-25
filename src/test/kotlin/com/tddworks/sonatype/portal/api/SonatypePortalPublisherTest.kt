package com.tddworks.sonatype.portal.api

import com.tddworks.sonatype.publish.portal.api.Authentication
import com.tddworks.sonatype.publish.portal.api.DeploymentBundle
import com.tddworks.sonatype.publish.portal.api.PublicationType
import com.tddworks.sonatype.publish.portal.api.SonatypePortalPublisher
import com.tddworks.sonatype.publish.portal.api.internal.api.FileUploader
import com.tddworks.sonatype.publish.portal.api.internal.api.HttpRequestBuilder
import java.io.File
import java.io.IOException
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class `Sonatype Portal Publisher` {

    private lateinit var publisher: SonatypePortalPublisher
    private lateinit var testFileUploader: TestFileUploader

    @TempDir private lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        testFileUploader = TestFileUploader()
        publisher = SonatypePortalPublisher(testFileUploader)
    }

    @Nested
    inner class `When authentication is invalid` {

        @Test
        fun `should reject deployment when username is null`() {
            val authentication = Authentication(username = null, password = "some-password")
            val deploymentBundle = createValidDeploymentBundle()

            assertThatThrownBy { publisher.deploy(authentication, deploymentBundle) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Username is required")
        }

        @Test
        fun `should reject deployment when username is blank`() {
            val authentication = Authentication(username = "", password = "some-password")
            val deploymentBundle = createValidDeploymentBundle()

            assertThatThrownBy { publisher.deploy(authentication, deploymentBundle) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Username cannot be empty")
        }

        @Test
        fun `should reject deployment when password is null`() {
            val authentication = Authentication(username = "some-username", password = null)
            val deploymentBundle = createValidDeploymentBundle()

            assertThatThrownBy { publisher.deploy(authentication, deploymentBundle) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Password is required")
        }

        @Test
        fun `should reject deployment when password is blank`() {
            val authentication = Authentication(username = "some-username", password = "")
            val deploymentBundle = createValidDeploymentBundle()

            assertThatThrownBy { publisher.deploy(authentication, deploymentBundle) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Password cannot be empty")
        }
    }

    @Nested
    inner class `When deployment bundle is invalid` {

        @Test
        fun `should reject deployment when file does not exist`() {
            val authentication = createValidAuthentication()
            val nonExistentFile = tempDir.resolve("non-existent.zip").toFile()
            val deploymentBundle = DeploymentBundle(nonExistentFile, PublicationType.USER_MANAGED)

            assertThatThrownBy { publisher.deploy(authentication, deploymentBundle) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Deployment file does not exist")
        }

        @Test
        fun `should reject deployment when file is not readable`() {
            val authentication = createValidAuthentication()
            val unreadableFile = tempDir.resolve("unreadable.zip").toFile()
            unreadableFile.createNewFile()
            unreadableFile.setReadable(false)
            val deploymentBundle = DeploymentBundle(unreadableFile, PublicationType.USER_MANAGED)

            assertThatThrownBy { publisher.deploy(authentication, deploymentBundle) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Deployment file is not readable")
        }
    }

    @Nested
    inner class `When upload fails` {

        @Test
        fun `should propagate upload failure with meaningful error message`() {
            val authentication = createValidAuthentication()
            val deploymentBundle = createValidDeploymentBundle()
            testFileUploader.shouldFailWith(IOException("Network timeout"))

            assertThatThrownBy { publisher.deploy(authentication, deploymentBundle) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("Failed to upload deployment bundle")
                .hasCauseInstanceOf(IOException::class.java)
        }
    }

    @Nested
    inner class `When deployment is valid` {

        @Test
        fun `should return deployment ID when upload succeeds`() {
            val authentication = createValidAuthentication()
            val deploymentBundle = createValidDeploymentBundle()
            testFileUploader.shouldReturnDeploymentId("deployment-12345")

            val result = publisher.deploy(authentication, deploymentBundle)

            assertThat(result).isEqualTo("deployment-12345")
        }

        @Test
        fun `should configure uploader with correct authentication`() {
            val authentication = Authentication("test-user", "test-password")
            val deploymentBundle = createValidDeploymentBundle()
            testFileUploader.shouldReturnDeploymentId("deployment-12345")

            publisher.deploy(authentication, deploymentBundle)

            val capturedBuilder = testFileUploader.getCapturedRequestBuilder()
            assertThat(capturedBuilder.getHeaders())
                .containsEntry("Authorization", "UserToken dGVzdC11c2VyOnRlc3QtcGFzc3dvcmQ=")
        }

        @Test
        fun `should configure uploader with correct publication type`() {
            val authentication = createValidAuthentication()
            val deploymentBundle = DeploymentBundle(createValidFile(), PublicationType.AUTOMATIC)
            testFileUploader.shouldReturnDeploymentId("deployment-12345")

            publisher.deploy(authentication, deploymentBundle)

            val capturedBuilder = testFileUploader.getCapturedRequestBuilder()
            assertThat(capturedBuilder.getParameters()).containsEntry("publishingType", "AUTOMATIC")
        }
    }

    @Nested
    inner class `When using result-based deployment` {

        @Test
        fun `should return success result when deployment succeeds`() {
            val authentication = createValidAuthentication()
            val deploymentBundle = createValidDeploymentBundle()
            testFileUploader.shouldReturnDeploymentId("deployment-56789")

            val result = publisher.deployWithResult(authentication, deploymentBundle)

            assertThat(result.isSuccess()).isTrue()
            assertThat(result.getDeploymentIdOrNull()).isEqualTo("deployment-56789")
        }

        @Test
        fun `should return failure result when authentication is invalid`() {
            val authentication = Authentication(username = null, password = "some-password")
            val deploymentBundle = createValidDeploymentBundle()

            val result = publisher.deployWithResult(authentication, deploymentBundle)

            assertThat(result.isFailure()).isTrue()
            assertThat(result.getErrorMessageOrNull()).contains("Username is required")
        }

        @Test
        fun `should return failure result when upload fails`() {
            val authentication = createValidAuthentication()
            val deploymentBundle = createValidDeploymentBundle()
            testFileUploader.shouldFailWith(IOException("Network error"))

            val result = publisher.deployWithResult(authentication, deploymentBundle)

            assertThat(result.isFailure()).isTrue()
            assertThat(result.getErrorMessageOrNull())
                .contains("Failed to upload deployment bundle")
        }
    }

    private fun createValidAuthentication() = Authentication("valid-username", "valid-password")

    private fun createValidDeploymentBundle(): DeploymentBundle {
        return DeploymentBundle(createValidFile(), PublicationType.USER_MANAGED)
    }

    private fun createValidFile(): File {
        val file = tempDir.resolve("valid-bundle.zip").toFile()
        file.writeText("test content")
        return file
    }
}

/**
 * Test double for FileUploader following Chicago School principles. Uses real state instead of
 * mocked interactions.
 */
private class TestFileUploader : FileUploader {
    private var deploymentId: String? = null
    private var failureException: Exception? = null
    private var capturedFile: File? = null
    private var capturedBuilder: HttpRequestBuilder? = null

    fun shouldReturnDeploymentId(id: String) {
        this.deploymentId = id
        this.failureException = null
    }

    fun shouldFailWith(exception: Exception) {
        this.failureException = exception
        this.deploymentId = null
    }

    fun getCapturedFile(): File? = capturedFile

    fun getCapturedRequestBuilder(): HttpRequestBuilder {
        return capturedBuilder ?: throw IllegalStateException("No request captured yet")
    }

    override fun uploadFile(file: File, builder: HttpRequestBuilder.() -> Unit): String {
        capturedFile = file
        capturedBuilder = HttpRequestBuilder().apply(builder)

        failureException?.let { throw it }
        return deploymentId ?: throw IllegalStateException("Test not configured properly")
    }
}
