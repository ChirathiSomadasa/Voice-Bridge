package com.chirathi.voicebridge

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import kotlin.math.abs

object KeywordImageMapper {

    // =========== COMPLETE IMAGE MAPPING FOR ALL KEYWORDS ===========

    // Primary keywords (from songs)
    val primaryKeywords = mapOf(
        // Row Row Row Your Boat
        "boat" to R.drawable.rhy_song0_boat,
        "stream" to R.drawable.rhy_song0_stream,
        "dream" to R.drawable.rhy_song0_dream,
        "creek" to R.drawable.rhy_song0_creek,
        "mouse" to R.drawable.rhy_song0_mouse,
        "river" to R.drawable.rhy_song0_river,
        "polar bear" to R.drawable.rhy_song0_polar_bear,
        "crocodile" to R.drawable.rhy_song0_crocodile,

        // Twinkle Twinkle Little Star
        "star" to R.drawable.rhy_song1_star,
        "world" to R.drawable.rhy_song1_world,
        "diamond" to R.drawable.rhy_song1_diamond,
        "sun" to R.drawable.rhy_song1_sun,
        "light" to R.drawable.rhy_song1_light,
        "night" to R.drawable.rhy_song1_moon,
        "traveller" to R.drawable.rhy_song1_traveller,
        "dark blue sky" to R.drawable.rhy_song1_dark_blue_sky,
        "window" to R.drawable.rhy_song1_window,
        "eyes" to R.drawable.rhy_song1_eyes,

        // Jack and Jill
        "hill" to R.drawable.hill_image,
        "water" to R.drawable.water_image,
        "crown" to R.drawable.crown_image
    )

    // =========== PHONETIC DISTRACTORS (Rhyming Words) ===========
    val phoneticDistractors = mapOf(
        // Boat rhymes
        "coat" to R.drawable.coat,
        "goat" to R.drawable.goat,
        "float" to R.drawable.float_ring,
        "moat" to R.drawable.moat,
        "note" to R.drawable.note,

        // Stream rhymes
        "dream" to R.drawable.rhy_song0_dream,
//        "cream" to R.drawable.cream,
//        "beam" to R.drawable.beam,
//        "team" to R.drawable.team,
//        "seam" to R.drawable.seam,

        // Creek rhymes
        "squeak" to R.drawable.squeak,
//        "beak" to R.drawable.beak,
//        "leak" to R.drawable.leak,
//        "peak" to R.drawable.peak,
//        "weak" to R.drawable.weak,

        // Mouse rhymes
//        "house" to R.drawable.house,
//        "louse" to R.drawable.louse,
//        "douse" to R.drawable.douse,
//        "grouse" to R.drawable.grouse,

        // River rhymes
        "shiver" to R.drawable.shiver,
//        "liver" to R.drawable.liver,
//        "giver" to R.drawable.giver,
//        "quiver" to R.drawable.quiver,

        // Star rhymes
        "car" to R.drawable.car,
//        "far" to R.drawable.far,
//        "bar" to R.drawable.bar,
//        "jar" to R.drawable.jar,
//        "tar" to R.drawable.tar,

        // World rhymes
//        "whirled" to R.drawable.whirled,
//        "hurled" to R.drawable.hurled,
//        "curled" to R.drawable.curled,
//        "swirled" to R.drawable.swirled,

        // Diamond rhymes
//        "lemon" to R.drawable.lemon,
//        "demon" to R.drawable.demon,
//        "common" to R.drawable.common,
//        "salmon" to R.drawable.salmon,

        // Sun rhymes
        "run" to R.drawable.run,
//        "fun" to R.drawable.fun,
//        "bun" to R.drawable.bun,
//        "gun" to R.drawable.gun,
//        "nun" to R.drawable.nun,

        // Light rhymes
        "night" to R.drawable.rhy_song1_moon,
//        "bright" to R.drawable.bright,
//        "sight" to R.drawable.sight,
//        "fight" to R.drawable.fight,
        "kite" to R.drawable.kite,

        // Hill rhymes
//        "pill" to R.drawable.pill,
//        "mill" to R.drawable.mill,
//        "fill" to R.drawable.fill,
//        "will" to R.drawable.will,
//        "bill" to R.drawable.bill,

        // Water rhymes
//        "daughter" to R.drawable.daughter,
//        "otter" to R.drawable.otter,
//        "quarter" to R.drawable.quarter,
//        "slaughter" to R.drawable.slaughter,
//        "porter" to R.drawable.porter,

        // Crown rhymes
//        "brown" to R.drawable.brown,
//        "clown" to R.drawable.clown,
//        "drown" to R.drawable.drown,
//        "frown" to R.drawable.frown,
//        "gown" to R.drawable.gown
    )

    // =========== SEMANTIC DISTRACTORS (Similar Meaning) ===========
    val semanticDistractors = mapOf(
        // Boat meanings
        "anchor" to R.drawable.anchor,
//        "sail" to R.drawable.sail,
        "oar" to R.drawable.oar,
//        "lifejacket" to R.drawable.lifejacket,
//        "ship" to R.drawable.ship,
//        "vessel" to R.drawable.vessel,
//        "canoe" to R.drawable.canoe,
//        "kayak" to R.drawable.kayak,
//        "raft" to R.drawable.raft,
//        "dinghy" to R.drawable.dinghy,

        // Stream meanings
        "river" to R.drawable.rhy_song0_river,
        "creek" to R.drawable.rhy_song0_creek,
//        "brook" to R.drawable.brook,
//        "canal" to R.drawable.canal,
//        "tributary" to R.drawable.tributary,

        // Star meanings
        "moon" to R.drawable.rhy_song1_moon,
        "planet" to R.drawable.planet,
//        "comet" to R.drawable.comet,
//        "galaxy" to R.drawable.galaxy,

        // Sun meanings
//        "solar" to R.drawable.solar,
//        "day" to R.drawable.day,
//        "bright" to R.drawable.bright,

        // Hill meanings
//        "mountain" to R.drawable.mountain,
//        "slope" to R.drawable.slope,
//        "elevation" to R.drawable.elevation,
//        "peak" to R.drawable.peak,

        // Water meanings
//        "h2o" to R.drawable.h2o,
//        "liquid" to R.drawable.liquid,
//        "aqua" to R.drawable.aqua,
//        "fluid" to R.drawable.fluid,

        // Crown meanings
//        "tiara" to R.drawable.tiara,
//        "royalty" to R.drawable.royalty,
//        "king" to R.drawable.king,
//        "queen" to R.drawable.queen
    )

    // =========== VISUAL DISTRACTORS (Similar Appearance) ===========
    val visualDistractors = mapOf(
        // Boat visuals
//        "canoe" to R.drawable.canoe,
//        "kayak" to R.drawable.kayak,
//        "raft" to R.drawable.raft,

        // Stream visuals
//        "wave" to R.drawable.wave,
//        "waterfall" to R.drawable.waterfall,
//        "rapids" to R.drawable.rapids,

        // Star visuals
        "sparkle" to R.drawable.sparkle,
        "twinkle" to R.drawable.twinkle,

        // Diamond visuals
//        "square" to R.drawable.square,
//        "circle" to R.drawable.circle,
//        "triangle" to R.drawable.triangle,
//        "oval" to R.drawable.oval,
//        "rectangle" to R.drawable.rectangle,

        // Sun visuals
        "yellow" to R.drawable.yellow,
//        "orange" to R.drawable.orange,

        // Mouse visuals
        "cat" to R.drawable.cat,
        "cheese" to R.drawable.cheese,

        // Eye visuals
//        "nose" to R.drawable.nose,
//        "ears" to R.drawable.ears,
//        "mouth" to R.drawable.mouth,
//        "face" to R.drawable.face
    )

    // =========== RANDOM DISTRACTORS (No Association) ===========
    val randomDistractors = mapOf(
//        "apple" to R.drawable.apple,
        "ball" to R.drawable.ball,
        "car" to R.drawable.car,
//        "house" to R.drawable.house,
//        "tree" to R.drawable.tree,
//        "book" to R.drawable.book,
        "cat" to R.drawable.cat,
//        "dog" to R.drawable.dog,
//        "bird" to R.drawable.bird,
//        "fish" to R.drawable.fish,
//        "flower" to R.drawable.flower,
        "cloud" to R.drawable.cloud
    )

    // =========== COMPLETE MASTER MAP ===========
    val allImages = primaryKeywords +
            phoneticDistractors +
            semanticDistractors +
            visualDistractors +
            randomDistractors

    // =========== SMART IMAGE RESOLVER ===========
    fun getImageResource(word: String): Int {
        val lowercaseWord = word.lowercase()
        return allImages[lowercaseWord] ?: 0
    }

    // =========== CHECK IF IMAGE EXISTS ===========
    fun hasImageResource(word: String): Boolean {
        return getImageResource(word) != 0
    }

    // =========== GET CONSISTENT COLOR FOR WORD ===========
    fun getColorForWord(word: String): Int {
        val colors = listOf(
            "#FFB74D", // Orange
            "#64B5F6", // Blue
            "#81C784", // Green
            "#E57373", // Red
            "#BA68C8", // Purple
            "#FFD54F", // Yellow
            "#4FC3F7", // Light Blue
            "#A1887F", // Brown
            "#90A4AE", // Grey
            "#F06292"  // Pink
        )
        val hash = word.lowercase().hashCode()
        return Color.parseColor(colors[abs(hash % colors.size)])
    }

    // =========== CREATE COLORFUL TEXT CARD ===========
    fun createTextCard(context: Context, word: String, size: Int): TextView {
        return TextView(context).apply {
            layoutParams = ViewGroup.LayoutParams(size, size)
            gravity = Gravity.CENTER
            text = word.uppercase()
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.BLACK)
            setBackgroundColor(getColorForWord(word))
            setPadding(16, 16, 16, 16)
        }
    }

    // =========== CREATE CARD WITH IMAGE OR TEXT ===========
    fun createOptionCardView(
        context: Context,
        word: String,
        imageResId: Int,
        size: Int,
        isSimplified: Boolean = false
    ): View {
        // If we have a valid image resource, use ImageView
        if (imageResId != 0) {
            return ImageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(size, size)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(imageResId)
                contentDescription = word
                setPadding(8, 8, 8, 8)
            }
        }
        // Otherwise create colorful text card
        else {
            return createTextCard(context, word, size)
        }
    }

    // =========== CREATE SIMPLIFIED CARD WITH IMAGE AND TEXT ===========
    fun createSimplifiedCardView(
        context: Context,
        word: String,
        imageResId: Int,
        size: Int
    ): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(size, size)
        }

        // Add image if available
        if (imageResId != 0) {
            val imageView = ImageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    (size * 0.6).toInt(),
                    (size * 0.6).toInt()
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(imageResId)
                contentDescription = word
            }
            container.addView(imageView)
        }

        // ALWAYS add text label
        val textView = TextView(context).apply {
            text = word.uppercase()
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
            }
        }
        container.addView(textView)

        // If no image, set background color
        if (imageResId == 0) {
            container.setBackgroundColor(getColorForWord(word))
            container.setPadding(16, 16, 16, 16)
        }

        return container
    }

    // =========== VALIDATE ALL IMAGES ===========
    fun validateAllImages(): Map<String, Int> {
        val missingImages = mutableMapOf<String, Int>()

        allImages.forEach { (word, resId) ->
            // This just logs, doesn't actually check if resource exists at runtime
            // For actual validation, you'd need context
        }

        return missingImages
    }
}