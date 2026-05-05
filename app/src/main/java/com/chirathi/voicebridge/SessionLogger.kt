package com.chirathi.voicebridge

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * SessionLogger — Research evidence collector.
 *
 * Every model prediction is logged with its inputs and outputs.
 * Export as CSV from any debug screen to prove personalization:
 *   - Two children with different profiles → different optDiff, distractor, friendAct
 *   - Same child across sessions → optDiff increases as rhythmAccuracy improves
 */
object SessionLogger {
    private const val TAG = "SessionLogger"

    fun logPrediction(
        context:   Context,
        childId:   Int,
        round:     Int,
        gameType:  String,
        ageGroup:  Int,
        features:  FloatArray,
        pred:      Prediction,
        diffLabel: String = ""
    ) {
        runCatching {
            val entry = JSONObject().apply {
                put("ts",          System.currentTimeMillis())
                put("childId",     childId)
                put("game",        gameType)
                put("ageGroup",    ageGroup)
                put("round",       round)
                put("accuracy",    features.getOrElse(5) { 0f })
                put("frustration", features.getOrElse(7) { 0f })
                put("jitter",      features.getOrElse(1) { 0f })
                put("rt",          features.getOrElse(3) { 3000f })
                put("optDiff",     pred.optimalDifficulty)
                put("frustRisk",   pred.frustrationRisk)
                put("distractor",  pred.distractor)
                put("miniTrigger", pred.minigameTrigger)
                put("friendAct",   pred.friendAction)
                put("interMode",   pred.interactionMode)
                put("timePressure",pred.timePressure)
                put("emotLevel",   pred.emotionLevel)
                put("hintLevel",   pred.optimalHint)
                put("rhythmComp",  pred.rhythmComplexity)
                put("diffLabel",   diffLabel)
                put("fromModel",   pred.fromModel)
            }
            val prefs = context.getSharedPreferences("session_log", Context.MODE_PRIVATE)
            val logs  = prefs.getStringSet("logs_$childId", mutableSetOf())!!.toMutableSet()
            // Cap at 500 entries per child to avoid SharedPreferences bloat
            if (logs.size > 500) logs.clear()
            logs.add(entry.toString())
            prefs.edit().putStringSet("logs_$childId", logs).apply()
        }.onFailure { Log.e(TAG, "Log failed: ${it.message}") }
    }

    fun logRhythmSession(
        context:        Context,
        childId:        Int,
        songTitle:      String,
        tier:           RhythmFlashcardManager.ProgressTier,
        shownCount:     Int,
        summaryWords:   List<String>,
        score:          Int,
        totalRounds:    Int
    ) {
        Log.d(TAG, "═══ RHYTHM SESSION LOG ═══════════════════════")
        Log.d(TAG, "  childId=$childId  song=$songTitle")
        Log.d(TAG, "  tier=$tier  flashcardsShownDuringSong=$shownCount")
        Log.d(TAG, "  summaryPool(5): $summaryWords")
        Log.d(TAG, "  score=$score/$totalRounds")
        Log.d(TAG, "══════════════════════════════════════════════")

        runCatching {
            val entry = JSONObject().apply {
                put("ts",           System.currentTimeMillis())
                put("childId",      childId)
                put("game",         "rhythm")
                put("song",         songTitle)
                put("tier",         tier.name)
                put("shownCount",   shownCount)
                put("summaryWords", summaryWords.joinToString(","))
                put("score",        score)
                put("totalRounds",  totalRounds)
            }
            val prefs = context.getSharedPreferences("rhythm_log", Context.MODE_PRIVATE)
            val logs  = prefs.getStringSet("logs_$childId", mutableSetOf())!!.toMutableSet()
            if (logs.size > 200) logs.clear()
            logs.add(entry.toString())
            prefs.edit().putStringSet("logs_$childId", logs).apply()
        }.onFailure { Log.e(TAG, "Rhythm log failed: ${it.message}") }
    }

    fun exportCsv(context: Context): String {
        val header = "ts,childId,game,ageGroup,round,accuracy,frustration,jitter,rt," +
                "optDiff,frustRisk,distractor,miniTrigger,friendAct,interMode," +
                "timePressure,emotLevel,hintLevel,rhythmComp,diffLabel,fromModel"
        val keys = listOf("ts","childId","game","ageGroup","round","accuracy","frustration",
            "jitter","rt","optDiff","frustRisk","distractor","miniTrigger","friendAct",
            "interMode","timePressure","emotLevel","hintLevel","rhythmComp","diffLabel","fromModel")
        val prefs = context.getSharedPreferences("session_log", Context.MODE_PRIVATE)
        val rows  = (0..50).flatMap { id ->
            (prefs.getStringSet("logs_$id", emptySet()) ?: emptySet())
                .mapNotNull { raw ->
                    runCatching {
                        val j = JSONObject(raw)
                        keys.joinToString(",") { k -> j.opt(k)?.toString() ?: "" }
                    }.getOrNull()
                }
        }.sortedBy { it.split(",").firstOrNull()?.toLongOrNull() ?: 0L }
        return "$header\n${rows.joinToString("\n")}"
    }
}