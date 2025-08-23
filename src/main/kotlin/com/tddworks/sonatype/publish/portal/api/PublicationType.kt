package com.tddworks.sonatype.publish.portal.api

enum class PublicationType {
    USER_MANAGED,
    AUTOMATIC;

    companion object {
        fun valueOf(value: String): PublicationType {
            return when (value) {
                "USER_MANAGED" -> USER_MANAGED
                "AUTOMATIC" -> AUTOMATIC
                else -> throw IllegalArgumentException("Invalid value for PublicationType: $value")
            }
        }
    }
}
