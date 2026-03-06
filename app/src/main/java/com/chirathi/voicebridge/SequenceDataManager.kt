package com.chirathi.voicebridge

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores and retrieves customisable activity sequences.
 *
 * Structure:  routine (0/1/2)  →  sub-routine (0/1/2)  →  list of (id, displayText) pairs
 *
 * • Parents edit via ParentSequenceSettingsActivity.
 * • ActivitySequenceOverActivity reads via getSequences().
 * • If no custom data is saved the built-in defaults are returned.
 *
 * Routine mapping (must match arrays.xml and ActivitySequenceUnderActivity):
 *   0 → Morning Routine   (3 sub-routines: wake-up, hygiene, getting dressed)
 *   1 → Mealtime Routine  (2 sub-routines: before eating, eating)
 *   2 → School Routine    (1 sub-routine:  pack for school)
 */
object SequenceDataManager {

    private const val PREFS_NAME    = "sequence_prefs"
    private const val KEY_SEQUENCES = "custom_sequences"

    // ── Routine / sub-routine labels ──────────────────────────────────────
    // Order and names must match the routineId constants used throughout the app
    val routineNames = listOf("Morning Routine", "Mealtime Routine", "School Routine")
    val subRoutineNames = listOf("Sub-Routine 1", "Sub-Routine 2", "Sub-Routine 3")

    // ── Built-in defaults ─────────────────────────────────────────────────
    // IDs mirror the drawable-key prefixes used in ActivitySequenceUnderActivity
    // so both activities stay in sync.
    //
    // Routine 0 — Morning (seq_rtn0_sub1 / sub2 / sub3)
    // Routine 1 — Mealtime (seq_rtn1_sub1 / sub2)          ← was wrongly "Bedtime"
    // Routine 2 — School (seq_rtn2_sub1 only)              ← was wrongly 3 sub-routines
    val defaults: Map<Int, Map<Int, List<Pair<String, String>>>> = mapOf(

        // ── Routine 0: Morning ────────────────────────────────────────────
        0 to mapOf(
            // seq_rtn0_sub1: wake up sequence
            0 to listOf(
                "rtn0_sub1_wake"   to "Wake up",
                "rtn0_sub1_bed"    to "Make your bed",
                "rtn0_sub1_drink"  to "Drink water"
            ),
            // seq_rtn0_sub2: hygiene sequence
            1 to listOf(
                "rtn0_sub2_brush"  to "Brush your teeth",
                "rtn0_sub2_wash"   to "Wash your face",
                "rtn0_sub2_dry"    to "Dry with a towel"
            ),
            // seq_rtn0_sub3: getting dressed
            2 to listOf(
                "rtn0_sub3_change" to "Get dressed",
                "rtn0_sub3_cream"  to "Put on lotion",
                "rtn0_sub3_wash"   to "Put away your pajamas"
            )
        ),

        // ── Routine 1: Mealtime ───────────────────────────────────────────
        1 to mapOf(
            // seq_rtn1_sub1: before eating
            0 to listOf(
                "rtn1_sub1_wash"   to "Wash your hands",
                "rtn1_sub1_sit"    to "Sit down at the table",
                "rtn1_sub1_napkin" to "Put on your napkin"
            ),
            // seq_rtn1_sub2: eating
            1 to listOf(
                "rtn1_sub2_eat"    to "Eat your food",
                "rtn1_sub2_wipe"   to "Wipe your mouth",
                "rtn1_sub2_wash"   to "Wash your hands"
            )
        ),

        // ── Routine 2: School ─────────────────────────────────────────────
        2 to mapOf(
            // seq_rtn2_sub1: pack for school
            0 to listOf(
                "rtn2_sub1_books"  to "Pack your books",
                "rtn2_sub1_lunch"  to "Pack your lunch",
                "rtn2_sub1_pack"   to "Pack your bag"
            )
        )
    )

    // ── Public API ────────────────────────────────────────────────────────

    fun getSequences(context: Context): Map<Int, Map<Int, List<Pair<String, String>>>> {
        val json = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SEQUENCES, null) ?: return defaults
        return try {
            parseJson(json)
        } catch (e: Exception) {
            defaults
        }
    }

    fun saveSequences(context: Context, data: Map<Int, Map<Int, List<Pair<String, String>>>>) {
        val json = toJson(data)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_SEQUENCES, json).apply()
    }

    fun resetToDefaults(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_SEQUENCES).apply()
    }

    // ── JSON helpers ──────────────────────────────────────────────────────

    private fun toJson(data: Map<Int, Map<Int, List<Pair<String, String>>>>): String {
        val root = JSONObject()
        data.forEach { (routineId, subMap) ->
            val subObj = JSONObject()
            subMap.forEach { (subId, steps) ->
                val arr = JSONArray()
                steps.forEach { (id, text) ->
                    arr.put(JSONObject().put("id", id).put("text", text))
                }
                subObj.put(subId.toString(), arr)
            }
            root.put(routineId.toString(), subObj)
        }
        return root.toString()
    }

    private fun parseJson(json: String): Map<Int, Map<Int, List<Pair<String, String>>>> {
        val result = mutableMapOf<Int, MutableMap<Int, List<Pair<String, String>>>>()
        val root   = JSONObject(json)
        for (rKey in root.keys()) {
            val subMap = mutableMapOf<Int, List<Pair<String, String>>>()
            val subObj = root.getJSONObject(rKey)
            for (sKey in subObj.keys()) {
                val arr   = subObj.getJSONArray(sKey)
                val steps = (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    obj.getString("id") to obj.getString("text")
                }
                subMap[sKey.toInt()] = steps
            }
            result[rKey.toInt()] = subMap
        }
        return result
    }
}