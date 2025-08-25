package com.tddworks.sonatype.publish.portal.api

/**
 * Represents the result of a deployment operation. Follows Chicago School TDD principles with clear
 * state representation.
 */
sealed class DeploymentResult {
    data class Success(
        val deploymentId: String,
        val message: String = "Deployment completed successfully",
    ) : DeploymentResult()

    data class Failure(
        val errorMessage: String,
        val errorCode: String,
        val cause: Throwable? = null,
    ) : DeploymentResult()

    fun isSuccess(): Boolean = this is Success

    fun isFailure(): Boolean = this is Failure

    fun getDeploymentIdOrNull(): String? =
        when (this) {
            is Success -> deploymentId
            is Failure -> null
        }

    fun getErrorMessageOrNull(): String? =
        when (this) {
            is Success -> null
            is Failure -> errorMessage
        }
}
