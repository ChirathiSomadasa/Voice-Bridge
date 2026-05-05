package com.chirathi.voicebridge

import android.content.Intent
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Timer
import java.util.TimerTask

/**
 * MusicPlayerActivity — v4.0
 *
 * CHANGES FROM v3.0
 * ─────────────────
 *  1. Reads EXTRA_SHOWN_KEYWORDS and EXTRA_PROGRESS_TIER from SongSelectionActivity.
 *  2. Filters currentKeywords to only the tier-appropriate words from RhythmFlashcardManager.
 *     LOW  → only 5 easy words flash during song (e.g. boat, mouse, river)
 *     MED  → 8-10 mixed words flash
 *     HIGH → all words flash
 *  3. Passes the same shown-keyword list to RMIntroActivity so RhythmSummaryActivity
 *     only tests the child on words they actually saw.
 *  4. All existing navigation, prev/next/replay, seek bar, lyrics unchanged.
 */
class MusicPlayerActivity : AppCompatActivity() {

    private val TAG = "MusicPlayerActivity"

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

    // ── Lyrics (per song, fixed — lyrics don't change with tier) ──────────
    private lateinit var currentLyrics: List<Lyric>

    // ── Flashcard keywords (tier-filtered — changes per child) ────────────
    // This is the ACTIVE set shown during playback.
    // It is a SUBSET of the full song keyword list, chosen by RhythmFlashcardManager.
    private var currentKeywords: List<Keyword> = emptyList()
    private var currentKeyword: Keyword? = null

    // ── Personalisation state ─────────────────────────────────────────────
    private var progressTier = RhythmFlashcardManager.ProgressTier.LOW
    // Unique words shown during this playback (used for invariant passing to summary)
    private var shownKeywordWords = arrayListOf<String>()

    data class Lyric   (val text: String, val startTime: Int, val endTime: Int)
    data class Keyword (val word: String, val imageRes: Int, val startTime: Int, val endTime: Int)

    // =========================================================================
    // onCreate
    // =========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)

        // Safety: restore ChildSession if process was killed
        if (!ChildSession.isInitialized) ChildSession.restore(this)

        songList = intent.getStringArrayExtra("SONG_LIST")?.toList() ?: defaultSongList

        currentSongTitle    = intent.getStringExtra("SONG_TITLE") ?: defaultSongList[0]
        currentSongResource = intent.getIntExtra("SONG_RESOURCE", R.raw.row_row_row_your_boat)
        intent.getStringArrayExtra("LIKED_SONGS")?.let { likedSongs.addAll(it) }

        currentSongIndex = songList.indexOf(currentSongTitle).coerceAtLeast(0)

        // ── Read tier and shown-keyword list from SongSelectionActivity ────
        val tierName = intent.getStringExtra(RhythmFlashcardManager.EXTRA_PROGRESS_TIER) ?: "LOW"
        progressTier = runCatching {
            RhythmFlashcardManager.ProgressTier.valueOf(tierName)
        }.getOrDefault(RhythmFlashcardManager.ProgressTier.LOW)

        val receivedWords = intent.getStringArrayListExtra(
            RhythmFlashcardManager.EXTRA_SHOWN_KEYWORDS) ?: arrayListOf()

        // If SongSelection passed the words, use them; otherwise compute here as fallback
        shownKeywordWords = if (receivedWords.isNotEmpty()) {
            receivedWords
        } else {
            Log.w(TAG, "No shown keywords in intent — computing from profile")
            val profile = ChildProfileManager.load(this, ChildSession.childId)
            val fallbackTier = RhythmFlashcardManager.determineTierFromProfile(profile)
            progressTier = fallbackTier
            RhythmFlashcardManager.toWordList(
                RhythmFlashcardManager.selectSongFlashcards(currentSongTitle, fallbackTier))
        }

        Log.d(TAG, "══════════════════════════════════════════")
        Log.d(TAG, "MusicPlayerActivity: song='$currentSongTitle' tier=$progressTier")
        Log.d(TAG, "  childId=${ChildSession.childId}  age=${ChildSession.age}")
        Log.d(TAG, "  Shown keyword words (${shownKeywordWords.size}): $shownKeywordWords")
        Log.d(TAG, "══════════════════════════════════════════")

        initializeViews()
        setupSongData()           // sets full currentLyrics + applies tier filter to keywords
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

        findViewById<ImageView>(R.id.backBtn).setOnClickListener { onBackPressed() }

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

        findViewById<ImageButton>(R.id.prevButton).setOnClickListener {
            currentSongIndex = (currentSongIndex - 1 + songList.size) % songList.size
            loadSong(songList[currentSongIndex])
        }

        findViewById<ImageButton>(R.id.nextButton).setOnClickListener {
            currentSongIndex = (currentSongIndex + 1) % songList.size
            loadSong(songList[currentSongIndex])
        }

        findViewById<ImageButton>(R.id.repeatButton).setOnClickListener {
            mediaPlayer.seekTo(0)
            progressBar.progress = 0
            currentTimeText.text = formatTime(0)
            updateLyricsAndKeywords(0)
            if (!isPlaying) playMusic()
        }

        likeButton.setOnClickListener {
            it.isSelected = !it.isSelected
            likeButton.setImageResource(
                if (it.isSelected) R.drawable.heart_filled else R.drawable.heart)
            if (it.isSelected) likedSongs.add(currentSongTitle)
            else               likedSongs.remove(currentSongTitle)
            likeButton.animate().scaleX(1.35f).scaleY(1.35f).setDuration(130)
                .withEndAction {
                    likeButton.animate().scaleX(1f).scaleY(1f).setDuration(130).start()
                }.start()
        }
    }

    // ── Load song (prev/next navigation) ──────────────────────────────────

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

        // Recompute shown keywords for the new song using same tier
        val newFlashcards = RhythmFlashcardManager.selectSongFlashcards(title, progressTier)
        shownKeywordWords = RhythmFlashcardManager.toWordList(newFlashcards)

        Log.d(TAG, "loadSong: '$title'  tier=$progressTier  shown=${shownKeywordWords.size}")

        updateSongTitleUI()
        setupSongData()
        setupMediaPlayer()
        playMusic()
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
            Handler(Looper.getMainLooper()).postDelayed({ navigateToRMIntroActivity() }, 1000)
        }
        totalTimeText.text   = formatTime(mediaPlayer.duration)
        progressBar.max      = mediaPlayer.duration
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

    /**
     * Passes shownKeywordWords and tier to RMIntroActivity.
     * RMIntroActivity MUST forward these to RhythmSummaryActivity.
     * Add to RMIntroActivity's intent to RhythmSummaryActivity:
     *   putStringArrayListExtra(RhythmFlashcardManager.EXTRA_SHOWN_KEYWORDS, shownWords)
     *   putExtra(RhythmFlashcardManager.EXTRA_PROGRESS_TIER, tier)
     */
    private fun navigateToRMIntroActivity() {
        val difficultyHint = when {
            replayCount == 0 -> 0
            replayCount <= 2 -> 1
            else             -> 2
        }

        Log.d(TAG, "══════════════════════════════════════════")
        Log.d(TAG, "navigateToRMIntroActivity:")
        Log.d(TAG, "  song=$currentSongTitle  tier=$progressTier")
        Log.d(TAG, "  shownWords(${shownKeywordWords.size}): $shownKeywordWords")
        Log.d(TAG, "  → These MUST reach RhythmSummaryActivity via RMIntroActivity")
        Log.d(TAG, "══════════════════════════════════════════")

        startActivity(
            Intent(this, RMIntroActivity::class.java)
                .putExtra("SONG_TITLE",       currentSongTitle)
                .putExtra("LIKED_SONGS",      likedSongs.toTypedArray())
                .putExtra("SLOW_MODE",        slowMode)
                .putExtra("DIFFICULTY_HINT",  difficultyHint)
                // ── Flashcard personalisation extras — forward to RhythmSummaryActivity ──
                .putStringArrayListExtra(
                    RhythmFlashcardManager.EXTRA_SHOWN_KEYWORDS, shownKeywordWords)
                .putExtra(
                    RhythmFlashcardManager.EXTRA_PROGRESS_TIER, progressTier.name)
        )
    }

    // ── Lyrics & flashcard sync ────────────────────────────────────────────

    /**
     * Called every 100ms during playback.
     * Shows flashcard image ONLY if the current position matches a keyword
     * in currentKeywords (the tier-filtered list, not the full song list).
     */
    private fun updateLyricsAndKeywords(pos: Int) {
        // Lyrics display (unchanged — all lyrics always shown)
        val lyricLine = currentLyrics.find { pos in it.startTime..it.endTime }?.text
        lyricsTextView.text = if (lyricLine != null) buildHighlightedLyric(lyricLine) else ""

        // Flashcard display — only tier-appropriate keywords trigger
        var foundActive = false
        for (kw in currentKeywords) {
            if (pos in kw.startTime..kw.endTime) {
                foundActive = true
                if (currentKeyword != kw) {
                    currentKeyword = kw
                    flashImage.setImageResource(kw.imageRes)
                    flashImage.visibility = View.VISIBLE
                    flashImage.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150)
                        .withEndAction {
                            flashImage.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                        }.start()
                    Log.d(TAG, "Flashcard shown: '${kw.word}' @${pos}ms (tier=$progressTier)")
                }
                break
            }
        }
        if (!foundActive && currentKeyword != null) {
            currentKeyword = null
            flashImage.visibility = View.INVISIBLE
        }
    }

    private fun buildHighlightedLyric(lyricText: String): android.text.SpannableString {
        val spannable    = android.text.SpannableString(lyricText)
        val keywordWords = currentKeywords.map { it.word.lowercase() }.distinct()

        for (word in keywordWords) {
            var searchStart = 0
            val lowerLyric  = lyricText.lowercase()
            while (searchStart < lowerLyric.length) {
                val idx = lowerLyric.indexOf(word, searchStart)
                if (idx == -1) break
                val before = if (idx > 0) lowerLyric[idx - 1] else ' '
                val after  = if (idx + word.length < lowerLyric.length)
                    lowerLyric[idx + word.length] else ' '
                if (!before.isLetter() && !after.isLetter()) {
                    spannable.setSpan(
                        android.text.style.ForegroundColorSpan(
                            android.graphics.Color.parseColor("#D32F2F")),
                        idx, idx + word.length,
                        android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannable.setSpan(
                        android.text.style.RelativeSizeSpan(1.25f),
                        idx, idx + word.length,
                        android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannable.setSpan(
                        android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        idx, idx + word.length,
                        android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                searchStart = idx + word.length
            }
        }
        return spannable
    }

    private fun formatTime(ms: Int): String =
        "%02d:%02d".format((ms / 60000) % 60, (ms / 1000) % 60)

    // ── Song data ──────────────────────────────────────────────────────────
    //
    // setupSongData() does two things:
    //   1. Sets currentLyrics (full, unchanged — all lyrics always display)
    //   2. Sets currentKeywords — FILTERED to tier-appropriate words only
    //
    // The full keyword list per song is defined inline here and then filtered
    // against shownKeywordWords (passed from SongSelectionActivity via intent).
    // A keyword only flashes if its word is in the allowed set.
    //
    // IMPORTANT: "boat" appears 4 times in Row Row Row — if "boat" is in the
    // shown set, ALL 4 occurrences flash. If it's not in the set, none do.

    private fun setupSongData() {
        when (currentSongTitle) {
            "Row Row Row Your Boat" -> {
                currentLyrics = listOf(
                    Lyric("Row, row, row your boat",             9000,  12000),
                    Lyric("Gently down the stream",              12000, 14000),
                    Lyric("Merrily, merrily, merrily, merrily", 14000, 17000),
                    Lyric("Life is but a dream",                 17000, 20000),
                    Lyric("Row, row, row your boat",             20000, 22000),
                    Lyric("Gently down the creek",               22000, 25000),
                    Lyric("And if you see a little mouse,",      25000, 28000),
                    Lyric("Don't forget to squeak",              28000, 30000),
                    Lyric("Row, row, row your boat",             30000, 33000),
                    Lyric("Gently down the river",               33000, 35000),
                    Lyric("And if you see a polar bear,",        35000, 38000),
                    Lyric("Don't forget to shiver",              38000, 41000),
                    Lyric("Row, row, row your boat",             40000, 44000),
                    Lyric("Gently down the stream",              44000, 46000),
                    Lyric("And if you see a crocodile",          46000, 49000),
                    Lyric("Don't forget to scream",              49000, 51000)
                )
                // FULL list — all flashcards that exist in the song
                val fullKeywords = listOf(
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
                    Keyword("scream",     R.drawable.scream_image,         50000, 51000)
                )
                // Apply tier filter: only show keywords whose word is in shownKeywordWords
                currentKeywords = applyTierFilter(fullKeywords)
            }

            "Twinkle Twinkle Little Star" -> {
                currentLyrics = listOf(
                    Lyric("Twinkle, twinkle, little star",    7000,  10000),
                    Lyric("How I wonder what you are",        10000, 15000),
                    Lyric("Up above the world so high",       15000, 19000),
                    Lyric("Like a diamond in the sky",        19000, 22000),
                    Lyric("Twinkle, twinkle, little star",    23000, 27000),
                    Lyric("How I wonder what you are",        27000, 31000),
                    Lyric("When the blazing sun is gone,",    39000, 43000),
                    Lyric("Then you show your little light,", 46000, 51000),
                    Lyric("Twinkle, twinkle, little star",    55000, 58000),
                    Lyric("How I wonder what you are",        58000, 63000)
                )
                val fullKeywords = listOf(
                    Keyword("star",    R.drawable.rhy_song1_star,    8000,  10000),
                    Keyword("world",   R.drawable.rhy_song1_world,   16000, 18000),
                    Keyword("diamond", R.drawable.rhy_song1_diamond, 19000, 20000),
                    Keyword("star",    R.drawable.rhy_song1_star,    25000, 27000),
                    Keyword("sun",     R.drawable.rhy_song1_sun,     40000, 42000),
                    Keyword("light",   R.drawable.rhy_song1_light,   48000, 50000),
                    Keyword("night",   R.drawable.rhy_song1_moon,    53000, 55000)
                )
                currentKeywords = applyTierFilter(fullKeywords)
            }

            "Jack and Jill" -> {
                currentLyrics = listOf(
                    Lyric("Jack and Jill went up the hill",    10000, 11000),
                    Lyric("To fetch a pail of water",           12000, 14000),
                    Lyric("Jack fell down and broke his crown", 14000, 16000),
                    Lyric("And Jill came tumbling after",       16000, 20000)
                )
                // Jack and Jill only has 3 keywords — all tiers show all 3
                val fullKeywords = listOf(
                    Keyword("hill",  R.drawable.hill_image,  10500, 11000),
                    Keyword("water", R.drawable.water_image, 12500, 14000),
                    Keyword("crown", R.drawable.crown_image, 14500, 16000)
                )
                currentKeywords = applyTierFilter(fullKeywords)
            }

            else -> {
                currentLyrics   = emptyList()
                currentKeywords = emptyList()
            }
        }

        Log.d(TAG, "setupSongData: '$currentSongTitle'  tier=$progressTier")
        Log.d(TAG, "  Active flashcards (${currentKeywords.size}): " +
                "${currentKeywords.map { it.word }.distinct()}")
    }

    /**
     * Filters a full keyword list to only those words in shownKeywordWords.
     * Preserves ALL time-based occurrences of each allowed word
     * (e.g. "boat" appears 4 times — all 4 entries are kept if boat is allowed).
     */
    private fun applyTierFilter(full: List<Keyword>): List<Keyword> {
        if (shownKeywordWords.isEmpty()) {
            Log.w(TAG, "applyTierFilter: shownKeywordWords empty — showing all keywords")
            return full
        }
        val allowed = shownKeywordWords.map { it.lowercase() }.toSet()
        val filtered = full.filter { it.word.lowercase() in allowed }
        Log.d(TAG, "applyTierFilter: ${full.size} total → ${filtered.size} after tier filter")
        Log.d(TAG, "  Allowed words: $allowed")
        Log.d(TAG, "  Active entries: ${filtered.map { it.word }}")
        return filtered
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