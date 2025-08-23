package com.tddworks.example.kmp

/** JavaScript implementation of Platform. */
actual class Platform actual constructor() {
    actual val name: String = "JavaScript"
}

/** JavaScript-specific utility functions. */
class JsUtils {
    /** Gets current timestamp using JavaScript Date. */
    fun getCurrentTimestamp(): Double {
        return js("Date.now()") as Double
    }

    /** Logs a message to console. */
    fun log(message: String) {
        console.log(message)
    }
}
