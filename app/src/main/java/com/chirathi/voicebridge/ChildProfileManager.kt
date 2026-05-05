package com.chirathi.voicebridge

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * ChildProfileManager — Persistent per-child longitudinal learning profile.
 *
 * RESEARCH ROLE:
 * Enables inter-session personalization by seeding the model's LSTM history
 * input [1,5,17] with the child's actual last 5 feature vectors from their
 * most recent session. Without this, every session starts with a zero-history
 * tensor and the model has no temporal context about the child.
 *
 * With this, session N+1 begins with the model already "knowing" the child's
 * accuracy, jitter, and frustration trajectory from session N. The model's
 * temporal encoder (LSTM) can then make better-calibrated decisions from
 * round 1, rather than needing several rounds to "warm up".
 *
 * UPDATE RULE: Exponential moving average (α=0.35) so recent sessions
 * have higher weight. α=0.35 gives the last 3 sessions ~70% of the weight.
 */
object ChildProfileManager {

    private const val TAG        = "ChildProfileManager"
    private const val PREFS_NAME = "child_profiles_v2"
    private const val EMA_ALPHA  = 0.35f

    data class ChildProfile(
        val childId:              Int,
        val ageGroup:             Int,        // 6 = ages 6-7, 8 = ages 8-10
        var avgAccuracy:          Float = 0.50f,
        var avgFrustration:       Float = 0.20f,
        var avgEngagement:        Float = 0.60f,
        var avgJitter:            Float = 0.15f,
        var avgRt:                Float = 3000f,
        var avgAlpha:             Float = 4.00f,
        var sessionsCompleted:    Int   = 0,
        var peakConsecWrong:      Int   = 0,
        var moodMatchAccuracy:    Float = 0.50f,
        var rhythmAccuracy:       Float = 0.50f,
        var sequenceAccuracy:     Float = 0.50f,
        var preferredSong:        String = "",
        // Last 5 feature vectors flattened: 5 × 17 = 85 floats
        // Stored in order: [step0_feat0..feat16, step1_feat0..feat16, ...]
        var historyVectors:       List<Float> = List(85) { 0f },
        var lastSessionTimestamp: Long = 0L
    )

    fun save(context: Context, profile: ChildProfile) {
        try {
            val json = JSONObject().apply {
                put("childId",              profile.childId)
                put("ageGroup",             profile.ageGroup)
                put("avgAccuracy",          profile.avgAccuracy)
                put("avgFrustration",       profile.avgFrustration)
                put("avgEngagement",        profile.avgEngagement)
                put("avgJitter",            profile.avgJitter)
                put("avgRt",                profile.avgRt)
                put("avgAlpha",             profile.avgAlpha)
                put("sessionsCompleted",    profile.sessionsCompleted)
                put("peakConsecWrong",      profile.peakConsecWrong)
                put("moodMatchAccuracy",    profile.moodMatchAccuracy)
                put("rhythmAccuracy",       profile.rhythmAccuracy)
                put("sequenceAccuracy",     profile.sequenceAccuracy)
                put("preferredSong",        profile.preferredSong)
                put("lastSessionTimestamp", profile.lastSessionTimestamp)
                put("historyVectors",       JSONArray(profile.historyVectors))
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString("profile_${profile.childId}", json.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Save failed: ${e.message}")
        }
    }

    fun load(context: Context, childId: Int): ChildProfile? {
        return try {
            val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString("profile_$childId", null) ?: return null
            val j = JSONObject(raw)
            val histArr = j.optJSONArray("historyVectors")
            ChildProfile(
                childId = j.getInt("childId"),
                ageGroup = j.getInt("ageGroup"),
                avgAccuracy = j.getDouble("avgAccuracy").toFloat(),
                avgFrustration = j.getDouble("avgFrustration").toFloat(),
                avgEngagement = j.getDouble("avgEngagement").toFloat(),
                avgJitter = j.getDouble("avgJitter").toFloat(),
                avgRt = j.getDouble("avgRt").toFloat(),
                avgAlpha = j.getDouble("avgAlpha").toFloat(),
                sessionsCompleted = j.getInt("sessionsCompleted"),
                peakConsecWrong = j.getInt("peakConsecWrong"),
                moodMatchAccuracy = j.optDouble("moodMatchAccuracy", 0.5).toFloat(),
                rhythmAccuracy = j.optDouble("rhythmAccuracy", 0.5).toFloat(),
                sequenceAccuracy = j.optDouble("sequenceAccuracy", 0.5).toFloat(),
                preferredSong = j.optString("preferredSong", ""),
                lastSessionTimestamp = j.optLong("lastSessionTimestamp", 0L),
                historyVectors = if (histArr != null)
                    (0 until histArr.length()).map { histArr.getDouble(it).toFloat() }
                else List(85) { 0f }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Load failed childId=$childId: ${e.message}"); null
        }
    }

    /**
     * Called at the end of every session. Updates EMA fields and stores
     * the session's last 5 feature vectors for next-session LSTM seeding.
     *
     * @param lastFiveFeatureVectors  Flat list of 85 floats (5 steps × 17 features).
     *                                Pass the session's last 5 feature arrays flattened.
     */
    fun updateAfterSession(
        context:                  Context,
        childId:                  Int,
        ageGroup:                 Int,
        gameType:                 String,  // "mood_match" | "rhythm" | "sequence"
        sessionAccuracy:          Float,
        sessionFrustration:       Float,
        sessionEngagement:        Float,
        sessionJitter:            Float,
        sessionRt:                Float,
        sessionAlpha:             Float,
        peakConsecWrong:          Int,
        lastFiveFeatureVectors:   List<Float> = List(85) { 0f },
        preferredSong:            String = ""
    ) {
        val e = load(context, childId) ?: ChildProfile(childId = childId, ageGroup = ageGroup)
        fun ema(old: Float, new: Float) = (1f - EMA_ALPHA) * old + EMA_ALPHA * new

        save(context, e.copy(
            ageGroup          = ageGroup,
            avgAccuracy       = ema(e.avgAccuracy,    sessionAccuracy),
            avgFrustration    = ema(e.avgFrustration, sessionFrustration),
            avgEngagement     = ema(e.avgEngagement,  sessionEngagement),
            avgJitter         = ema(e.avgJitter,      sessionJitter),
            avgRt             = ema(e.avgRt,          sessionRt),
            avgAlpha          = ema(e.avgAlpha,       sessionAlpha),
            sessionsCompleted = e.sessionsCompleted + 1,
            peakConsecWrong   = maxOf(e.peakConsecWrong, peakConsecWrong),
            moodMatchAccuracy = if (gameType == "mood_match") ema(e.moodMatchAccuracy, sessionAccuracy) else e.moodMatchAccuracy,
            rhythmAccuracy    = if (gameType == "rhythm")     ema(e.rhythmAccuracy,    sessionAccuracy) else e.rhythmAccuracy,
            sequenceAccuracy  = if (gameType == "sequence")   ema(e.sequenceAccuracy,  sessionAccuracy) else e.sequenceAccuracy,
            preferredSong     = if (preferredSong.isNotBlank()) preferredSong else e.preferredSong,
            historyVectors    = if (lastFiveFeatureVectors.size == 85) lastFiveFeatureVectors else e.historyVectors,
            lastSessionTimestamp = System.currentTimeMillis()
        ))
        Log.d(TAG, "Updated childId=$childId sessions=${e.sessionsCompleted + 1} " +
                "alpha=${sessionAlpha.format()} accuracy=${sessionAccuracy.format()}")
    }

    /**
     * Returns the [1,5,17] history tensor for model input, seeded from the
     * child's last session. Pass directly as historyBuf in GameMasterModel.
     */
    fun getHistoryTensor(context: Context, childId: Int): Array<Array<FloatArray>> {
        val flat = load(context, childId)?.historyVectors ?: return arrayOf(Array(5) { FloatArray(17) })
        return arrayOf(Array(5) { step -> FloatArray(17) { feat ->
            val idx = step * 17 + feat
            if (idx < flat.size) flat[idx] else 0f
        }})
    }

    /**
     * Returns seed features for the first prediction of a new session.
     * Uses child's historical averages so round-1 decisions are not cold-start defaults.
     */
    fun getSeedFeatures(context: Context, childId: Int, currentAge: Int): FloatArray {
        val p = load(context, childId)
        return FloatArray(17).apply {
            this[0]  = currentAge.toFloat()
            this[1]  = p?.avgJitter      ?: 0.15f
            this[2]  = (p?.avgRt ?: 3000f) * 0.6f   // itl estimate
            this[3]  = p?.avgRt          ?: 3000f
            this[4]  = 1400f
            this[5]  = p?.avgAccuracy    ?: 0.50f
            this[6]  = p?.avgEngagement  ?: 0.60f
            this[7]  = p?.avgFrustration ?: 0.20f
            // [8-11] error types: 0 — no prior error type at session start
            this[12] = 0f
            this[13] = 0f
            this[14] = 0f
            this[15] = (p?.avgJitter ?: 0.15f) * 0.9f
            this[16] = p?.avgAlpha   ?: 4.0f
        }
    }

    private fun Float.format() = "%.3f".format(this)
}