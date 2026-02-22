package com.chirathi.voicebridge

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * StickerGalleryActivity — shows all stickers the child has earned.
 *
 *  Earned stickers are displayed as large image cards with their name.
 *  Un-earned pool slots show a locked placeholder (greyed silhouette).
 *  Launched from the "My Stickers 🌟" button in RoutineSelectionActivity.
 *
 *  No extra XML layout required — built entirely in code.
 */
class StickerGalleryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout())
    }

    private fun buildLayout(): View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#FFF8E1"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 56, 32, 48)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // ── Back button ───────────────────────────────────────────────────────
        val backBtn = Button(this).apply {
            text     = "← Back"
            textSize = 16f
            setTextColor(Color.parseColor("#3F51B5"))
            setBackgroundColor(Color.TRANSPARENT)
            gravity  = Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { finish() }
        }
        root.addView(backBtn)

        // ── Title ─────────────────────────────────────────────────────────────
        val title = TextView(this).apply {
            text     = "⭐ My Sticker Collection"
            textSize = 26f
            gravity  = Gravity.CENTER
            setTextColor(Color.parseColor("#E65100"))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 8 }
        }
        root.addView(title)

        // ── Count subtitle ────────────────────────────────────────────────────
        val earned = StickerManager.earnedStickers(this)
        val total  = StickerManager.STICKER_POOL.size

        val subtitle = TextView(this).apply {
            text     = "You have ${earned.size} out of $total stickers! 🎉"
            textSize = 16f
            gravity  = Gravity.CENTER
            setTextColor(Color.parseColor("#5D4037"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 32 }
        }
        root.addView(subtitle)

        // ── Sticker grid (2 columns) ──────────────────────────────────────────
        val earnedIds = StickerManager.earnedIds(this)
        val grid      = GridLayout(this).apply {
            columnCount = 2
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        for (sticker in StickerManager.STICKER_POOL) {
            val isEarned = earnedIds.contains(sticker.id)
            val card     = buildStickerCard(sticker, isEarned)
            val lp       = GridLayout.LayoutParams().apply {
                width  = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(16, 16, 16, 16)
            }
            card.layoutParams = lp
            grid.addView(card)
        }
        root.addView(grid)

        // ── Progress bar toward next unlock ───────────────────────────────────
        root.addView(buildProgressSection(earned.size))

        scroll.addView(root)
        return scroll
    }

    private fun buildStickerCard(sticker: StickerManager.StickerInfo, isEarned: Boolean): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity     = Gravity.CENTER
            setPadding(20, 24, 20, 24)
            setBackgroundColor(if (isEarned) Color.parseColor("#FFFFFF") else Color.parseColor("#F5F5F5"))
            elevation = if (isEarned) 8f else 2f

            val img = ImageView(this@StickerGalleryActivity).apply {
                setImageResource(sticker.drawableRes)
                scaleType   = ImageView.ScaleType.CENTER_INSIDE
                alpha       = if (isEarned) 1f else 0.25f
                layoutParams = LinearLayout.LayoutParams(180, 180)
            }

            val nameLabel = TextView(this@StickerGalleryActivity).apply {
                text     = if (isEarned) sticker.name else "???"
                textSize = 14f
                gravity  = Gravity.CENTER
                setTextColor(if (isEarned) Color.parseColor("#333333") else Color.parseColor("#BDBDBD"))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = 8 }
            }

            val statusIcon = TextView(this@StickerGalleryActivity).apply {
                text     = if (isEarned) "✅ Earned!" else "🔒 Locked"
                textSize = 12f
                gravity  = Gravity.CENTER
                setTextColor(if (isEarned) Color.parseColor("#4CAF50") else Color.parseColor("#BDBDBD"))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = 4 }
            }

            addView(img); addView(nameLabel); addView(statusIcon)
        }
    }

    private fun buildProgressSection(earnedCount: Int): LinearLayout {
        val section = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 32, 0, 0)
        }

        val nextMilestone = when {
            earnedCount < 3 -> 3
            earnedCount < 6 -> 6
            else             -> StickerManager.STICKER_POOL.size
        }

        val progressLabel = TextView(this).apply {
            text = when {
                earnedCount < 3 -> "Collect ${3 - earnedCount} more sticker${if (3 - earnedCount != 1) "s" else ""} to unlock the Bedtime Routine!"
                earnedCount < 6 -> "Collect ${6 - earnedCount} more sticker${if (6 - earnedCount != 1) "s" else ""} to unlock the School Routine!"
                else             -> "🎊 All routines unlocked! You're a superstar!"
            }
            textSize = 15f
            gravity  = Gravity.CENTER
            setTextColor(Color.parseColor("#E65100"))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 16 }
        }
        section.addView(progressLabel)

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max      = nextMilestone
            progress = earnedCount.coerceAtMost(nextMilestone)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 32
            )
            progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800"))
        }
        section.addView(progressBar)

        val fracLabel = TextView(this).apply {
            text     = "$earnedCount / $nextMilestone"
            textSize = 14f
            gravity  = Gravity.CENTER
            setTextColor(Color.parseColor("#888888"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 8 }
        }
        section.addView(fracLabel)

        return section
    }
}