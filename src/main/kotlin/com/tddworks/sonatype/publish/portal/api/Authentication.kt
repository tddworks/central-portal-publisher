package com.tddworks.sonatype.publish.portal.api

data class Authentication(val username: String?, val password: String?)

class AuthenticationBuilder {
    var username: String? = null
    var password: String? = null

    fun build(): Authentication {
        return Authentication(username, password)
    }
}
