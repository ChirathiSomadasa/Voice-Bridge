package com.chirathi.voicebridge

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Timer
import java.util.TimerTask
import kotlin.math.roundToInt

class MusicPlayerActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var playPauseButton: ImageButton
    private lateinit var progressBar: SeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var totalTimeText: TextView
    private lateinit var lyricsTextView: TextView
    private lateinit var flashImage: ImageView
    private var isPlaying = false
    private var timer: Timer? = null
    private val handler = Handler(Looper.getMainLooper())

    // Lyrics data with timestamps (in milliseconds)
    private val lyrics = listOf(
        Lyric("Row, row, row your boat", 20000, 23000),
        Lyric("Gently down the stream", 23000, 25000),
        Lyric("Merrily, merrily, merrily, merrily", 25000, 29000),
        Lyric("Life is but a dream", 29000, 31000)
    )

    // Keywords with corresponding image resources and timestamps
    private val keywords = listOf(
        Keyword("boat", R.drawable.boat_image, 22000, 23000),
        Keyword("stream", R.drawable.stream_image, 24000, 25000),
        Keyword("dream", R.drawable.dream_image, 30000, 31000)
    )

    data class Lyric(val text: String, val startTime: Int, val endTime: Int)
    data class Keyword(val word: String, val imageRes: Int, val startTime: Int, val endTime: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)

        initializeViews()
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

        // Hide flash image initially
        flashImage.visibility = View.INVISIBLE
    }

    private fun setupMediaPlayer() {
        // Create media player with the song
        mediaPlayer = MediaPlayer.create(this, R.raw.row_row_row_your_boat)
        mediaPlayer.setOnCompletionListener {
            isPlaying = false
            playPauseButton.setImageResource(R.drawable.sound)
            stopTimer()
        }

        // Set total time
        val duration = mediaPlayer.duration
        totalTimeText.text = formatTime(duration)
        progressBar.max = duration
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

        findViewById<ImageButton>(R.id.likeButton).setOnClickListener {
            // Toggle like state
            it.isSelected = !it.isSelected
            val heartRes = if (it.isSelected) R.drawable.heart_filled else R.drawable.heart
            (it as ImageButton).setImageResource(heartRes)
        }

        // Back to dashboard - tap anywhere outside main content
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
        val currentLyric = lyrics.find { currentPosition in it.startTime..it.endTime }
        lyricsTextView.text = currentLyric?.text ?: ""

        // Check for keywords
        keywords.forEach { keyword ->
            if (currentPosition in keyword.startTime..keyword.endTime) {
                showKeywordFlash(keyword)
            }
        }
    }

    private fun showKeywordFlash(keyword: Keyword) {
        // Set the image
        flashImage.setImageResource(keyword.imageRes)

        // Create fade in/out animation
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 300

        val fadeOut = AlphaAnimation(1f, 0f)
        fadeOut.duration = 300
        fadeOut.startOffset = 1500 // Show for 1.5 seconds

        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                flashImage.visibility = View.INVISIBLE
            }
        })

        flashImage.visibility = View.VISIBLE
        flashImage.startAnimation(fadeIn)
        flashImage.postDelayed({
            flashImage.startAnimation(fadeOut)
        }, 1800)
    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onBackPressed() {
        releaseMediaPlayer()
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
    }

    private fun releaseMediaPlayer() {
        stopTimer()
        mediaPlayer.release()
    }
}