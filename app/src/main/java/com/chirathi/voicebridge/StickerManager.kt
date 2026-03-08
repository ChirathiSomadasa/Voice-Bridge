package com.chirathi.voicebridge

import android.content.Context
import android.content.SharedPreferences

/**
 * StickerManager — centralised source of truth for the sticker system.
 *
 *  • Each sub-routine completion can award ONE sticker from the pool.
 *  • A sticker ID is never awarded twice (tracked in a StringSet).
 *  • Sticker count drives routine unlocking:
 *      ≥ 3 stickers → routine_1 (Bedtime/Local-Time) unlocked
 *      ≥ 6 stickers → routine_2 (School) unlocked
 *
 *  Sticker name mapping (by drawable number):
 *   1 → Knight        2 → Astronaut     3 → Robot
 *   4 → Explorer      5 → Sorcerer      6 → Dinosaur
 *   7 → Dragon        8 → Pilot         12 → Alien
 *   13 → Ocean Diver
 */
object StickerManager {

    // ── Prefs keys ────────────────────────────────────────────────────────────
    private const val PREFS_STICKERS    = "sticker_progress"
    private const val KEY_EARNED_IDS    = "earned_sticker_ids"
    private const val KEY_STICKER_COUNT = "stickers_collected"
    private const val PREFS_ROUTINES    = "unlocked_routines"
    private const val KEY_ROUTINE_1     = "routine_1"
    private const val KEY_ROUTINE_2     = "routine_2"

    // ── Sticker pool ──────────────────────────────────────────────────────────
    data class StickerInfo(
        val id          : String,
        val drawableRes : Int,
        val name        : String,
        val category    : String   // "adventure"|"achievement"|"fantasy"|"discovery"
    )

    val STICKER_POOL: List<StickerInfo> = listOf(
        StickerInfo("sticker_1",  R.drawable.sticker,      "Knight",      "adventure"),
        StickerInfo("sticker_2",  R.drawable.sticker_unlock_two,      "Astronaut",   "discovery"),
        StickerInfo("sticker_3",  R.drawable.sticker_unlock_three,    "Robot",       "achievement"),
        StickerInfo("sticker_4",  R.drawable.sticker_unlock_four,     "Explorer",    "discovery"),
        StickerInfo("sticker_5",  R.drawable.sticker_unlock_five,     "Sorcerer",    "fantasy"),
        StickerInfo("sticker_6",  R.drawable.sticker_unlock_six,      "Dinosaur",    "adventure"),
        StickerInfo("sticker_7",  R.drawable.sticker_unlock_seven,    "Dragon",      "fantasy"),
        StickerInfo("sticker_8",  R.drawable.sticker_unlock_eight,    "Pilot",       "discovery"),
        StickerInfo("sticker_12", R.drawable.sticker_unlock_twelve,   "Alien",       "discovery"),
        StickerInfo("sticker_13", R.drawable.sticker_unlock_thirteen, "Ocean Diver", "adventure")
    )

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_STICKERS, Context.MODE_PRIVATE)

    private fun routinePrefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_ROUTINES, Context.MODE_PRIVATE)

    /** IDs of stickers the child has already earned. */
    fun earnedIds(ctx: Context): Set<String> =
        prefs(ctx).getStringSet(KEY_EARNED_IDS, emptySet()) ?: emptySet()

    /** Full StickerInfo for each earned sticker, in pool order. */
    fun earnedStickers(ctx: Context): List<StickerInfo> {
        val ids = earnedIds(ctx)
        return STICKER_POOL.filter { ids.contains(it.id) }
    }

    /** Total stickers earned. */
    fun stickerCount(ctx: Context): Int =
        prefs(ctx).getInt(KEY_STICKER_COUNT, 0)

    /**
     * Awards the next un-earned sticker from the pool.
     * Returns the awarded StickerInfo, or null if the pool is exhausted.
     * Automatically updates routine unlocks.
     */
    fun awardNextSticker(ctx: Context): StickerInfo? {
        val earned = earnedIds(ctx).toMutableSet()
        val next   = STICKER_POOL.firstOrNull { !earned.contains(it.id) } ?: return null

        earned.add(next.id)
        val newCount = earned.size

        prefs(ctx).edit()
            .putStringSet(KEY_EARNED_IDS, earned)
            .putInt(KEY_STICKER_COUNT, newCount)
            .apply()

        updateRoutineUnlocks(ctx, newCount)
        return next
    }

    /**
     * Call after earning a sticker to unlock routines based on count.
     * Also safe to call at app start to ensure consistency.
     */
    fun updateRoutineUnlocks(ctx: Context, count: Int = stickerCount(ctx)) {
        routinePrefs(ctx).edit().apply {
            if (count >= 3) putBoolean(KEY_ROUTINE_1, true)
            if (count >= 6) putBoolean(KEY_ROUTINE_2, true)
        }.apply()
    }

    /** True if the sticker pool is fully exhausted. */
    fun isPoolExhausted(ctx: Context): Boolean =
        earnedIds(ctx).size >= STICKER_POOL.size

    /** Sticker type string expected by UnlockStickerActivity. */
    fun StickerInfo.asType(): String = category

    /** DEBUG ONLY — reset all sticker progress. */
    fun debugReset(ctx: Context) {
        prefs(ctx).edit().clear().apply()
        routinePrefs(ctx).edit().clear().apply()
    }
}