package com.chirathi.voicebridge

import android.util.Log

/**
 * RhythmFlashcardManager — Single source of truth for rhythm game content.
 */
object RhythmFlashcardManager {

    private const val TAG = "RhythmFlashcardManager"

    enum class ProgressTier { LOW, MEDIUM, HIGH }

    data class SongKeyword(
        val word:        String,
        val imageRes:    Int,
        val startTimeMs: Int,
        val endTimeMs:   Int,
        val difficulty:  Int
    )

    val SONG_KEYWORDS: Map<String, List<SongKeyword>> = mapOf(

        "Row Row Row Your Boat" to listOf(
            SongKeyword("boat",        R.drawable.rhy_song0_boat,       11000, 12000, 0),
            SongKeyword("stream",      R.drawable.rhy_song0_stream,     13000, 15000, 1),
            SongKeyword("dream",       R.drawable.rhy_song0_dream,      18000, 20000, 1),
            SongKeyword("creek",       R.drawable.rhy_song0_creek,      24000, 25000, 1),
            SongKeyword("mouse",       R.drawable.rhy_song0_mouse,      27000, 28000, 0),
            SongKeyword("river",       R.drawable.rhy_song0_river,      34000, 36000, 0),
            SongKeyword("polar bear",  R.drawable.rhy_song0_polar_bear, 37000, 38000, 0),
            SongKeyword("crocodile",   R.drawable.rhy_song0_crocodile,  48000, 49000, 0)
        ),

        "Twinkle Twinkle Little Star" to listOf(
            SongKeyword("star",        R.drawable.rhy_song1_star,       8000,  10000, 0),
            SongKeyword("world",       R.drawable.rhy_song1_world,      16000, 18000, 1),
            SongKeyword("diamond",     R.drawable.rhy_song1_diamond,    19000, 20000, 2),
            SongKeyword("sun",         R.drawable.rhy_song1_sun,        40000, 42000, 0),
            SongKeyword("light",       R.drawable.rhy_song1_light,      48000, 50000, 1),
            SongKeyword("night",       R.drawable.rhy_song1_moon,       53000, 55000, 1)
        ),

        "Jack and Jill" to listOf(
            SongKeyword("hill",        R.drawable.hill_image,           10500, 11000, 0),
            SongKeyword("water",       R.drawable.water_image,          12500, 14000, 0),
            SongKeyword("crown",       R.drawable.crown_image,          14500, 16000, 1)
        )
    )

    fun determineTierFromModel(rhythmComplexity: Float, optimalDifficulty: Int): ProgressTier {
        return when {
            rhythmComplexity >= 0.66f || optimalDifficulty >= 3 -> ProgressTier.HIGH
            rhythmComplexity >= 0.33f || optimalDifficulty >= 1 -> ProgressTier.MEDIUM
            else                                                -> ProgressTier.LOW
        }
    }

    fun determineTierFromProfile(profile: ChildProfileManager.ChildProfile?): ProgressTier {
        // High >= 70%, Medium >= 40%, Low < 40%
        // Note: Because of EMA smoothing, a new profile starting at 50% will need
        // 2 back-to-back 100% sessions to cross 70% and become HIGH.
        return when {
            profile == null                 -> ProgressTier.LOW
            profile.rhythmAccuracy >= 0.70f -> ProgressTier.HIGH
            profile.rhythmAccuracy >= 0.40f -> ProgressTier.MEDIUM
            else                            -> ProgressTier.LOW
        }
    }

    fun selectSongFlashcards(songTitle: String, tier: ProgressTier): List<SongKeyword> {
        val pool   = SONG_KEYWORDS[songTitle] ?: SONG_KEYWORDS["Row Row Row Your Boat"]!!
        val sorted = pool.sortedBy { it.startTimeMs }

        return when (tier) {
            ProgressTier.LOW -> {
                // Exactly 5, shuffled so it's a new mix of easy words each time
                val easy = sorted.filter { it.difficulty == 0 }
                if (easy.size >= 5) easy.shuffled().take(5).sortedBy { it.startTimeMs }
                else (easy + sorted.filter { it.difficulty == 1 }.shuffled()).take(5).sortedBy { it.startTimeMs }
            }
            ProgressTier.MEDIUM -> {
                // Exactly 2/3 of the keywords, dynamically shuffled
                val targetCount = (sorted.size * 2) / 3
                val eligible = sorted.filter { it.difficulty <= 1 }
                eligible.shuffled().take(targetCount).sortedBy { it.startTimeMs }
            }
            ProgressTier.HIGH -> {
                // All available keywords
                sorted
            }
        }
    }

    fun selectSummaryKeywords(shownDuringSong: List<SongKeyword>, tier: ProgressTier): List<SongKeyword> {
        if (shownDuringSong.isEmpty()) return emptyList()

        val byDifficulty = shownDuringSong.sortedByDescending { it.difficulty }

        val initialPicks = when (tier) {
            ProgressTier.LOW -> shownDuringSong.shuffled().take(5)
            ProgressTier.MEDIUM -> {
                val medium = byDifficulty.filter { it.difficulty == 1 }.shuffled()
                val easy   = byDifficulty.filter { it.difficulty == 0 }.shuffled()
                (medium + easy).take(5)
            }
            ProgressTier.HIGH -> byDifficulty.take(5)
        }

        return padToFive(initialPicks, shownDuringSong).shuffled()
    }

    private fun padToFive(picks: List<SongKeyword>, pool: List<SongKeyword>): List<SongKeyword> {
        if (picks.size >= 5) return picks.take(5)
        val result  = picks.toMutableList()
        val notYet  = pool.filter { it !in result }.shuffled()
        for (extra in notYet) {
            if (result.size >= 5) break
            result.add(extra)
        }
        while (result.size < 5 && pool.isNotEmpty()) {
            result.add(pool.random())
        }
        return result.take(5)
    }

    fun toWordList(keywords: List<SongKeyword>): ArrayList<String> =
        ArrayList(keywords.map { it.word })

    fun fromWordList(words: List<String>, songTitle: String): List<SongKeyword> {
        val pool = SONG_KEYWORDS[songTitle] ?: return emptyList()
        return words.mapNotNull { w -> pool.firstOrNull { it.word == w } }
    }

    const val EXTRA_SHOWN_KEYWORDS = "SHOWN_KEYWORD_WORDS"
    const val EXTRA_PROGRESS_TIER  = "PROGRESS_TIER"
}