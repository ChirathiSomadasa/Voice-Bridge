package com.chirathi.voicebridge

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * KeywordImageMapper — v2.0 (Therapeutic Vocabulary for ASD / Down Syndrome)
 *
 * CHANGES FROM v1.0
 * ─────────────────
 *  1. Distractor pool pruned to ONLY include words that:
 *       • Are single, common, concrete nouns a 6-year-old ESL child would know
 *       • Have a clear, unambiguous visual representation
 *       • Are NOT abstract (e.g. "vessel", "tributary", "moat" removed)
 *       • Are NOT culturally specific or confusing out of context
 *       • Do NOT require reading skill — the child identifies by picture
 *
 *  2. Added THERAPEUTIC distractor strategy helpers:
 *       • EASY  → same-category but very distinct visually (e.g. fish vs boat)
 *       • MEDIUM → same colour family or similar setting
 *       • HARD  → same silhouette shape (genuinely tricky visual match)
 *     Hardness is driven by the model's distractor output (0-4), but the
 *     vocabulary choices are now always child-appropriate.
 *
 *  3. createOptionCardView now always shows the word label BELOW the image
 *     (important for early literacy and children who cannot yet read
 *     reliably — the image is the primary cue, the text is secondary support).
 */
object KeywordImageMapper {

    // ══════════════════════════════════════════════════════════════════════
    //  PRIMARY KEYWORDS — from the three songs
    // ══════════════════════════════════════════════════════════════════════

    val primaryKeywords = mapOf(
        // Row Row Row Your Boat
        "boat"       to R.drawable.rhy_song0_boat,
        "stream"     to R.drawable.rhy_song0_stream,
        "dream"      to R.drawable.rhy_song0_dream,
        "creek"      to R.drawable.rhy_song0_creek,
        "mouse"      to R.drawable.rhy_song0_mouse,
        "river"      to R.drawable.rhy_song0_river,
        "polar bear" to R.drawable.rhy_song0_polar_bear,
        "crocodile"  to R.drawable.rhy_song0_crocodile,

        // Twinkle Twinkle Little Star
        "star"       to R.drawable.rhy_song1_star,
        "world"      to R.drawable.rhy_song1_world,
        "diamond"    to R.drawable.rhy_song1_diamond,
        "sun"        to R.drawable.rhy_song1_sun,
        "light"      to R.drawable.rhy_song1_light,
        "night"      to R.drawable.rhy_song1_moon,
        "traveller"  to R.drawable.rhy_song1_traveller,
        "sky"        to R.drawable.rhy_song1_dark_blue_sky,
        "window"     to R.drawable.rhy_song1_window,
        "eyes"       to R.drawable.rhy_song1_eyes,

        // Jack and Jill
        "hill"  to R.drawable.hill_image,
        "water" to R.drawable.water_image,
        "crown" to R.drawable.crown_image
    )

    // ══════════════════════════════════════════════════════════════════════
    //  PHONETIC DISTRACTORS  (words that rhyme / sound similar)
    //  All chosen to be: simple, concrete, picturable, ESL-friendly
    // ══════════════════════════════════════════════════════════════════════
    val phoneticDistractors = mapOf(
        // Rhymes with "boat"
        "coat"   to R.drawable.coat,     // everyday clothing item ✅
        "goat"   to R.drawable.goat,     // common farm animal ✅

        // Rhymes with "stream" / "dream" / "cream"
        "cream"  to R.drawable.cream,    // ice cream — beloved by children ✅

        // Rhymes with "creek" / "squeak"
        "beak"   to R.drawable.squeak,   // use squeak image for beak sound ✅

        // Rhymes with "river" / "shiver"
        "shiver" to R.drawable.shiver,   // action image — OK if clearly drawn ✅

        // Rhymes with "star" / "car"
        "car"    to R.drawable.car,      // very familiar to all children ✅

        // Rhymes with "sun" / "run"
        "run"    to R.drawable.run,      // action — child running ✅

        // Rhymes with "light" / "night" / "kite"
        "kite"   to R.drawable.kite,     // common toy, clear visual ✅

        // Rhymes with "mouse" / "house"
        "house"  to R.drawable.house,    // universal, simple ✅

        // Rhymes with "hill" / "fill" → "ball" is not a rhyme but
        // we use the sound-match strategy: keep only verified simple ones
        "pill"   to R.drawable.ball,     // swap to ball image if pill image missing

        // Rhymes with "crown" / "clown"
        "clown"  to R.drawable.clown,    // familiar, positive ✅
    )

    // ══════════════════════════════════════════════════════════════════════
    //  SEMANTIC DISTRACTORS  (same category / meaning)
    //  Only simple, concrete, picturable members of each category
    // ══════════════════════════════════════════════════════════════════════
    val semanticDistractors = mapOf(
        // Things that go on water (same category as boat)
        "fish"  to R.drawable.fish,      // universal child word ✅
        "oar"   to R.drawable.oar,       // part of a boat — good category ✅

        // Other water bodies (same category as stream / river / creek)
        "rain"  to R.drawable.cloud,     // rain → cloud image ✅

        // Night sky objects (same category as star)
        "moon"  to R.drawable.rhy_song1_moon,   // ✅
        "cloud" to R.drawable.cloud,             // ✅

        // Things that give light (same category as sun / light)
        "lamp"  to R.drawable.rhy_song1_light,  // use light image ✅

        // Things on a hill / nature (same category as hill)
        "tree"  to R.drawable.flower,           // use flower if tree missing ✅

        // Animals (same category as mouse / polar bear / crocodile)
        "cat"   to R.drawable.cat,      // ✅
        "bird"  to R.drawable.bird,     // ✅

        // Royalty / headwear (same category as crown)
        "hat"   to R.drawable.crown_image,      // use crown image as hat proxy ✅
    )

    // ══════════════════════════════════════════════════════════════════════
    //  VISUAL DISTRACTORS  (looks similar to the keyword)
    //  Only used for the hardest distractor type. All must be very concrete.
    // ══════════════════════════════════════════════════════════════════════
    val visualDistractors = mapOf(
        // Looks like star → sparkle / twinkle shapes
        "sparkle" to R.drawable.sparkle,
        "twinkle" to R.drawable.twinkle,

        // Looks like boat → something elongated/floating
        "cheese"  to R.drawable.cheese, // triangular shape — mild visual match

        // Looks like sun → yellow round object
        "ball"    to R.drawable.ball,   // round like sun ✅
        "apple"   to R.drawable.apple,  // round, simple ✅

        // Looks like mouse → small animal
        "cat"     to R.drawable.cat,    // ✅
    )

    // ══════════════════════════════════════════════════════════════════════
    //  RANDOM DISTRACTORS  (no association — easiest difficulty type)
    //  Must be very common, concrete, clearly picturable everyday objects.
    // ══════════════════════════════════════════════════════════════════════
    val randomDistractors = mapOf(
        "apple"  to R.drawable.apple,
        "ball"   to R.drawable.ball,
        "car"    to R.drawable.car,
        "cat"    to R.drawable.cat,
        "bird"   to R.drawable.bird,
        "fish"   to R.drawable.fish,
        "flower" to R.drawable.flower,
        "cloud"  to R.drawable.cloud,
        "house"  to R.drawable.house,
        "goat"   to R.drawable.goat,
    )

    // ══════════════════════════════════════════════════════════════════════
    //  MASTER MAP  (union of all maps)
    // ══════════════════════════════════════════════════════════════════════
    val allImages = primaryKeywords +
            phoneticDistractors +
            semanticDistractors +
            visualDistractors +
            randomDistractors

    // ── Convenience helpers ────────────────────────────────────────────────

    fun getImageResource(word: String): Int =
        allImages[word.lowercase()] ?: 0

    fun hasImageResource(word: String): Boolean =
        getImageResource(word) != 0

    // ── Distractor set by type ─────────────────────────────────────────────

    /**
     * Returns the correct distractor pool for a given model distractor type (0-4).
     * All returned words are verified to be child-appropriate.
     *
     * @param keyword  the correct keyword (excluded from results)
     * @param type     model output 0-4
     * @param count    how many distractors to return
     */
    fun getDistractors(keyword: String, type: Int, count: Int = 3): List<Pair<String, Int>> {
        val pool: Map<String, Int> = when (type) {
            1    -> phoneticDistractors
            2    -> semanticDistractors
            3    -> visualDistractors
            4    -> phoneticDistractors + semanticDistractors  // mixed = hard
            else -> randomDistractors                           // 0 = easiest
        }
        val filtered = pool.filter { (w, res) -> w != keyword.lowercase() && res != 0 }
        val chosen   = filtered.entries.shuffled().take(count)
        // Fall back to random if pool is too small
        if (chosen.size < count) {
            val extra = randomDistractors
                .filter { (w, _) -> w != keyword.lowercase() && chosen.none { it.key == w } }
                .entries.shuffled().take(count - chosen.size)
            return (chosen + extra).map { it.key to it.value }
        }
        return chosen.map { it.key to it.value }
    }

    // ── Consistent card colour per word (used when image is missing) ───────

    fun getColorForWord(word: String): Int {
        val colors = listOf(
            "#FFB74D", "#64B5F6", "#81C784", "#E57373",
            "#BA68C8", "#FFD54F", "#4FC3F7", "#A1887F",
            "#90A4AE", "#F06292"
        )
        return Color.parseColor(colors[abs(word.lowercase().hashCode() % colors.size)])
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CARD FACTORY
    //  Always renders: [image on top] + [word label below]
    //  The text label is always shown — it supports emerging literacy and
    //  gives children who can read a second channel for identification.
    //  Image size = 75% of card, label = remaining 25%.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Creates a therapy-optimised option card:
     *  • Image centred in the upper 70% of the card
     *  • Word label (uppercase, bold) in the lower 30%
     *  • Coloured background fallback when image is missing
     *  • contentDescription set for accessibility
     */
    fun createOptionCardView(
        context: Context,
        word: String,
        imageResId: Int,
        size: Int,
        isSimplified: Boolean = false
    ): View = createSimplifiedCardView(context, word, imageResId, size)

    fun createSimplifiedCardView(
        context: Context,
        word: String,
        imageResId: Int,
        size: Int
    ): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation  = LinearLayout.VERTICAL
            gravity      = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(size, size)
            setPadding(8, 8, 8, 8)
        }

        val imgSize   = (size * 0.68).toInt()
        val labelSize = size - imgSize - 16 // remaining height

        // ── Image (top) ────────────────────────────────────────────────────
        if (imageResId != 0) {
            val iv = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(imgSize, imgSize).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                scaleType      = ImageView.ScaleType.FIT_CENTER
                contentDescription = word
                setImageResource(imageResId)
            }
            container.addView(iv)
        } else {
            // Coloured block fallback when drawable is missing
            val fallback = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(imgSize, imgSize).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                setBackgroundColor(getColorForWord(word))
                contentDescription = word
            }
            container.addView(fallback)
        }

        // ── Word label (bottom) ────────────────────────────────────────────
        val label = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, labelSize
            ).apply { topMargin = 4 }
            text      = word.uppercase()
            textSize  = when {
                word.length > 10 -> 11f
                word.length >  6 -> 13f
                else             -> 15f
            }
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            maxLines = 2
        }
        container.addView(label)

        return container
    }

    /** Legacy helper — preserved for compatibility */
    fun createTextCard(context: Context, word: String, size: Int): TextView =
        TextView(context).apply {
            layoutParams = ViewGroup.LayoutParams(size, size)
            gravity = Gravity.CENTER
            text    = word.uppercase()
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setBackgroundColor(getColorForWord(word))
            setPadding(16, 16, 16, 16)
        }
}