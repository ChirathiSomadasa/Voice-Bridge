package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class SongSelectionActivity : AppCompatActivity() {

    private val TAG = "SongSelectionActivity"
    private val likedSongs = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_selection)

        // Safety: restore ChildSession if process was killed
        if (!ChildSession.isInitialized) ChildSession.restore(this)

        Log.d(TAG, "SongSelectionActivity: childId=${ChildSession.childId} age=${ChildSession.age}")

        loadLikedSongs()
        setupSongCards()

        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            startActivity(Intent(this, GameDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }
    }

    private fun loadLikedSongs() { likedSongs.clear() }

    private fun setupSongCards() {
        // Row Row Row Your Boat
        val rowRowBoatLayout        = findViewById<CardView>(R.id.rowRowBoatLayout)
        val playButtonContainerBoat = findViewById<View>(R.id.playButtonContainerBoat)
        val playButtonBoat          = findViewById<ImageView>(R.id.playButtonBoat)
        val heartBoat               = findViewById<ImageView>(R.id.heartBoat)

        heartBoat.visibility = if (likedSongs.contains("Row Row Row Your Boat")) View.VISIBLE else View.GONE

        val launchBoat = { navigateToMusicPlayer("Row Row Row Your Boat", R.raw.row_row_row_your_boat) }
        playButtonContainerBoat.setOnClickListener { launchBoat() }
        playButtonBoat.setOnClickListener { launchBoat() }
        rowRowBoatLayout.setOnClickListener { launchBoat() }
        heartBoat.setOnClickListener { toggleLike("Row Row Row Your Boat", heartBoat) }

        // Twinkle Twinkle Little Star
        val twinkleLayout           = findViewById<CardView>(R.id.twinkleLayout)
        val playButtonContainerStar = findViewById<View>(R.id.playButtonContainerStar)
        val playButtonStar          = findViewById<ImageView>(R.id.playButtonStar)
        val heartTwinkle            = findViewById<ImageView>(R.id.heartTwinkle)

        heartTwinkle.visibility = if (likedSongs.contains("Twinkle Twinkle Little Star")) View.VISIBLE else View.GONE

        val launchTwinkle = { navigateToMusicPlayer("Twinkle Twinkle Little Star", R.raw.twinkle_twinkle) }
        playButtonContainerStar.setOnClickListener { launchTwinkle() }
        playButtonStar.setOnClickListener { launchTwinkle() }
        twinkleLayout.setOnClickListener { launchTwinkle() }
        heartTwinkle.setOnClickListener { toggleLike("Twinkle Twinkle Little Star", heartTwinkle) }

        /*
        // Jack and Jill (Temporarily disabled)
        val jackAndJillLayout       = findViewById<CardView>(R.id.jackAndJillLayout)
        val playButtonContainerJack = findViewById<View>(R.id.playButtonContainerJack)
        val playButtonJack          = findViewById<ImageView>(R.id.playButtonJack)
        val heartJack               = findViewById<ImageView>(R.id.heartJack)

        heartJack.visibility = if (likedSongs.contains("Jack and Jill")) View.VISIBLE else View.GONE

        val launchJack = { navigateToMusicPlayer("Jack and Jill", R.raw.jack_and_jill) }
        playButtonContainerJack.setOnClickListener { launchJack() }
        playButtonJack.setOnClickListener { launchJack() }
        jackAndJillLayout.setOnClickListener { launchJack() }
        heartJack.setOnClickListener { toggleLike("Jack and Jill", heartJack) }
        */
    }

    private fun toggleLike(title: String, heart: ImageView) {
        if (likedSongs.contains(title)) {
            likedSongs.remove(title); heart.visibility = View.GONE
        } else {
            likedSongs.add(title); heart.visibility = View.VISIBLE
        }
    }

    /**
     * Computes the tier from the child's profile BEFORE launching the music player.
     * This ensures the music player knows which flashcards to show before the song starts.
     * The child freely chose the song — we only decide how many flashcards to show.
     */
    private fun navigateToMusicPlayer(songTitle: String, songResource: Int) {
        val profile = ChildProfileManager.load(this, ChildSession.childId)
        val tier    = RhythmFlashcardManager.determineTierFromProfile(profile)

        // Pre-select song flashcards so MusicPlayerActivity doesn't have to recompute
        val songFlashcards = RhythmFlashcardManager.selectSongFlashcards(songTitle, tier)
        val shownWords     = RhythmFlashcardManager.toWordList(songFlashcards)

        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "Launching MusicPlayer:")
        Log.d(TAG, "  child=${ChildSession.childId}  age=${ChildSession.age}")
        Log.d(TAG, "  song=$songTitle")
        Log.d(TAG, "  tier=$tier  (rhythmAccuracy=${profile?.rhythmAccuracy})")
        Log.d(TAG, "  flashcards to show (${shownWords.size}): $shownWords")
        Log.d(TAG, "═══════════════════════════════════════")

        startActivityForResult(
            Intent(this, MusicPlayerActivity::class.java).apply {
                putExtra("SONG_TITLE",    songTitle)
                putExtra("SONG_RESOURCE", songResource)
                putStringArrayListExtra("LIKED_SONGS", ArrayList(likedSongs))
                // These two extras carry the flashcard decision to MusicPlayerActivity
                putStringArrayListExtra(RhythmFlashcardManager.EXTRA_SHOWN_KEYWORDS, shownWords)
                putExtra(RhythmFlashcardManager.EXTRA_PROGRESS_TIER, tier.name)
            },
            1
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val updatedLikedSongs = data?.getStringArrayExtra("UPDATED_LIKED_SONGS")
            updatedLikedSongs?.let { likedSongs.addAll(it); setupSongCards() }
        }
    }
}