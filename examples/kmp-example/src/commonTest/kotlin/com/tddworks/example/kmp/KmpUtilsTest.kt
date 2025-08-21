package com.tddworks.example.kmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KmpUtilsTest {
    
    @Test
    fun `should return greeting with platform name`() {
        val greeting = greeting()
        assertTrue(greeting.startsWith("Hello from"))
        assertTrue(greeting.endsWith("!"))
    }
    
    @Test
    fun `should format message with platform info`() {
        val utils = KmpUtils()
        val result = utils.formatMessage("test message")
        
        assertTrue(result.contains("test message"))
        assertTrue(result.startsWith("["))
        assertTrue(result.contains("]"))
    }
    
    @Test
    fun `should reverse string correctly`() {
        val utils = KmpUtils()
        
        assertEquals("dcba", utils.reverseString("abcd"))
        assertEquals("", utils.reverseString(""))
        assertEquals("a", utils.reverseString("a"))
        assertEquals("54321", utils.reverseString("12345"))
    }
    
    @Test
    fun `platform name should not be empty`() {
        val platform = Platform()
        assertTrue(platform.name.isNotBlank())
    }
}