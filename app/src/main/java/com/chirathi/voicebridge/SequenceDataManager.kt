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
 */
object SequenceDataManager {

    private const val PREFS_NAME    = "sequence_prefs"
    private const val KEY_SEQUENCES = "custom_sequences"

    // ── Routine / sub-routine labels ──────────────────────────────────────
    val routineNames    = listOf("Morning Routine", "Bedtime Routine", "School Routine")
    val subRoutineNames = listOf("Sub-Routine 1", "Sub-Routine 2", "Sub-Routine 3")

    // ── Built-in defaults ─────────────────────────────────────────────────
    val defaults: Map<Int, Map<Int, List<Pair<String, String>>>> = mapOf(
        0 to mapOf(
            0 to listOf("wake_up"      to "Wake up",
                "make_bed"     to "Make your bed",
                "drink_water"  to "Drink water"),
            1 to listOf("brush_teeth"  to "Brush your teeth",
                "wash_face"    to "Wash your face",
                "dry_towel"    to "Dry your face"),
            2 to listOf("get_dressed"  to "Get dressed",
                "apply_powder" to "Put on lotion",
                "put_pajamas"  to "Put away pajamas")
        ),
        1 to mapOf(
            0 to listOf("brush_teeth"  to "Brush your teeth",
                "put_pajamas"  to "Put on pajamas",
                "drink_water"  to "Drink water"),
            1 to listOf("wash_face"    to "Wash your face",
                "dry_towel"    to "Dry your face",
                "make_bed"     to "Tidy your bed"),
            2 to listOf("get_dressed"  to "Change clothes",
                "apply_powder" to "Apply lotion",
                "wake_up"      to "Say good night")
        ),
        2 to mapOf(
            0 to listOf("wake_up"      to "Wake up",
                "get_dressed"  to "Get dressed",
                "drink_water"  to "Have breakfast"),
            1 to listOf("brush_teeth"  to "Brush your teeth",
                "wash_face"    to "Wash your face",
                "make_bed"     to "Pack your bag"),
            2 to listOf("apply_powder" to "Put on shoes",
                "dry_towel"    to "Grab your lunch",
                "put_pajamas"  to "Head to school")
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