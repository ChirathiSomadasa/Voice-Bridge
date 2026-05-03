package com.chirathi.voicebridge

import android.util.Log

/**
 * SessionStateTracker — Model-calibrated within-session emotional state.
 *
 * RESEARCH NOVELTY:
 * The frustration/engagement update rates are NOT hardcoded constants.
 * They are modulated by the model's own frustrationRisk output, creating
 * a closed feedback loop:
 *
 *   frustrationRisk (model output)
 *       ↓ modulates growth/decay rates
 *   sessionFrustration/Engagement updated
 *       ↓ fed back to model as input next round
 *   model refines frustrationRisk
 *       ↓ loop continues
 *
 * This means the state tracker is not a fixed equation — it is calibrated
 * by the same model that reads its output, creating a self-correcting
 * system that amplifies signals the model is already detecting.
 *
 * Also tracks the feature history buffer for ChildProfileManager seeding.
 */
class SessionStateTracker(
    private val ageGroup:          Int,
    initialFrustration:            Float = 0.20f,
    initialEngagement:             Float = 0.60f
) {
    var frustration:    Float = initialFrustration; private set
    var engagement:     Float = initialEngagement;  private set
    var totalWrong:     Int   = 0; private set
    var totalCorrect:   Int   = 0; private set
    var peakConsecWrong:Int   = 0; private set
    var roundsPlayed:   Int   = 0; private set

    // Rolling buffer of the last 5 feature vectors (for LSTM history seeding)
    private val featureHistory = ArrayDeque<FloatArray>(5)

    private val TAG = "SessionStateTracker"

    /**
     * @param wasCorrect      Whether the child answered correctly this attempt
     * @param attemptNumber   1=first try, 2=second, 3=third
     * @param modelFrustRisk  Model's frustrationRisk output for this round (0–1)
     * @param consecutiveWrong Current consecutive wrong streak
     */
    fun update(
        wasCorrect:       Boolean,
        attemptNumber:    Int,
        modelFrustRisk:   Float,
        consecutiveWrong: Int
    ) {
        val risk = modelFrustRisk.coerceIn(0.10f, 0.90f)

        if (wasCorrect) {
            totalCorrect++
            // Decay faster when model agrees child is recovering (low risk)
            // Decay slower when model still predicts risk despite correct answer
            val decay = when (attemptNumber) {
                1    -> 0.08f + (1f - risk) * 0.06f   // 0.08–0.14 range
                2    -> 0.04f + (1f - risk) * 0.04f   // 0.04–0.08
                else -> 0.01f + (1f - risk) * 0.02f   // 0.01–0.03
            }
            frustration = (frustration - decay).coerceAtLeast(0.10f)
            engagement  = (engagement  + decay * 0.70f).coerceAtMost(1.00f)
        } else {
            totalWrong++
            if (consecutiveWrong > peakConsecWrong) peakConsecWrong = consecutiveWrong
            // Grow faster when model already detects a risk trend (positive feedback)
            val growth = when (attemptNumber) {
                1    -> 0.06f + risk * 0.08f   // 0.06–0.14
                2    -> 0.09f + risk * 0.10f   // 0.09–0.19
                else -> 0.13f + risk * 0.12f   // 0.13–0.25
            }
            frustration = (frustration + growth).coerceAtMost(1.00f)
            engagement  = (engagement  - growth * 0.50f).coerceAtLeast(0.10f)
        }

        Log.d(TAG, "attempt=$attemptNumber correct=$wasCorrect risk=${risk.f()} " +
                "→ frust=${frustration.f()} eng=${engagement.f()}")
    }

    fun recordFeatureVector(features: FloatArray) {
        if (featureHistory.size >= 5) featureHistory.removeFirst()
        featureHistory.addLast(features.copyOf())
    }

    fun recordRound() { roundsPlayed++ }

    /** Flat 85-float list for ChildProfileManager.updateAfterSession() */
    fun getFlatHistory(): List<Float> {
        val padded = Array(5) { i ->
            if (i < featureHistory.size) featureHistory[i] else FloatArray(17)
        }
        return padded.flatMap { it.toList() }
    }

    fun liveAccuracy(totalRounds: Int): Float =
        if (totalRounds > 0) totalCorrect.toFloat() / totalRounds else 0.5f

    fun wrongRatio(maxPossible: Int): Float =
        if (maxPossible > 0) totalWrong.toFloat() / maxPossible else 0f

    fun avgJitter(tapVariances: List<Float>): Float =
        if (tapVariances.isNotEmpty()) tapVariances.average().toFloat() else 0.15f

    private fun Float.f() = "%.3f".format(this)
}