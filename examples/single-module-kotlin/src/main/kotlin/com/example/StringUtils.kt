package com.example

/**
 * A simple utility class demonstrating a single-module library. This would be published to Maven
 * Central using the Central Portal Publisher plugin.
 */
object StringUtils {

    /**
     * Capitalizes the first letter of each word in the string.
     *
     * @param input The input string to capitalize
     * @return The string with each word capitalized
     */
    fun capitalizeWords(input: String): String {
        return input.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Reverses the given string.
     *
     * @param input The input string to reverse
     * @return The reversed string
     */
    fun reverse(input: String): String {
        return input.reversed()
    }

    /**
     * Checks if a string is a palindrome (reads the same forwards and backwards).
     *
     * @param input The string to check
     * @return true if the string is a palindrome, false otherwise
     */
    fun isPalindrome(input: String): Boolean {
        val normalized = input.lowercase().filter { it.isLetterOrDigit() }
        return normalized == normalized.reversed()
    }

    /**
     * Truncates a string to the specified length, adding ellipsis if truncated.
     *
     * @param input The input string
     * @param maxLength The maximum length
     * @return The truncated string with ellipsis if needed
     */
    fun truncate(input: String, maxLength: Int): String {
        return if (input.length <= maxLength) {
            input
        } else {
            "${input.take(maxLength - 3)}..."
        }
    }

    /**
     * Counts the occurrences of a word in a string (case-insensitive).
     *
     * @param text The text to search in
     * @param word The word to count
     * @return The number of occurrences
     */
    fun countWordOccurrences(text: String, word: String): Int {
        val normalizedText = text.lowercase()
        val normalizedWord = word.lowercase()

        return normalizedText.split("\\W+".toRegex()).count { it == normalizedWord }
    }
}
