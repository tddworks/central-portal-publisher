package com.tddworks.sonatype.publish.portal.api

import com.tddworks.sonatype.publish.portal.api.internal.api.FileUploader
import com.tddworks.sonatype.publish.portal.api.internal.api.http.internal.okHttpClient
import com.tddworks.sonatype.publish.portal.api.validation.AuthenticationValidator
import com.tddworks.sonatype.publish.portal.api.validation.DeploymentBundleValidator

/**
 * Publisher for deploying artifacts to Sonatype Central Portal.
 * 
 * Refactored following Chicago School TDD principles:
 * - Uses dedicated validators for separation of concerns
 * - Provides clear state-based validation results
 * - Offers structured error reporting
 * - Maintains single responsibility principle
 */
class SonatypePortalPublisher(
    private val uploader: FileUploader = FileUploader.okHttpClient(),
    private val authenticationValidator: AuthenticationValidator = AuthenticationValidator(),
    private val deploymentBundleValidator: DeploymentBundleValidator = DeploymentBundleValidator()
) {
    
    /**
     * Deploys an authenticated deployment bundle to Sonatype Central Portal.
     * 
     * @param authentication The credentials for Sonatype access
     * @param deploymentBundle The bundle containing artifacts to deploy
     * @return The deployment ID from Sonatype Portal
     * @throws IllegalArgumentException for validation failures
     * @throws RuntimeException for upload failures
     */
    fun deploy(authentication: Authentication, deploymentBundle: DeploymentBundle): String {
        // Validate inputs using dedicated validators
        val authValidation = authenticationValidator.validate(authentication)
        if (!authValidation.isValid) {
            throw IllegalArgumentException(authValidation.getFirstError()!!)
        }
        
        val bundleValidation = deploymentBundleValidator.validate(deploymentBundle)
        if (!bundleValidation.isValid) {
            throw IllegalArgumentException(bundleValidation.getFirstError()!!)
        }
        
        // Perform upload with proper error wrapping
        return try {
            uploader.uploadFile(deploymentBundle.file) {
                authentication.username?.let { username ->
                    authentication.password?.let { password -> addAuthorization(username, password) }
                }
                addParameter("publishingType", deploymentBundle.publicationType.name)
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to upload deployment bundle: ${e.message}", e)
        }
    }
    
    /**
     * Alternative method that returns a structured result instead of throwing exceptions.
     * More suitable for functional programming patterns.
     */
    fun deployWithResult(authentication: Authentication, deploymentBundle: DeploymentBundle): DeploymentResult {
        return try {
            val deploymentId = deploy(authentication, deploymentBundle)
            DeploymentResult.Success(deploymentId)
        } catch (e: IllegalArgumentException) {
            DeploymentResult.Failure(e.message ?: "Validation failed", "VALIDATION_ERROR", e)
        } catch (e: RuntimeException) {
            DeploymentResult.Failure(e.message ?: "Upload failed", "UPLOAD_ERROR", e)
        }
    }

    companion object {
        fun default(): SonatypePortalPublisher {
            return SonatypePortalPublisher()
        }
    }
}
