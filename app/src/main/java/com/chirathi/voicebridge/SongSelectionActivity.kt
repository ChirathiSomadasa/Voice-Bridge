package com.chirathi.voicebridge

import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class SongSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_selection)

        // Example: Twinkle Twinkle play button
        val playButtonTwinkle = findViewById<ImageButton>(R.id.playButtonTwinkle)
        val mascotTwinkle = findViewById<ImageView>(R.id.mascotTwinkle)

        playButtonTwinkle.setOnClickListener {
            // Animate mascot peek
            mascotTwinkle.visibility = View.VISIBLE
            mascotTwinkle.animate().translationY(-20f).setDuration(300).start()

            // Animate sound waves
            val drawable = playButtonTwinkle.drawable
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
            }

            // Update stars (example: 2 plays)
            updateStars(R.id.starContainerTwinkle, 2)

            // Navigate to MusicPlayerActivity
            val intent = Intent(this, MusicPlayerActivity::class.java)
            intent.putExtra("song_title", "Twinkle Twinkle Little Star")
            intent.putExtra("song_duration", "1:05")
            startActivity(intent)
        }
    }

    private fun updateStars(containerId: Int, playCount: Int) {
        val starContainer = findViewById<LinearLayout>(containerId)
        for (i in 0 until starContainer.childCount) {
            val star = starContainer.getChildAt(i) as ImageView
            if (i < playCount) {
                star.setImageResource(R.drawable.star_filled)
            } else {
                star.setImageResource(R.drawable.star_empty)
            }
        }
    }
}
