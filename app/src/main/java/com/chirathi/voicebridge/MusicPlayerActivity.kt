package com.chirathi.voicebridge

import android.content.Intent
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
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

/**
 * MusicPlayerActivity — v3.0
 *
 * CHANGES FROM v2.0
 * ─────────────────
 *  1. Next button  → advances to the NEXT song in the list (wraps around).
 *  2. Prev button  → goes back to the PREVIOUS song in the list (wraps around).
 *  3. Replay button → seeks to the beginning and resumes playback of the SAME song.
 *  4. On song completion → automatically navigates to RMIntroActivity (unchanged).
 *  5. Song list is passed in via intent extra "SONG_LIST" (String array).
 *     Falls back to the default three-song list if not provided.
 */
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

    // ── Song list navigation ───────────────────────────────────────────────
    private val defaultSongList = listOf(
        "Row Row Row Your Boat",
        "Twinkle Twinkle Little Star",
        "Jack and Jill"
    )
    private lateinit var songList: List<String>
    private var currentSongIndex = 0

    private var currentSongTitle    = ""
    private var currentSongResource = 0
    private val likedSongs          = mutableSetOf<String>()

    private var slowMode    = false
    private var replayCount = 0

    private lateinit var currentLyrics: List<Lyric>
    private lateinit var currentKeywords: List<Keyword>
    private var currentKeyword: Keyword? = null

    data class Lyric   (val text: String, val startTime: Int, val endTime: Int)
    data class Keyword (val word: String, val imageRes: Int,  val startTime: Int, val endTime: Int)

    // ════════════════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)

        // Build song list (caller may override)
        songList = intent.getStringArrayExtra("SONG_LIST")?.toList() ?: defaultSongList

        currentSongTitle    = intent.getStringExtra("SONG_TITLE") ?: defaultSongList[0]
        currentSongResource = intent.getIntExtra("SONG_RESOURCE", R.raw.row_row_row_your_boat)
        intent.getStringArrayExtra("LIKED_SONGS")?.let { likedSongs.addAll(it) }

        // Resolve current index from title
        currentSongIndex = songList.indexOf(currentSongTitle).coerceAtLeast(0)

        initializeViews()
        setupSongData()
        setupMediaPlayer()
        setupClickListeners()
        setupSeekBar()
    }

    // ── Views ──────────────────────────────────────────────────────────────

    private fun initializeViews() {
        playPauseButton   = findViewById(R.id.playPauseButton)
        progressBar       = findViewById(R.id.progressBar)
        currentTimeText   = findViewById(R.id.currentTime)
        totalTimeText     = findViewById(R.id.totalTime)
        lyricsTextView    = findViewById(R.id.lyricsTextView)
        flashImage        = findViewById(R.id.flashImage)
        likeButton        = findViewById(R.id.likeButton)
        songTitleTextView = findViewById(R.id.songTitle)

        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            onBackPressed()
        }

        updateSongTitleUI()
        flashImage.visibility = View.INVISIBLE
    }

    private fun updateSongTitleUI() {
        songTitleTextView.text = currentSongTitle.uppercase()
        val isLiked = likedSongs.contains(currentSongTitle)
        likeButton.isSelected = isLiked
        likeButton.setImageResource(if (isLiked) R.drawable.heart_filled else R.drawable.heart)
    }

    // ── Click listeners ────────────────────────────────────────────────────

    private fun setupClickListeners() {
        playPauseButton.setOnClickListener { togglePlayPause() }

        // ── PREV: go to previous song in the list ──────────────────────────
        findViewById<ImageButton>(R.id.prevButton).setOnClickListener {
            currentSongIndex = (currentSongIndex - 1 + songList.size) % songList.size
            loadSong(songList[currentSongIndex])
        }

        // ── NEXT: go to next song in the list ─────────────────────────────
        findViewById<ImageButton>(R.id.nextButton).setOnClickListener {
            currentSongIndex = (currentSongIndex + 1) % songList.size
            loadSong(songList[currentSongIndex])
        }

        // ── REPLAY: seek to beginning and play the same song ──────────────
        findViewById<ImageButton>(R.id.repeatButton).setOnClickListener {
            mediaPlayer.seekTo(0)
            progressBar.progress = 0
            currentTimeText.text = formatTime(0)
            updateLyricsAndKeywords(0)
            if (!isPlaying) playMusic()
        }

        // ── ♥ Favourite button ─────────────────────────────────────────────
        likeButton.setOnClickListener {
            it.isSelected = !it.isSelected
            likeButton.setImageResource(
                if (it.isSelected) R.drawable.heart_filled else R.drawable.heart
            )
            if (it.isSelected) likedSongs.add(currentSongTitle)
            else               likedSongs.remove(currentSongTitle)

            likeButton.animate().scaleX(1.35f).scaleY(1.35f).setDuration(130)
                .withEndAction {
                    likeButton.animate().scaleX(1f).scaleY(1f).setDuration(130).start()
                }.start()
        }

        // Background tap → back
        findViewById<View>(R.id.rootLayout).setOnClickListener { onBackPressed() }
    }

    // ── Load a new song without leaving the activity ───────────────────────

    /**
     * Switches to a different song: tears down the current MediaPlayer,
     * rebuilds song data, and starts playback automatically.
     */
    private fun loadSong(title: String) {
        stopTimer()
        if (::mediaPlayer.isInitialized) {
            try { mediaPlayer.stop(); mediaPlayer.release() } catch (_: Exception) {}
        }
        currentSongTitle    = title
        currentSongResource = resourceForSong(title)
        replayCount         = 0
        currentKeyword      = null
        flashImage.visibility = View.INVISIBLE
        lyricsTextView.text   = ""

        updateSongTitleUI()
        setupSongData()
        setupMediaPlayer()

        // Auto-play on song change
        playMusic()
    }

    private fun buildHighlightedLyric(lyricText: String): android.text.SpannableString {
        val spannable = android.text.SpannableString(lyricText)

        // Collect all keyword words for the current song
        val keywordWords = currentKeywords.map { it.word.lowercase() }.distinct()

        for (word in keywordWords) {
            var searchStart = 0
            val lowerLyric = lyricText.lowercase()
            while (searchStart < lowerLyric.length) {
                val idx = lowerLyric.indexOf(word, searchStart)
                if (idx == -1) break

                // Make sure we match whole words, not substrings
                val before = if (idx > 0) lowerLyric[idx - 1] else ' '
                val after  = if (idx + word.length < lowerLyric.length) lowerLyric[idx + word.length] else ' '
                if (!before.isLetter() && !after.isLetter()) {
                    spannable.setSpan(
                        android.text.style.ForegroundColorSpan(
                            android.graphics.Color.parseColor("#D32F2F") // dark orange
                        ),
                        idx, idx + word.length,
                        android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannable.setSpan(
                        android.text.style.RelativeSizeSpan(1.25f), // 25% bigger than surrounding text
                        idx, idx + word.length,
                        android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannable.setSpan(
                        android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        idx, idx + word.length,
                        android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                searchStart = idx + word.length
            }
        }
        return spannable
    }

    private fun resourceForSong(title: String): Int = when (title) {
        "Twinkle Twinkle Little Star" -> R.raw.twinkle_twinkle
        "Jack and Jill"               -> R.raw.jack_and_jill
        else                          -> R.raw.row_row_row_your_boat
    }

    // ── SeekBar ────────────────────────────────────────────────────────────

    private fun setupSeekBar() {
        progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) { mediaPlayer.seekTo(p); updateLyricsAndKeywords(p) }
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
    }

    // ── Playback speed ─────────────────────────────────────────────────────

    private fun applyPlaybackSpeed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer.playbackParams =
                    PlaybackParams().setSpeed(if (slowMode) 0.75f else 1.0f)
            } catch (_: Exception) {}
        }
    }

    // ── MediaPlayer ────────────────────────────────────────────────────────

    private fun setupMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, currentSongResource)
        mediaPlayer.setOnCompletionListener {
            isPlaying = false
            playPauseButton.setImageResource(R.drawable.sound)
            stopTimer()
            flashImage.visibility = View.INVISIBLE
            // Auto-navigate to game intro after song ends
            Handler(Looper.getMainLooper()).postDelayed({ navigateToRMIntroActivity() }, 1000)
        }
        totalTimeText.text  = formatTime(mediaPlayer.duration)
        progressBar.max     = mediaPlayer.duration
        progressBar.progress = 0
        currentTimeText.text = formatTime(0)
    }

    private fun togglePlayPause() { if (isPlaying) pauseMusic() else playMusic() }

    private fun playMusic() {
        mediaPlayer.start()
        applyPlaybackSpeed()
        isPlaying = true
        playPauseButton.setImageResource(R.drawable.pause)
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
                        val pos = mediaPlayer.currentPosition
                        progressBar.progress = pos
                        currentTimeText.text = formatTime(pos)
                        updateLyricsAndKeywords(pos)
                    }
                }
            }
        }, 0, 100)
    }

    private fun stopTimer() { timer?.cancel(); timer = null }

    // ── Navigation ─────────────────────────────────────────────────────────

    private fun navigateToRMIntroActivity() {
        val difficultyHint = when {
            replayCount == 0 -> 0
            replayCount <= 2 -> 1
            else             -> 2
        }
        startActivity(
            Intent(this, RMIntroActivity::class.java)
                .putExtra("SONG_TITLE",      currentSongTitle)
                .putExtra("LIKED_SONGS",     likedSongs.toTypedArray())
                .putExtra("SLOW_MODE",       slowMode)
                .putExtra("DIFFICULTY_HINT", difficultyHint)
        )
    }

    // ── Lyrics & flash-card sync ───────────────────────────────────────────

    private fun updateLyricsAndKeywords(pos: Int) {
        val lyricLine = currentLyrics.find { pos in it.startTime..it.endTime }?.text
        if (lyricLine != null) {
            lyricsTextView.text = buildHighlightedLyric(lyricLine)
        } else {
            lyricsTextView.text = ""
        }

        currentKeywords.forEach { kw ->
            if (pos in kw.startTime..kw.endTime) {
                if (currentKeyword != kw) {
                    currentKeyword = kw
                    flashImage.setImageResource(kw.imageRes)
                    flashImage.visibility = View.VISIBLE
                    flashImage.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150)
                        .withEndAction {
                            flashImage.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                        }.start()
                }
            } else if (pos > kw.endTime && currentKeyword == kw) {
                currentKeyword = null
                flashImage.visibility = View.INVISIBLE
            }
        }
    }

    private fun formatTime(ms: Int): String =
        "%02d:%02d".format((ms / 60000) % 60, (ms / 1000) % 60)

    // ── Song data ──────────────────────────────────────────────────────────

    private fun setupSongData() {
        when (currentSongTitle) {
            "Row Row Row Your Boat" -> {
                currentLyrics = listOf(
                    Lyric("Row, row, row your boat",              9000,  12000),
                    Lyric("Gently down the stream",               12000, 14000),
                    Lyric("Merrily, merrily, merrily, merrily",  14000, 17000),
                    Lyric("Life is but a dream",                  17000, 20000),
                    Lyric("Row, row, row your boat",              21000, 22000),
                    Lyric("Gently down the creek",                22000, 25000),
                    Lyric("And if you see a little mouse,",       25000, 28000),
                    Lyric("Don't forget to squeak",               28000, 30000),
                    Lyric("Row, row, row your boat",              30000, 33000),
                    Lyric("Gently down the river",                33000, 35000),
                    Lyric("And if you see a polar bear,",         35000, 38000),
                    Lyric("Don't forget to shiver",               38000, 41000),
                    Lyric("Row, row, row your boat",              40000, 44000),
                    Lyric("Gently down the stream",               44000, 46000),
                    Lyric("And if you see a crocodile",           46000, 49000),
                    Lyric("Don't forget to scream",               49000, 51000),
                )
                currentKeywords = listOf(
                    Keyword("boat",       R.drawable.rhy_song0_boat,       11000, 12000),
                    Keyword("stream",     R.drawable.rhy_song0_stream,     13000, 14000),
                    Keyword("dream",      R.drawable.rhy_song0_dream,      18000, 20000),
                    Keyword("boat",       R.drawable.rhy_song0_boat,       21000, 23000),
                    Keyword("creek",      R.drawable.rhy_song0_creek,      24000, 25000),
                    Keyword("mouse",      R.drawable.rhy_song0_mouse,      27000, 28000),
                    Keyword("squeak",     R.drawable.squeak,               29000, 30000),
                    Keyword("boat",       R.drawable.rhy_song0_boat,       32000, 33000),
                    Keyword("river",      R.drawable.rhy_song0_river,      34000, 36000),
                    Keyword("polar bear", R.drawable.rhy_song0_polar_bear, 37000, 38000),
                    Keyword("shiver",     R.drawable.shiver,               39000, 41000),
                    Keyword("boat",       R.drawable.rhy_song0_boat,       43000, 44000),
                    Keyword("stream",     R.drawable.rhy_song0_stream,     45000, 46000),
                    Keyword("crocodile",  R.drawable.rhy_song0_crocodile,  48000, 49000),
                    Keyword("scream",     R.drawable.scream_image,         50000, 51000),
                )
            }
            "Twinkle Twinkle Little Star" -> {
                currentLyrics = listOf(
                    Lyric("Twinkle, twinkle, little star",   7000,  10000),
                    Lyric("How I wonder what you are",       10000, 15000),
                    Lyric("Up above the world so high",      15000, 19000),
                    Lyric("Like a diamond in the sky",       19000, 22000),
                    Lyric("Twinkle, twinkle, little star",   23000, 27000),
                    Lyric("How I wonder what you are",       27000, 31000),
                    Lyric("When the blazing sun is gone,",   39000, 43000),
                    Lyric("Then you show your little light,",46000, 51000),
                    Lyric("Twinkle, twinkle, little star",   55000, 58000),
                    Lyric("How I wonder what you are",       58000, 63000),
                )
                currentKeywords = listOf(
                    Keyword("star",    R.drawable.rhy_song1_star,    8000,  10000),
                    Keyword("world",   R.drawable.rhy_song1_world,   16000, 18000),
                    Keyword("diamond", R.drawable.rhy_song1_diamond, 19000, 20000),
                    Keyword("star",    R.drawable.rhy_song1_star,    25000, 27000),
                    Keyword("sun",     R.drawable.rhy_song1_sun,     40000, 42000),
                    Keyword("light",   R.drawable.rhy_song1_light,   48000, 50000),
                    Keyword("night",   R.drawable.rhy_song1_moon,    53000, 55000),
                )
            }
            "Jack and Jill" -> {
                currentLyrics = listOf(
                    Lyric("Jack and Jill went up the hill",    10000, 11000),
                    Lyric("To fetch a pail of water",           12000, 14000),
                    Lyric("Jack fell down and broke his crown", 14000, 16000),
                    Lyric("And Jill came tumbling after",       16000, 20000)
                )
                currentKeywords = listOf(
                    Keyword("hill",  R.drawable.hill_image,  10500, 11000),
                    Keyword("water", R.drawable.water_image, 12500, 14000),
                    Keyword("crown", R.drawable.crown_image, 14500, 16000)
                )
            }
            else -> { currentLyrics = emptyList(); currentKeywords = emptyList() }
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onBackPressed() {
        setResult(RESULT_OK, Intent().putExtra("UPDATED_LIKED_SONGS", likedSongs.toTypedArray()))
        releaseMediaPlayer()
        super.onBackPressed()
    }

    override fun onDestroy() { super.onDestroy(); releaseMediaPlayer() }

    private fun releaseMediaPlayer() {
        stopTimer()
        if (::mediaPlayer.isInitialized) {
            try { mediaPlayer.release() } catch (_: Exception) {}
        }
    }
}