package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.widget.NestedScrollView

class SongSelectionActivity : AppCompatActivity() {

    private val likedSongs = mutableSetOf<String>()
    private val songOrder = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_selection)

        // Initialize liked songs from shared preferences or intent
        loadLikedSongs()
        updateSongOrder()

        // Set up click listeners for each song card
        setupSongCards()
    }

    private fun loadLikedSongs() {
        // You can load liked songs from SharedPreferences here
        // For now, we'll use a simple set
        likedSongs.clear()
    }

    private fun updateSongOrder() {
        songOrder.clear()
        // Add liked songs first
        if (likedSongs.contains("Row Row Row Your Boat")) {
            songOrder.add("Row Row Row Your Boat")
        }
        if (likedSongs.contains("Twinkle Twinkle Little Star")) {
            songOrder.add("Twinkle Twinkle Little Star")
        }
        if (likedSongs.contains("Jack and Jill")) {
            songOrder.add("Jack and Jill")
        }

        // Add remaining songs
        if (!likedSongs.contains("Row Row Row Your Boat")) {
            songOrder.add("Row Row Row Your Boat")
        }
        if (!likedSongs.contains("Twinkle Twinkle Little Star")) {
            songOrder.add("Twinkle Twinkle Little Star")
        }
        if (!likedSongs.contains("Jack and Jill")) {
            songOrder.add("Jack and Jill")
        }
    }

    private fun setupSongCards() {
        // Row Row Row Your Boat
        val rowRowBoatLayout = findViewById<CardView>(R.id.rowRowBoatLayout)
        val playButtonContainerBoat = findViewById<View>(R.id.playButtonContainerBoat)
        val playButtonBoat = findViewById<ImageView>(R.id.playButtonBoat)
        val heartBoat = findViewById<ImageView>(R.id.heartBoat)

        // Check if this song is liked
        val isBoatLiked = likedSongs.contains("Row Row Row Your Boat")
        heartBoat.visibility = if (isBoatLiked) View.VISIBLE else View.GONE

        // Set play button click listener - click on the container or the button itself
        playButtonContainerBoat.setOnClickListener {
            navigateToMusicPlayer("Row Row Row Your Boat", R.raw.row_row_row_your_boat)
        }

        // Also set listener on the image button itself
        playButtonBoat.setOnClickListener {
            navigateToMusicPlayer("Row Row Row Your Boat", R.raw.row_row_row_your_boat)
        }

        // Heart click listener
        heartBoat.setOnClickListener {
            // Toggle like status
            if (likedSongs.contains("Row Row Row Your Boat")) {
                likedSongs.remove("Row Row Row Your Boat")
                heartBoat.visibility = View.GONE
            } else {
                likedSongs.add("Row Row Row Your Boat")
                heartBoat.visibility = View.VISIBLE
            }
            updateSongOrder()
            reorderSongCards()
        }

        // Whole card click listener (optional - if you want the whole card to be clickable)
        rowRowBoatLayout.setOnClickListener {
            navigateToMusicPlayer("Row Row Row Your Boat", R.raw.row_row_row_your_boat)
        }

        // Twinkle Twinkle Little Star
        val twinkleLayout = findViewById<CardView>(R.id.twinkleLayout)
        val playButtonContainerStar = findViewById<View>(R.id.playButtonContainerStar)
        val playButtonStar = findViewById<ImageView>(R.id.playButtonStar)
        val heartTwinkle = findViewById<ImageView>(R.id.heartTwinkle)

        val isTwinkleLiked = likedSongs.contains("Twinkle Twinkle Little Star")
        heartTwinkle.visibility = if (isTwinkleLiked) View.VISIBLE else View.GONE

        playButtonContainerStar.setOnClickListener {
            navigateToMusicPlayer("Twinkle Twinkle Little Star", R.raw.twinkle_twinkle)
        }

        playButtonStar.setOnClickListener {
            navigateToMusicPlayer("Twinkle Twinkle Little Star", R.raw.twinkle_twinkle)
        }

        heartTwinkle.setOnClickListener {
            if (likedSongs.contains("Twinkle Twinkle Little Star")) {
                likedSongs.remove("Twinkle Twinkle Little Star")
                heartTwinkle.visibility = View.GONE
            } else {
                likedSongs.add("Twinkle Twinkle Little Star")
                heartTwinkle.visibility = View.VISIBLE
            }
            updateSongOrder()
            reorderSongCards()
        }

        twinkleLayout.setOnClickListener {
            navigateToMusicPlayer("Twinkle Twinkle Little Star", R.raw.twinkle_twinkle)
        }

        // Jack and Jill
        val jackAndJillLayout = findViewById<CardView>(R.id.jackAndJillLayout)
        val playButtonContainerJack = findViewById<View>(R.id.playButtonContainerJack)
        val playButtonJack = findViewById<ImageView>(R.id.playButtonJack)
        val heartJack = findViewById<ImageView>(R.id.heartJack)

        val isJackLiked = likedSongs.contains("Jack and Jill")
        heartJack.visibility = if (isJackLiked) View.VISIBLE else View.GONE

        playButtonContainerJack.setOnClickListener {
            navigateToMusicPlayer("Jack and Jill", R.raw.jack_and_jill)
        }

        playButtonJack.setOnClickListener {
            navigateToMusicPlayer("Jack and Jill", R.raw.jack_and_jill)
        }

        heartJack.setOnClickListener {
            if (likedSongs.contains("Jack and Jill")) {
                likedSongs.remove("Jack and Jill")
                heartJack.visibility = View.GONE
            } else {
                likedSongs.add("Jack and Jill")
                heartJack.visibility = View.VISIBLE
            }
            updateSongOrder()
            reorderSongCards()
        }

        jackAndJillLayout.setOnClickListener {
            navigateToMusicPlayer("Jack and Jill", R.raw.jack_and_jill)
        }
    }

    private fun navigateToMusicPlayer(songTitle: String, songResource: Int) {
        val intent = Intent(this, MusicPlayerActivity::class.java)
        intent.putExtra("SONG_TITLE", songTitle)
        intent.putExtra("SONG_RESOURCE", songResource)
        intent.putExtra("LIKED_SONGS", likedSongs.toTypedArray())
        startActivityForResult(intent, 1)
    }

    private fun reorderSongCards() {
        // This is a simplified version. For a complete implementation,
        // you would need to remove and re-add the CardViews to the LinearLayout
        // based on the songOrder list

        val songsContainer = findViewById<androidx.core.widget.NestedScrollView>(R.id.songsContainer)
        // Note: In your XML, songsContainer is actually a NestedScrollView
        // The LinearLayout inside it has android:id="@+id/songsContainer"
        // but we need to access it properly

        // For now, we'll just update heart visibility
        // A full implementation would require more complex layout manipulation
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Update liked songs from MusicPlayerActivity
            val updatedLikedSongs = data?.getStringArrayExtra("UPDATED_LIKED_SONGS")
            updatedLikedSongs?.let {
                likedSongs.clear()
                likedSongs.addAll(it)
                updateSongOrder()
                // Refresh heart visibility
                setupSongCards()
            }
        }
    }
}