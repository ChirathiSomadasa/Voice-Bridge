package com.chirathi.voicebridge

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.GridLayout

class StickerGalleryActivity : AppCompatActivity() {

    private lateinit var backBtn          : ImageView
    private lateinit var tvStickerCount   : TextView
    private lateinit var stickerGrid      : GridLayout
    private lateinit var tvProgressLabel  : TextView
    private lateinit var progressBar      : ProgressBar
    private lateinit var tvProgressFraction: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sticker_gallery)

        backBtn             = findViewById(R.id.backBtn)
        tvStickerCount      = findViewById(R.id.tv_sticker_count)
        stickerGrid         = findViewById(R.id.stickerGrid)
        tvProgressLabel     = findViewById(R.id.tv_progress_label)
        progressBar         = findViewById(R.id.progressBar)
        tvProgressFraction  = findViewById(R.id.tv_progress_fraction)

        backBtn.setOnClickListener { finish() }

        populateGallery()
        populateProgress()
    }

    // ── Gallery grid ──────────────────────────────────────────────────────────

    private fun populateGallery() {
        val earned    = StickerManager.earnedStickers(this)
        val total     = StickerManager.STICKER_POOL.size
        val earnedIds = StickerManager.earnedIds(this)

        tvStickerCount.text = "You have ${earned.size} out of $total stickers! 🎉"

        stickerGrid.removeAllViews()

        for (sticker in StickerManager.STICKER_POOL) {
            val isEarned = earnedIds.contains(sticker.id)
            val card     = buildStickerCard(sticker, isEarned)
            val lp       = GridLayout.LayoutParams().apply {
                width      = 0
                height     = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(16, 16, 16, 16)
            }
            card.layoutParams = lp
            stickerGrid.addView(card)
        }
    }

    private fun buildStickerCard(sticker: StickerManager.StickerInfo, isEarned: Boolean): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity     = Gravity.CENTER
            setPadding(20, 24, 20, 24)
            setBackgroundColor(
                if (isEarned) Color.parseColor("#FFFFFF") else Color.parseColor("#F5F5F5")
            )
            elevation = if (isEarned) 8f else 2f

            val img = ImageView(this@StickerGalleryActivity).apply {
                setImageResource(sticker.drawableRes)
                scaleType    = ImageView.ScaleType.CENTER_INSIDE
                alpha        = if (isEarned) 1f else 0.25f
                layoutParams = LinearLayout.LayoutParams(180, 180)
            }

            val nameLabel = TextView(this@StickerGalleryActivity).apply {
                text      = if (isEarned) sticker.name else "???"
                textSize  = 14f
                gravity   = Gravity.CENTER
                setTextColor(
                    if (isEarned) Color.parseColor("#333333") else Color.parseColor("#BDBDBD")
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = 8 }
            }

            val statusIcon = TextView(this@StickerGalleryActivity).apply {
                text     = if (isEarned) "Earned! ⭐" else "Locked 🔒"
                textSize = 12f
                gravity  = Gravity.CENTER
                setTextColor(
                    if (isEarned) Color.parseColor("#4CAF50") else Color.parseColor("#BDBDBD")
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = 4 }
            }

            addView(img)
            addView(nameLabel)
            addView(statusIcon)
        }
    }

    // ── Progress section ──────────────────────────────────────────────────────

    private fun populateProgress() {
        val earnedCount   = StickerManager.earnedStickers(this).size
        val nextMilestone = when {
            earnedCount < 3 -> 3
            earnedCount < 6 -> 6
            else            -> StickerManager.STICKER_POOL.size
        }

        tvProgressLabel.text = when {
            earnedCount < 3 -> "Collect ${3 - earnedCount} more sticker${if (3 - earnedCount != 1) "s" else ""} to unlock the Bedtime Routine!"
            earnedCount < 6 -> "Collect ${6 - earnedCount} more sticker${if (6 - earnedCount != 1) "s" else ""} to unlock the School Routine!"
            else            -> "All routines unlocked! You're a superstar! 🎉"
        }

        progressBar.max      = nextMilestone
        progressBar.progress = earnedCount.coerceAtMost(nextMilestone)
        tvProgressFraction.text = "$earnedCount / $nextMilestone"
    }
}