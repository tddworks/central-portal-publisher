package com.tddworks.example.kmp

/** JVM implementation of Platform. */
actual class Platform actual constructor() {
    actual val name: String = "JVM (${System.getProperty("java.version")})"
}

/** JVM-specific utility functions. */
class JvmUtils {
    /** Gets system property safely. */
    fun getSystemProperty(key: String, defaultValue: String = "unknown"): String {
        return System.getProperty(key, defaultValue)
    }

    /** Gets current timestamp in milliseconds. */
    fun getCurrentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}
