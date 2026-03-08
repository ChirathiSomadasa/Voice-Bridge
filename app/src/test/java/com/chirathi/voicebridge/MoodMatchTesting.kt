package com.chirathi.voicebridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodMatchScoringLogicTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Unit Test 1: Score points awarded per attempt number
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test score points per attempt`() {
        // First attempt correct → 10 points
        assertEquals(10, getPointsForAttempt(wrongAttempts = 0))

        // Second attempt correct → 6 points
        assertEquals(6, getPointsForAttempt(wrongAttempts = 1))

        // Third attempt correct → 3 points
        assertEquals(3, getPointsForAttempt(wrongAttempts = 2))

        // Fourth attempt correct → 1 point
        assertEquals(1, getPointsForAttempt(wrongAttempts = 3))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Unit Test 2: Session frustration increases on wrong answers
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test frustration increases correctly on wrong answers`() {
        var frustration = 0.20f

        // First wrong tap in a round adds 0.08
        frustration = updateFrustration(frustration, wasCorrect = false, attemptNumber = 1)
        assertEquals(0.28f, frustration, 0.001f)

        // Second wrong tap adds 0.12
        frustration = updateFrustration(frustration, wasCorrect = false, attemptNumber = 2)
        assertEquals(0.40f, frustration, 0.001f)

        // Third wrong tap adds 0.16
        frustration = updateFrustration(frustration, wasCorrect = false, attemptNumber = 3)
        assertEquals(0.56f, frustration, 0.001f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Unit Test 3: Session frustration decreases on correct answers
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test frustration decreases correctly on correct answers`() {
        var frustration = 0.50f

        // First-attempt correct removes 0.10
        frustration = updateFrustration(frustration, wasCorrect = true, attemptNumber = 1)
        assertEquals(0.40f, frustration, 0.001f)

        // Second-attempt correct removes 0.05
        frustration = updateFrustration(frustration, wasCorrect = true, attemptNumber = 2)
        assertEquals(0.35f, frustration, 0.001f)

        // Third-attempt correct removes only 0.02
        frustration = updateFrustration(frustration, wasCorrect = true, attemptNumber = 3)
        assertEquals(0.33f, frustration, 0.001f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Unit Test 4: Frustration stays within 0.10 – 1.0 bounds
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test frustration stays within bounds`() {
        // Floor: should never go below 0.10
        var frustration = 0.11f
        frustration = updateFrustration(frustration, wasCorrect = true, attemptNumber = 1)
        assertEquals(0.10f, frustration, 0.001f)

        // Ceiling: should never exceed 1.0
        frustration = 0.95f
        frustration = updateFrustration(frustration, wasCorrect = false, attemptNumber = 3)
        assertEquals(1.00f, frustration, 0.001f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Unit Test 5: Live accuracy calculation
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test live accuracy calculation`() {
        // Before any round is complete, default is 0.5
        assertEquals(0.5f, calculateLiveAccuracy(correctCount = 0, currentRound = 1), 0.001f)

        // 3 correct out of 4 completed rounds = 75%
        assertEquals(0.75f, calculateLiveAccuracy(correctCount = 3, currentRound = 5), 0.001f)

        // All correct = 100%
        assertEquals(1.0f, calculateLiveAccuracy(correctCount = 5, currentRound = 6), 0.001f)

        // None correct = 0%
        assertEquals(0.0f, calculateLiveAccuracy(correctCount = 0, currentRound = 6), 0.001f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Unit Test 6: Wrong ratio calculation
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test wrong ratio calculation`() {
        // No wrong taps → ratio is 0
        assertEquals(0.0f, calculateWrongRatio(totalWrong = 0, totalRounds = 5), 0.001f)

        // 6 wrong out of max 15 (5 rounds × 3) = 0.40
        assertEquals(0.40f, calculateWrongRatio(totalWrong = 6, totalRounds = 5), 0.001f)

        // All wrong → ratio is 1.0
        assertEquals(1.0f, calculateWrongRatio(totalWrong = 15, totalRounds = 5), 0.001f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Unit Test 7: Friend action / gift unlock decision
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test friend action gift unlock decision`() {
        // No frustration → no gift
        assertEquals(0, decideFriendAction(modelAction = 0, frustration = 0.40f, peakConsecWrong = 1, wrongRatio = 0.10f))

        // High frustration (>= 0.55) → comfort friend (1, 2, or 5)
        val comfortFriend = decideFriendAction(modelAction = 0, frustration = 0.60f, peakConsecWrong = 1, wrongRatio = 0.10f)
        assertTrue("Expected comfort friend (1,2,5) but got $comfortFriend", comfortFriend in listOf(1, 2, 5))

        // Peak frustration (>= 0.70) → blanket panda = 1
        assertEquals(1, decideFriendAction(modelAction = 0, frustration = 0.75f, peakConsecWrong = 1, wrongRatio = 0.10f))

        // Model action always wins regardless of frustration
        assertEquals(9, decideFriendAction(modelAction = 9, frustration = 0.10f, peakConsecWrong = 0, wrongRatio = 0.0f))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Unit Test 8: Max possible score across 5 rounds
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun `test maximum score does not exceed 50`() {
        var total = 0
        repeat(5) { total += getPointsForAttempt(wrongAttempts = 0) }

        assertEquals(50, total)
        assertTrue("Score exceeded max", total <= 50)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper functions — mirror exact logic from MoodMatchSevenDownActivity
    // ─────────────────────────────────────────────────────────────────────────

    private fun getPointsForAttempt(wrongAttempts: Int): Int = when (wrongAttempts) {
        0    -> 10
        1    -> 6
        2    -> 3
        else -> 1
    }

    private fun updateFrustration(current: Float, wasCorrect: Boolean, attemptNumber: Int): Float {
        return if (wasCorrect) {
            val drop = when (attemptNumber) { 1 -> 0.10f; 2 -> 0.05f; else -> 0.02f }
            (current - drop).coerceAtLeast(0.10f)
        } else {
            val rise = when (attemptNumber) { 1 -> 0.08f; 2 -> 0.12f; else -> 0.16f }
            (current + rise).coerceAtMost(1.00f)
        }
    }

    private fun calculateLiveAccuracy(correctCount: Int, currentRound: Int): Float =
        if (currentRound > 1) correctCount.toFloat() / (currentRound - 1) else 0.5f

    private fun calculateWrongRatio(totalWrong: Int, totalRounds: Int): Float =
        totalWrong.toFloat() / (totalRounds * 3).toFloat()

    private fun decideFriendAction(
        modelAction: Int,
        frustration: Float,
        peakConsecWrong: Int,
        wrongRatio: Float
    ): Int = when {
        modelAction > 0                                    -> modelAction
        frustration >= 0.70f                               -> 1
        frustration >= 0.55f                               -> listOf(1, 2, 5).random()
        peakConsecWrong >= 3 && wrongRatio >= 0.4f         -> 2
        else                                               -> 0
    }
}