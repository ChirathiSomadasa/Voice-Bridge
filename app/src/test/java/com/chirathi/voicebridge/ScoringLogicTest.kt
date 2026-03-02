package com.chirathi.voicebridge

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoringLogicTest {

    // Unit Test 1: Testing Clinical Category Categorization
    @Test
    fun `test score categorization logic`() {
        // Test Good Pronunciation
        assertEquals("good", getCategory(80))
        assertEquals("good", getCategory(75))

        // Test Moderate Pronunciation
        assertEquals("moderate", getCategory(74))
        assertEquals("moderate", getCategory(50))

        // Test Poor Pronunciation
        assertEquals("bad", getCategory(49))
        assertEquals("bad", getCategory(0))
    }

    // Unit Test 2: Testing Progress Percentage Calculation
    @Test
    fun `test total progress calculation`() {
        val wordScores = intArrayOf(100, 50, 75, 0, 100) // 5 words
        val totalMaxScore = wordScores.size * 100 // 500
        val earnedScore = wordScores.sum() // 325

        val expectedProgress = ((325f / 500f) * 100).toInt() // 65%

        val actualProgress = calculateProgress(wordScores, totalMaxScore)
        assertEquals(expectedProgress, actualProgress)
    }

    // --- Helper Functions simulating Activity logic ---
    private fun getCategory(score: Int): String {
        return when {
            score >= 75 -> "good"
            score >= 50 -> "moderate"
            else -> "bad"
        }
    }

    private fun calculateProgress(scores: IntArray, maxScore: Int): Int {
        val earned = scores.sum()
        return ((earned.toFloat() / maxScore.toFloat()) * 100).toInt()
    }
}