package com.tddworks.example.kmp

/** Native implementation of Platform. */
actual class Platform actual constructor() {
    actual val name: String = "Native"
}

/** Native-specific utility functions. */
class NativeUtils {
    /** Gets platform-specific path separator. */
    fun getPathSeparator(): String {
        return "/"
    }
}
