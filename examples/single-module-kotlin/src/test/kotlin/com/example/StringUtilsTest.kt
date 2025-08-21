package com.example

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StringUtilsTest {
    
    @Test
    fun `should capitalize words correctly`() {
        val input = "hello world from kotlin"
        val result = StringUtils.capitalizeWords(input)
        assertThat(result).isEqualTo("Hello World From Kotlin")
    }
    
    @Test
    fun `should reverse string correctly`() {
        val input = "kotlin"
        val result = StringUtils.reverse(input)
        assertThat(result).isEqualTo("niltok")
    }
    
    @Test
    fun `should detect palindromes correctly`() {
        assertThat(StringUtils.isPalindrome("racecar")).isTrue()
        assertThat(StringUtils.isPalindrome("A man a plan a canal Panama")).isTrue()
        assertThat(StringUtils.isPalindrome("hello")).isFalse()
    }
    
    @Test
    fun `should truncate long strings`() {
        val input = "This is a very long string that needs to be truncated"
        val result = StringUtils.truncate(input, 20)
        assertThat(result).isEqualTo("This is a very lo...")
    }
    
    @Test
    fun `should not truncate short strings`() {
        val input = "Short"
        val result = StringUtils.truncate(input, 10)
        assertThat(result).isEqualTo("Short")
    }
    
    @Test
    fun `should count word occurrences correctly`() {
        val text = "The quick brown fox jumps over the lazy fox"
        val count = StringUtils.countWordOccurrences(text, "fox")
        assertThat(count).isEqualTo(2)
    }
}