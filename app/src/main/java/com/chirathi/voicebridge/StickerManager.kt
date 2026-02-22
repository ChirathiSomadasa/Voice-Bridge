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
 *  • The sticker pool is defined here; add drawables as needed.
 *
 *  Usage:
 *    val mgr  = StickerManager(context)
 *    val info = mgr.awardNextSticker()   // null if pool exhausted
 *    mgr.updateRoutineUnlocks()
 */
object StickerManager {

    // ── Prefs keys ────────────────────────────────────────────────────────────
    private const val PREFS_STICKERS        = "sticker_progress"
    private const val KEY_EARNED_IDS        = "earned_sticker_ids"
    private const val KEY_STICKER_COUNT     = "stickers_collected"
    private const val PREFS_ROUTINES        = "unlocked_routines"
    private const val KEY_ROUTINE_1         = "routine_1"
    private const val KEY_ROUTINE_2         = "routine_2"

    // ── Sticker pool ─────────────────────────────────────────────────────────
    data class StickerInfo(
        val id          : String,
        val drawableRes : Int,
        val name        : String,
        val category    : String   // "comfort"|"achievement"|"celebration"|"encouragement"
    )

    val STICKER_POOL: List<StickerInfo> = listOf(
        StickerInfo("sticker_comfort_1",       R.drawable.sticker_unlock_three,     "Cozy Star",       "comfort"),
        StickerInfo("sticker_achievement_1",   R.drawable.sticker_unlock_four,      "Champion Star",   "achievement"),
        StickerInfo("sticker_encouragement_1", R.drawable.sticker_unlock_twelve,    "Brave Star",      "encouragement"),
        StickerInfo("sticker_celebration_1",   R.drawable.sticker_unlock_thirteen,  "Party Star",      "celebration"),
        StickerInfo("sticker_comfort_2",       R.drawable.sticker,                  "Friendly Star",   "comfort"),
        StickerInfo("sticker_achievement_2",   R.drawable.sticker_unlock_three,     "Gold Star",       "achievement"),
        StickerInfo("sticker_celebration_2",   R.drawable.sticker_unlock_four,      "Rainbow Star",    "celebration"),
        StickerInfo("sticker_encouragement_2", R.drawable.sticker_unlock_twelve,    "Super Star",      "encouragement"),
        StickerInfo("sticker_comfort_3",       R.drawable.sticker_unlock_thirteen,  "Shining Star",    "comfort"),
        StickerInfo("sticker_achievement_3",   R.drawable.sticker,                  "Hero Star",       "achievement"),
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