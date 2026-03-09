package com.chirathi.voicebridge

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.*

class PhraseLogicTest {

    // Unit Test 1: Testing Greetings
    @Test
    fun testMorningGreeting() {
        assertEquals("Good Morning!", getGreeting(9))
    }

    @Test
    fun testAfternoonGreeting() {
        assertEquals("Good Afternoon!", getGreeting(13))
    }

    @Test
    fun testNightGreeting() {
        assertEquals("Good Night!", getGreeting(23))
    }

    // Unit Test 2: Testing Sentence Generation By filling blanks
    @Test
    fun testGenerateSentenceWithBlanks() {

        val sentence = "I want apple juice"

        val result = generateSentenceWithBlanks(sentence)

        val blankSentence = result.first
        val hiddenWords = result.second

        // Check blanks appear
        assertTrue(blankSentence.contains("____"))

        // Check words removed
        assertTrue(hiddenWords.isNotEmpty())
    }

        private fun getGreeting(hour: Int): String {
            return when (hour) {
                in 5..11 -> "Good Morning!"
                in 12..15 -> "Good Afternoon!"
                in 16..18 -> "Good Evening!"
                else -> "Good Night!"
            }
        }

        private fun generateSentenceWithBlanks(sentence: String): Pair<String, List<String>> {
            val words = sentence.split(" ").toMutableList()
            val index = 2
            val hiddenWord = words[index]
            words[index] = "____"
            return Pair(words.joinToString(" "), listOf(hiddenWord))
        }
}