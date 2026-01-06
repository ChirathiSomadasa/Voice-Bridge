package com.chirathi.voicebridge

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Timer
import java.util.TimerTask

class MusicPlayerActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var playPauseButton: ImageButton
    private lateinit var progressBar: SeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var totalTimeText: TextView
    private lateinit var lyricsTextView: TextView
    private lateinit var flashImage: ImageView
    private lateinit var likeButton: ImageButton
    private lateinit var songTitleTextView: TextView
    private var isPlaying = false
    private var timer: Timer? = null
    private val handler = Handler(Looper.getMainLooper())

    private var currentSongTitle = ""
    private var currentSongResource = 0
    private val likedSongs = mutableSetOf<String>()

    // Song-specific data
    private lateinit var currentLyrics: List<Lyric>
    private lateinit var currentKeywords: List<Keyword>

    // Track current keyword to avoid repeated showing
    private var currentKeyword: Keyword? = null

    data class Lyric(val text: String, val startTime: Int, val endTime: Int)
    data class Keyword(val word: String, val imageRes: Int, val startTime: Int, val endTime: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)

        // Get data from intent
        currentSongTitle = intent.getStringExtra("SONG_TITLE") ?: "Row Row Row Your Boat"
        currentSongResource = intent.getIntExtra("SONG_RESOURCE", R.raw.row_row_row_your_boat)

        // Get liked songs
        val likedSongsArray = intent.getStringArrayExtra("LIKED_SONGS")
        likedSongsArray?.let {
            likedSongs.addAll(it)
        }

        initializeViews()
        setupSongData()
        setupMediaPlayer()
        setupClickListeners()
        setupSeekBar()
    }

    private fun initializeViews() {
        playPauseButton = findViewById(R.id.playPauseButton)
        progressBar = findViewById(R.id.progressBar)
        currentTimeText = findViewById(R.id.currentTime)
        totalTimeText = findViewById(R.id.totalTime)
        lyricsTextView = findViewById(R.id.lyricsTextView)
        flashImage = findViewById(R.id.flashImage)
        likeButton = findViewById(R.id.likeButton)
        songTitleTextView = findViewById(R.id.songTitle)

        // Set song title
        songTitleTextView.text = currentSongTitle.uppercase()

        // Set initial like button state
        val isLiked = likedSongs.contains(currentSongTitle)
        likeButton.isSelected = isLiked
        val heartRes = if (isLiked) R.drawable.heart_filled else R.drawable.heart
        likeButton.setImageResource(heartRes)

        // Hide flash image initially
        flashImage.visibility = View.INVISIBLE
    }

    private fun setupSongData() {
        // Set lyrics and keywords based on current song
        when (currentSongTitle) {
            "Row Row Row Your Boat" -> {
                currentLyrics = listOf(
                    Lyric("Row, row, row your boat", 9000, 12000),
                    Lyric("Gently down the stream", 12000, 14000),
                    Lyric("Merrily, merrily, merrily, merrily", 14000, 17000),
                    Lyric("Life is but a dream", 17000, 20000),
                    Lyric("Row, row, row your boat", 21000, 22000),
                    Lyric("Gently down the creek", 22000, 25000),
                    Lyric("And if you see a little mouse,", 25000, 28000),
                    Lyric("Don't forget to squeak", 28000, 30000),
                    Lyric("Row, row, row your boat", 30000, 33000),
                    Lyric("Gently down the river", 33000, 35000),
                    Lyric("And if you see a polar bear,", 35000, 38000),
                    Lyric("Don' forget to shiver", 38000, 41000),
                    Lyric("Row, row, row your boat", 40000, 44000),
                    Lyric("Gently down the stream", 44000, 46000),
                    Lyric("And if you see a crocodile", 46000, 49000),
                    Lyric("Don' forget to scream", 49000, 51000),
                )
                currentKeywords = listOf(
                    Keyword("boat", R.drawable.boat_image, 11000, 12000),
                    Keyword("stream", R.drawable.stream_image, 13000, 14000),
                    Keyword("dream", R.drawable.dream_image, 18000, 20000),
                    Keyword("boat", R.drawable.boat_image, 21000, 23000),
                    Keyword("creek", R.drawable.creek, 24000, 25000),
                    Keyword("mouse", R.drawable.mouse_image, 27000, 28000),
                    Keyword("squeak", R.drawable.squeak, 29000, 30000),
                    Keyword("boat", R.drawable.boat_image, 32000, 33000),
                    Keyword("river", R.drawable.river_image, 34000, 36000),
                    Keyword("polar bear", R.drawable.polar_bear_image, 37000, 38000),
                    Keyword("shiver", R.drawable.shiver, 39000, 41000),
                    Keyword("boat", R.drawable.boat_image, 43000, 44000),
                    Keyword("stream", R.drawable.stream_image, 45000, 46000),
                    Keyword("crocodile", R.drawable.crocodile, 48000, 49000),
                    Keyword("scream", R.drawable.scream_image, 50000, 51000),
                )
            }
            "Twinkle Twinkle Little Star" -> {
                currentLyrics = listOf(
                    Lyric("Twinkle, twinkle, little star", 7000, 10000),
                    Lyric("How I wonder what you are", 10000, 15000),
                    Lyric("Up above the world so high", 15000, 19000),
                    Lyric("Like a diamond in the sky", 19000, 22000),
                    Lyric("Twinkle, twinkle, little star", 23000, 27000),
                    Lyric("How I wonder what you are", 27000, 31000),
                    Lyric("When the blazing sun is gone,", 39000, 43000),
                    Lyric("When he nothing shines upon,", 43000, 46000),
                    Lyric("Then you show your little light,", 46000, 51000),
                    Lyric("Twinkle, twinkle, all the night.", 51000, 55000),
                    Lyric("Twinkle, twinkle, little star", 55000, 58000),
                    Lyric("How I wonder what you are", 58000, 63000),
                    Lyric("Then the traveller in the dark,", 67000, 71000),
                    Lyric("Thanks you for your tiny spark,", 71000, 75000),
                    Lyric("He could not see which way to go,", 75000, 79000),
                    Lyric("If you did not twinkle so.", 79000, 83000),
                    Lyric("Twinkle, twinkle, little star", 83000, 86000),
                    Lyric("How I wonder what you are", 86000, 91000),
                    Lyric("In the dark blue sky you keep,", 99000, 103000),
                    Lyric("And often through my window peep,", 103000, 107000),
                    Lyric("For you never shut your eye,", 107000, 111000),
                    Lyric("Till the sun is in the sky.", 111000, 114000),
                    Lyric("Twinkle, twinkle, little star", 114000, 119000),
                    Lyric("How I wonder what you are", 119000, 124000),
                )
                currentKeywords = listOf(
                    Keyword("star", R.drawable.star_image, 8000, 10000),
                    Keyword("world", R.drawable.world, 16000, 18000),
                    Keyword("diamond", R.drawable.diamond_image, 19000, 20000),
                    Keyword("star", R.drawable.star_image, 25000, 27000),
                    Keyword("sun", R.drawable.sun, 40000, 42000),
                    Keyword("light", R.drawable.light, 48000, 50000),
                    Keyword("night", R.drawable.moon, 53000, 55000),
                    Keyword("star", R.drawable.star_image, 57000, 59000),
                    Keyword("traveller", R.drawable.traveller, 67000, 69000),
                    Keyword("star", R.drawable.star_image, 85000, 86000),
                    Keyword("dark blue sky", R.drawable.dark_blue_sky, 100000, 102000),
                    Keyword("window", R.drawable.window, 104000, 106000),
                    Keyword("eyes", R.drawable.eyes, 109000, 111000),
                    Keyword("sun", R.drawable.sun, 112000, 114000),
                    Keyword("star", R.drawable.star_image, 116000, 118000),
                )
            }
            "Jack and Jill" -> {
                currentLyrics = listOf(
                    Lyric("Jack and Jill went up the hill", 10000, 11000),
                    Lyric("To fetch a pail of water", 12000, 14000),
                    Lyric("Jack fell down and broke his crown", 14000, 16000),
                    Lyric("And Jill came tumbling after", 16000, 20000)
                )
                currentKeywords = listOf(
                    Keyword("hill", R.drawable.hill_image, 10500, 11000),
                    Keyword("water", R.drawable.water_image, 12500, 14000),
                    Keyword("crown", R.drawable.crown_image, 14500, 16000)
                )
            }
            else -> {
                currentLyrics = emptyList()
                currentKeywords = emptyList()
            }
        }
    }

    private fun setupMediaPlayer() {
        // Create media player with the selected song
        mediaPlayer = MediaPlayer.create(this, currentSongResource)
        mediaPlayer.setOnCompletionListener {
            isPlaying = false
            playPauseButton.setImageResource(R.drawable.sound)
            stopTimer()
            // Hide flash image when song ends
            flashImage.visibility = View.INVISIBLE

            // Navigate to RMIntroActivity after a short delay
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToRMIntroActivity()
            }, 1000) // 1 second delay before navigation
        }

        // Set total time
        val duration = mediaPlayer.duration
        totalTimeText.text = formatTime(duration)
        progressBar.max = duration
    }

    private fun navigateToRMIntroActivity() {
        val intent = Intent(this, RMIntroActivity::class.java)
        intent.putExtra("SONG_TITLE", currentSongTitle)

        // Pass liked songs if needed
        intent.putExtra("LIKED_SONGS", likedSongs.toTypedArray())

        startActivity(intent)
        // Don't finish() here if you want users to come back to the music player
        // finish()
    }

    private fun setupClickListeners() {
        playPauseButton.setOnClickListener {
            togglePlayPause()
        }

        findViewById<ImageButton>(R.id.prevButton).setOnClickListener {
            mediaPlayer.seekTo(0)
            progressBar.progress = 0
            updateLyricsAndKeywords(0)
        }

        findViewById<ImageButton>(R.id.nextButton).setOnClickListener {
            // In a real app, this would play next song
            mediaPlayer.seekTo(0)
            progressBar.progress = 0
            updateLyricsAndKeywords(0)
        }

        findViewById<ImageButton>(R.id.repeatButton).setOnClickListener {
            mediaPlayer.isLooping = !mediaPlayer.isLooping
            // You can change button color to indicate repeat state
        }

        likeButton.setOnClickListener {
            // Toggle like state
            it.isSelected = !it.isSelected
            val heartRes = if (it.isSelected) R.drawable.heart_filled else R.drawable.heart
            (it as ImageButton).setImageResource(heartRes)

            // Update liked songs set
            if (it.isSelected) {
                likedSongs.add(currentSongTitle)
            } else {
                likedSongs.remove(currentSongTitle)
            }
        }

        // Back to song selection with updated liked songs
        findViewById<View>(R.id.rootLayout).setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupSeekBar() {
        progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                    updateLyricsAndKeywords(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun togglePlayPause() {
        if (isPlaying) {
            pauseMusic()
        } else {
            playMusic()
        }
    }

    private fun playMusic() {
        mediaPlayer.start()
        isPlaying = true
        playPauseButton.setImageResource(R.drawable.pause)

        // Start updating progress
        startTimer()
    }

    private fun pauseMusic() {
        mediaPlayer.pause()
        isPlaying = false
        playPauseButton.setImageResource(R.drawable.sound)
        stopTimer()
    }

    private fun startTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.post {
                    if (mediaPlayer.isPlaying) {
                        val currentPosition = mediaPlayer.currentPosition
                        progressBar.progress = currentPosition
                        currentTimeText.text = formatTime(currentPosition)
                        updateLyricsAndKeywords(currentPosition)
                    }
                }
            }
        }, 0, 100) // Update every 100ms
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private fun updateLyricsAndKeywords(currentPosition: Int) {
        // Update lyrics
        val currentLyric = currentLyrics.find { currentPosition in it.startTime..it.endTime }
        lyricsTextView.text = currentLyric?.text ?: ""

        // Check for keywords
        currentKeywords.forEach { keyword ->
            if (currentPosition in keyword.startTime..keyword.endTime) {
                // Only show if it's a different keyword than currently shown
                if (currentKeyword != keyword) {
                    currentKeyword = keyword
                    showKeywordFlash(keyword)
                }
            } else if (currentPosition > keyword.endTime && currentKeyword == keyword) {
                // Hide the image if we've passed the keyword time
                currentKeyword = null
                hideKeywordFlash()
            }
        }
    }

    private fun showKeywordFlash(keyword: Keyword) {
        // Set the image
        flashImage.setImageResource(keyword.imageRes)
        // Show the image directly
        flashImage.visibility = View.VISIBLE
    }

    private fun hideKeywordFlash() {
        // Hide the image
        flashImage.visibility = View.INVISIBLE
    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onBackPressed() {
        // Return updated liked songs to SongSelectionActivity
        val resultIntent = Intent()
        resultIntent.putExtra("UPDATED_LIKED_SONGS", likedSongs.toTypedArray())
        setResult(RESULT_OK, resultIntent)

        releaseMediaPlayer()
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
    }

    private fun releaseMediaPlayer() {
        stopTimer()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }
}