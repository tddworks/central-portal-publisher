package com.tddworks.sonatype.publish.portal.plugin.wizard

/**
 * Interface for user interaction and prompting system.
 * Provides abstraction for different input methods (console, GUI, etc).
 */
interface PromptSystem {
    
    /**
     * Prompt the user for a string input
     */
    fun prompt(message: String): String
    
    /**
     * Prompt the user with a default value
     */
    fun promptWithDefault(message: String, defaultValue: String): String
    
    /**
     * Ask user for yes/no confirmation
     */
    fun confirm(message: String): Boolean
    
    /**
     * Present user with options to select from
     */
    fun select(message: String, options: List<String>): String
}