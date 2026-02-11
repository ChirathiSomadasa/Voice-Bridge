package com.chirathi.voicebridge

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.animation.TranslateAnimation
import android.widget.*
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import kotlin.math.abs

class RhythmSummaryActivity : AppCompatActivity() {

    private lateinit var currentSongTitle: String
    private val TAG = "RhythmSummaryActivity"

    // UI Components
    private lateinit var pandaImage: ImageView
    private lateinit var wordTitle: TextView
    private lateinit var scoreText: TextView
    private lateinit var progressContainer: LinearLayout
    private lateinit var optionsGrid: GridLayout
    private lateinit var feedbackIcon: ImageView
    private lateinit var feedbackText: TextView
    private lateinit var feedbackOverlay: FrameLayout
    private lateinit var nextButton: Button
    private lateinit var confidenceBuilderOverlay: FrameLayout
    private lateinit var simplifiedOptionsContainer: LinearLayout
    private lateinit var therapeuticMessage: TextView

    // Therapeutic components
    private lateinit var gameMaster: GameMasterModel
    private var currentRound = 0
    private var score = 0
    private var totalRounds = 5 // Will be adjusted dynamically
    private var correctAnswerIndex = 0
    private var isAnswerSelected = false
    private var isConfidenceBuilderMode = false
    private var individualLatencyBuffer: Long = 3000 // Default 3 seconds
    private var therapeuticIntent: TherapeuticIntent? = null
    private var confidenceBuilderSuccesses = 0
    private var consecutiveWrongAnswers = 0

    // Behavioral tracking
    private var responseStartTime: Long = 0
    private val responseTimes = mutableListOf<Long>()
    private val touchPatterns = mutableListOf<Pair<Float, Float>>()

    // Track used keywords to avoid repetition
    private val usedKeywords = mutableSetOf<String>()

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var correctSound: MediaPlayer
    private lateinit var wrongSound: MediaPlayer
    private lateinit var instructionsText: TextView
    private lateinit var rootLayout: View
    private lateinit var feedbackContainer: LinearLayout
    private var isTimerActive = false
    private var timeoutHandler: Handler? = null

    // Define Keyword data class
    data class Keyword(val word: String, val imageRes: Int, val startTime: Int, val endTime: Int)

    private val keywordImages = mapOf(
        "boat" to R.drawable.rhy_song0_boat,
        "stream" to R.drawable.rhy_song0_stream,
        "dream" to R.drawable.rhy_song0_dream,
        "creek" to R.drawable.rhy_song0_creek,
        "mouse" to R.drawable.rhy_song0_mouse,
        "river" to R.drawable.rhy_song0_river,
        "polar bear" to R.drawable.rhy_song0_polar_bear,
        "crocodile" to R.drawable.rhy_song0_crocodile,
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
        "hill" to R.drawable.hill_image,
        "water" to R.drawable.water_image,
        "crown" to R.drawable.crown_image
    )

    data class GameRound(val keyword: String, val correctImageRes: Int, val options: List<Pair<String, Int>>)

    private lateinit var currentKeywords: List<Keyword>
    private lateinit var availableKeywords: MutableList<Keyword>

    // Water-related keywords that shouldn't appear together
    private val waterTerms = listOf("stream", "river", "creek", "lake", "ocean", "water", "boat")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting RhythmSummaryActivity")

        try {
            setContentView(R.layout.activity_rythm_summary)
            Log.d(TAG, "Layout set successfully")

            // Initialize Game Master
            gameMaster = GameMasterModel(this)

            // Initialize UI components
            initializeViews()
            Log.d(TAG, "Views initialized successfully")

            // Get song title from intent
            currentSongTitle = intent.getStringExtra("SONG_TITLE") ?: "Row Row Row Your Boat"
            Log.d(TAG, "Received song title: $currentSongTitle")

            // Get therapeutic intent if passed
            therapeuticIntent = intent.getSerializableExtra("THERAPEUTIC_INTENT") as? TherapeuticIntent

            try {
                mediaPlayer = MediaPlayer.create(this, R.raw.button_click)
                correctSound = MediaPlayer.create(this, R.raw.correct_sound)
                wrongSound = MediaPlayer.create(this, R.raw.wrong_sound)
                Log.d(TAG, "Sound effects initialized")
            } catch (e: Exception) {
                Log.w(TAG, "Sound files not found")
                mediaPlayer = MediaPlayer()
                correctSound = MediaPlayer()
                wrongSound = MediaPlayer()
            }

            // Setup based on song
            setupSongData()
            Log.d(TAG, "Song data setup complete")

            // Get individualized latency buffer
            individualLatencyBuffer = gameMaster.getIndividualizedLatencyBuffer()
            Log.d(TAG, "Individual latency buffer: ${individualLatencyBuffer}ms")

            setupUI()
            Log.d(TAG, "UI setup complete")

            // Start first round with delay to allow therapeutic assessment
            Handler(Looper.getMainLooper()).postDelayed({
                startNewRound()
                Log.d(TAG, "First round started")
            }, 1000)

            // Set next button click listener
            nextButton.setOnClickListener {
                Log.d(TAG, "Next button clicked")
                onNextButtonClick()
            }

            Log.d(TAG, "RhythmSummaryActivity initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "FATAL ERROR in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading game: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                gameMaster.trackTouch(event.x, event.y, System.currentTimeMillis())
            }
        }
        return super.onTouchEvent(event)
    }

    private fun initializeViews() {
        Log.d(TAG, "Initializing views...")
        try {
            pandaImage = findViewById(R.id.pandaImage)
            wordTitle = findViewById(R.id.wordTitle)
            scoreText = findViewById(R.id.scoreText)
            progressContainer = findViewById(R.id.progressContainer)
            instructionsText = findViewById(R.id.instructionsText)
            optionsGrid = findViewById(R.id.optionsGrid)
            feedbackOverlay = findViewById(R.id.feedbackOverlay)
            feedbackIcon = findViewById(R.id.feedbackIcon)
            feedbackText = findViewById(R.id.feedbackText)

            // CRITICAL FIX: Initialize feedbackContainer FIRST
            feedbackContainer = findViewById(R.id.feedbackContainer)

            nextButton = findViewById(R.id.nextButton)
            rootLayout = findViewById(R.id.rootLayout)

            // Therapeutic UI components
            confidenceBuilderOverlay = findViewById(R.id.confidenceBuilderOverlay)
            simplifiedOptionsContainer = findViewById(R.id.simplifiedOptionsContainer)
            therapeuticMessage = findViewById(R.id.therapeuticMessage)

            Log.d(TAG, "All views initialized successfully")
            Log.d(TAG, "feedbackContainer initialized: ${feedbackContainer != null}")

        } catch (e: Exception) {
            Log.e(TAG, "ERROR initializing views: ${e.message}", e)
            // If therapeutic components don't exist, create them programmatically
            createTherapeuticComponentsIfMissing()
        }
    }

    private fun createTherapeuticComponentsIfMissing() {
        // Create confidence builder overlay if it doesn't exist
        confidenceBuilderOverlay = FrameLayout(this).apply {
            id = R.id.confidenceBuilderOverlay
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#80FFFFFF"))
            visibility = View.GONE
        }

        simplifiedOptionsContainer = LinearLayout(this).apply {
            id = R.id.simplifiedOptionsContainer
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
        }

        therapeuticMessage = TextView(this).apply {
            id = R.id.therapeuticMessage
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            textSize = 18f
            setTextColor(Color.parseColor("#4CAF50"))
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
        }

        // Add to root layout
        val parent = findViewById<ViewGroup>(R.id.rootLayout)
        parent.addView(confidenceBuilderOverlay)
        confidenceBuilderOverlay.addView(therapeuticMessage)
        confidenceBuilderOverlay.addView(simplifiedOptionsContainer)
    }

    private fun setupSongData() {
        Log.d(TAG, "Setting up song data for: $currentSongTitle")
        currentKeywords = when (currentSongTitle) {
            "Row Row Row Your Boat" -> {
                listOf(
                    Keyword("boat", R.drawable.rhy_song0_boat, 11000, 12000),
                    Keyword("stream", R.drawable.rhy_song0_stream, 13000, 15000),
                    Keyword("dream", R.drawable.rhy_song0_dream, 18000, 20000),
                    Keyword("creek", R.drawable.rhy_song0_creek, 22000, 25000),
                    Keyword("mouse", R.drawable.rhy_song0_mouse, 25000, 27000),
                    Keyword("river", R.drawable.rhy_song0_river, 33000, 35000),
                    Keyword("polar bear", R.drawable.rhy_song0_polar_bear, 35000, 38000),
                    Keyword("crocodile", R.drawable.rhy_song0_crocodile, 46000, 49000)
                )
            }
            "Twinkle Twinkle Little Star" -> {
                listOf(
                    Keyword("star", R.drawable.rhy_song1_star, 8000, 10000),
                    Keyword("world", R.drawable.rhy_song1_world, 16000, 18000),
                    Keyword("diamond", R.drawable.rhy_song1_diamond, 19000, 20000),
                    Keyword("sun", R.drawable.rhy_song1_sun, 40000, 42000),
                    Keyword("light", R.drawable.rhy_song1_light, 48000, 50000),
                    Keyword("night", R.drawable.rhy_song1_moon, 53000, 55000),
                    Keyword("traveller", R.drawable.rhy_song1_traveller, 67000, 69000),
                    Keyword("dark blue sky", R.drawable.rhy_song1_dark_blue_sky, 100000, 102000),
                    Keyword("window", R.drawable.rhy_song1_window, 104000, 106000),
                    Keyword("eyes", R.drawable.rhy_song1_eyes, 109000, 111000)
                )
            }
            "Jack and Jill" -> {
                listOf(
                    Keyword("hill", R.drawable.hill_image, 10500, 11000),
                    Keyword("water", R.drawable.water_image, 12500, 14000),
                    Keyword("crown", R.drawable.crown_image, 14500, 16000)
                )
            }
            else -> {
                listOf(
                    Keyword("boat", R.drawable.rhy_song0_boat, 11000, 12000),
                    Keyword("stream", R.drawable.rhy_song0_stream, 13000, 15000),
                    Keyword("dream", R.drawable.rhy_song0_dream, 18000, 20000)
                )
            }
        }

        availableKeywords = currentKeywords.toMutableList()
        Log.d(TAG, "Available keywords: ${availableKeywords.size}")
    }

    private fun setupUI() {
        Log.d(TAG, "Setting up UI")
        try {
            // Adjust UI complexity based on therapeutic intent
            adjustUIComplexity()

            // Setup panda animation
            animatePanda()

            // Setup progress dots
            setupProgressDots()

            // Update score display
            updateScore()

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupUI: ${e.message}", e)
        }
    }

    private fun adjustUIComplexity() {
        val intent = therapeuticIntent ?: return

        // Adjust grid based on UI complexity
        val columnCount = when {
            intent.uiComplexity < 0.4f -> 1 // Single column for very simple UI
            intent.uiComplexity < 0.6f -> 2 // 2x2 grid for moderate complexity
            else -> 2 // Default 2x2 grid
        }

        optionsGrid.columnCount = columnCount
        optionsGrid.rowCount = if (columnCount == 1) 4 else 2

        // Adjust text size and contrast
        wordTitle.textSize = when {
            intent.uiComplexity < 0.4f -> 24f
            intent.uiComplexity < 0.6f -> 28f
            else -> 32f
        }

        // Set high contrast colors for better visibility
        if (intent.uiComplexity < 0.5f) {
            findViewById<View>(R.id.rootLayout).setBackgroundColor(Color.WHITE)
            wordTitle.setTextColor(Color.BLACK)
        }

        // Adjust total rounds based on session duration
        totalRounds = when (intent.sessionDuration) {
            in 0..5 -> 3 // Very short session
            in 6..10 -> 5 // Short session
            in 11..15 -> 7 // Medium session
            else -> 5 // Default
        }

        Log.d(TAG, "UI Complexity adjusted: ${intent.uiComplexity}, Rounds: $totalRounds")
    }

    private fun animatePanda() {
        try {
            val bounceAnimator = ObjectAnimator.ofFloat(pandaImage, "translationY", 0f, -20f, 0f)
            bounceAnimator.duration = 1000
            bounceAnimator.repeatCount = ObjectAnimator.INFINITE
            bounceAnimator.repeatMode = ObjectAnimator.REVERSE
            bounceAnimator.start()
            Log.d(TAG, "Panda animation started")
        } catch (e: Exception) {
            Log.e(TAG, "Error animating panda: ${e.message}", e)
        }
    }

    private fun setupProgressDots() {
        try {
            progressContainer.removeAllViews()

            for (i in 0 until totalRounds) {
                val dot = View(this)
                val size = 16.dpToPx()
                val params = LinearLayout.LayoutParams(size, size)
                params.marginEnd = if (i < totalRounds - 1) 8.dpToPx() else 0
                dot.layoutParams = params

                dot.setBackgroundColor(
                    when {
                        i == currentRound -> Color.parseColor("#4CAF50")
                        i < currentRound -> Color.parseColor("#FF6B35")
                        else -> Color.parseColor("#BDBDBD")
                    }
                )
                dot.background.setAlpha(200)
                progressContainer.addView(dot)
            }
            Log.d(TAG, "Added $totalRounds progress dots")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up progress dots: ${e.message}", e)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun startNewRound() {
        Log.d(TAG, "🎮 Starting new round ${currentRound + 1}/$totalRounds")

        if (currentRound >= totalRounds) {
            Log.d(TAG, "✅ Game finished! Score: $score/$totalRounds")
            navigateToScoreboard()
            return
        }

        // Reset state
        isAnswerSelected = false
        nextButton.visibility = View.GONE

        // Clear all visual hints
        optionsGrid.removeAllViews()
        simplifiedOptionsContainer.removeAllViews()
        simplifiedOptionsContainer.visibility = View.GONE
        confidenceBuilderOverlay.visibility = View.GONE
        optionsGrid.visibility = View.VISIBLE

        // Reset confidence builder
        if (isConfidenceBuilderMode && currentRound == 0) {
            isConfidenceBuilderMode = false
            therapeuticMessage.visibility = View.GONE
        }

        // Check confidence builder
        if (shouldActivateConfidenceBuilder()) {
            activateConfidenceBuilderMode()
            // Start timer for confidence builder mode
            startResponseTimer()
            return
        }

        // Get keyword
        val keyword = getUniqueKeyword()
        val correctImageRes = keywordImages[keyword.word] ?: android.R.drawable.ic_dialog_info

        // Generate options
        val wrongOptions = generateDiagnosticDistractors(keyword.word)
        val gameRound = createGameRound(keyword.word, correctImageRes, wrongOptions)

        // Display
        wordTitle.text = "Find: ${keyword.word.uppercase()}"
        displayOptions(gameRound)
        updateProgressDots()

        // Enable options
        optionsGrid.isEnabled = true

        // Start timer for normal mode
        startResponseTimer()
    }

    private fun startResponseTimer() {
        timeoutHandler?.removeCallbacksAndMessages(null)
        isTimerActive = true

        // FIX: Store the time when options become VISIBLE, not when timer "starts"
        Handler(Looper.getMainLooper()).postDelayed({
            responseStartTime = System.currentTimeMillis()
            Log.d(TAG, "⏰ Timer started at ${responseStartTime}")

            // Reduce minimum response time for confidence builder mode
            val timeoutDuration = if (isConfidenceBuilderMode) 8000 else 5000

            timeoutHandler = Handler(Looper.getMainLooper())
            timeoutHandler?.postDelayed({
                if (!isAnswerSelected && isTimerActive) {
                    Log.d(TAG, "⏰ Response timeout triggered after ${timeoutDuration}ms")
                    handleTimeout()
                }
            }, timeoutDuration.toLong())
        }, if (isConfidenceBuilderMode) 500 else 1500) // Shorter delay for confidence builder
    }


    private fun shouldActivateConfidenceBuilder(): Boolean {
        if (currentRound == 0) return false
        if (isConfidenceBuilderMode) return false

        // Track consecutive wrong answers in NORMAL mode only
        if (consecutiveWrongAnswers >= 2) {
            Log.d(TAG, "🎯 2 consecutive wrong answers - activating confidence builder WITHOUT counting as round")
            return true
        }

        return false
    }

    private fun activateConfidenceBuilderMode() {
        Log.d(TAG, "Activating Confidence Builder Mode")
        isConfidenceBuilderMode = true

        // Show therapeutic message
        therapeuticMessage.text = "Let's try an easier one! You can do it!"
        therapeuticMessage.visibility = View.VISIBLE

        // Make sure confidence builder overlay is visible
        confidenceBuilderOverlay.visibility = View.VISIBLE
        simplifiedOptionsContainer.visibility = View.VISIBLE
        optionsGrid.visibility = View.GONE

        // Clear any previous views
        simplifiedOptionsContainer.removeAllViews()

        // Get keyword
        val keyword = getUniqueKeyword()
        val correctImageRes = keywordImages[keyword.word] ?: android.R.drawable.ic_dialog_info

        // 🔴 FIX: Don't shuffle for confidence builder - keep correct answer at index 0
        val simpleOptions = listOf(
            Pair(keyword.word, correctImageRes),  // Index 0 = correct
            Pair("apple", android.R.drawable.ic_menu_help)  // Index 1 = wrong
        )

        // 🔴 FIX: Explicitly set correctAnswerIndex = 0
        correctAnswerIndex = 0

        val gameRound = GameRound(keyword.word, correctImageRes, simpleOptions)
        displaySimplifiedOptions(gameRound)

        // Update therapeutic message
        wordTitle.text = "Tap the ${keyword.word}:"

        // Give extra time
        individualLatencyBuffer = (individualLatencyBuffer * 1.5f).toLong()

        Log.d(TAG, "Confidence builder activated with keyword: ${keyword.word}")
        Log.d(TAG, "Correct answer index set to: $correctAnswerIndex")
    }

    private fun generateDiagnosticDistractors(keyword: String): List<Pair<String, Int>> {
        // Use Game Master's diagnostic distractor engine
        val diagnosticDistractors = gameMaster.getDiagnosticDistractors(keyword, 3)

        // Add some generic distractors as fallback
        val genericDistractors = listOf(
            Pair("apple", android.R.drawable.ic_menu_help),
            Pair("ball", android.R.drawable.ic_menu_help),
            Pair("car", android.R.drawable.ic_menu_help)
        )

        return if (diagnosticDistractors.size >= 3) {
            diagnosticDistractors
        } else {
            diagnosticDistractors + genericDistractors.take(3 - diagnosticDistractors.size)
        }
    }

    private fun getUniqueKeyword(): Keyword {
        // Reset if all keywords used or list empty
        if (availableKeywords.isEmpty() || usedKeywords.size >= currentKeywords.size * 0.7) {
            availableKeywords = currentKeywords.toMutableList()
            usedKeywords.clear()
            Log.d(TAG, "🔄 Reset available keywords pool")
        }

        // Filter out used keywords
        val available = availableKeywords.filter { it.word !in usedKeywords }

        val keyword = if (available.isNotEmpty()) {
            available.random()
        } else {
            // If somehow all are used, pick any and reset
            val randomKeyword = currentKeywords.random()
            availableKeywords = currentKeywords.toMutableList()
            usedKeywords.clear()
            usedKeywords.add(randomKeyword.word)
            randomKeyword
        }

        usedKeywords.add(keyword.word)
        availableKeywords.remove(keyword)

        Log.d(TAG, "📝 Selected keyword: ${keyword.word}, Remaining: ${availableKeywords.size}")
        return keyword
    }

    private fun createGameRound(keyword: String, correctImageRes: Int, wrongOptions: List<Pair<String, Int>>): GameRound {
        // Combine correct and wrong options
        val allOptions = mutableListOf(Pair(keyword, correctImageRes))
        allOptions.addAll(wrongOptions.take(3))

        // Shuffle
        val shuffledOptions = allOptions.shuffled()
        correctAnswerIndex = shuffledOptions.indexOfFirst { it.first == keyword }

        return GameRound(keyword, correctImageRes, shuffledOptions)
    }

    private fun displayOptions(gameRound: GameRound) {
        val columnCount = optionsGrid.columnCount
        val rowCount = optionsGrid.rowCount
        val optionSize = resources.displayMetrics.widthPixels / columnCount - 48.dpToPx()

        for (i in gameRound.options.indices) {
            val option = gameRound.options[i]
            createOptionCard(i, option.first, option.second, optionSize, gameRound.keyword)
        }
    }

    private fun displaySimplifiedOptions(gameRound: GameRound) {
        simplifiedOptionsContainer.visibility = View.VISIBLE
        optionsGrid.visibility = View.GONE

        val optionSize = resources.displayMetrics.widthPixels / 2 - 48.dpToPx()

        // 🔴 FIX: Don't shuffle - display in order (index 0 = correct, index 1 = wrong)
        for (i in gameRound.options.indices) {
            val option = gameRound.options[i]
            createSimplifiedOptionCard(i, option.first, option.second, optionSize, gameRound.keyword)
        }

        // 🔴 Log to verify
        Log.d(TAG, "Confidence builder - Correct answer at index 0: ${gameRound.options[0].first}")
    }

    private fun createOptionCard(index: Int, word: String, imageResId: Int, size: Int, correctKeyword: String) {
        val card = CardView(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = size
                height = size
                columnSpec = GridLayout.spec(index % optionsGrid.columnCount, 1f)
                rowSpec = GridLayout.spec(index / optionsGrid.columnCount, 1f)
                setMargins(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
            }
            radius = 16.dpToPx().toFloat()
            cardElevation = 4.dpToPx().toFloat()
            isClickable = true
            isFocusable = true
            isEnabled = true
            tag = index

            setCardBackgroundColor(Color.WHITE)

            setOnClickListener {
                if (!isAnswerSelected && isEnabled) {
                    val responseTime = System.currentTimeMillis() - responseStartTime
                    responseTimes.add(responseTime)
                    handleOptionClick(this, index, word, correctKeyword, responseTime)
                }
            }
        }

        // 🔴 MISSING CODE - Add this:
        val imageView = ImageView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER

            try {
                val drawable = ContextCompat.getDrawable(this@RhythmSummaryActivity, imageResId)
                if (drawable != null) {
                    setImageResource(imageResId)
                } else {
                    setImageResource(android.R.drawable.ic_menu_help)
                }
            } catch (e: Exception) {
                setImageResource(android.R.drawable.ic_menu_help)
            }
        }

        card.addView(imageView)
        optionsGrid.addView(card)

        // Entrance animation
        card.alpha = 0f
        card.translationY = 100f
        card.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(index * 100L)
            .start()
    }

    private fun createSimplifiedOptionCard(index: Int, word: String, imageResId: Int, size: Int, correctKeyword: String) {
        val card = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                gravity = android.view.Gravity.CENTER
                marginStart = if (index == 0) 0 else 16.dpToPx()
            }
            radius = 24.dpToPx().toFloat() // Larger radius for simplicity
            cardElevation = 8.dpToPx().toFloat() // Higher elevation for prominence
            isClickable = true
            tag = index

            // High contrast background
            setCardBackgroundColor(if (index == 0) Color.parseColor("#E3F2FD") else Color.parseColor("#FFF3E0"))

            setOnClickListener {
                Log.d(TAG, "Simplified option $index clicked: $word")
                if (!isAnswerSelected) {
                    val responseTime = System.currentTimeMillis() - responseStartTime
                    responseTimes.add(responseTime)
                    handleOptionClick(this, index, word, correctKeyword, responseTime)
                }
            }
        }

        val imageView = ImageView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                (size * 0.8).toInt(),
                (size * 0.8).toInt()
            )
            scaleType = ImageView.ScaleType.FIT_CENTER

            try {
                val drawable = ContextCompat.getDrawable(this@RhythmSummaryActivity, imageResId)
                if (drawable != null) {
                    setImageResource(imageResId)
                } else {
                    setImageResource(android.R.drawable.ic_menu_help)
                }
            } catch (e: Exception) {
                setImageResource(android.R.drawable.ic_menu_help)
            }
        }

        // Add text label for clarity
        val textView = TextView(this).apply {
            text = word
            textSize = 20f
            setTextColor(Color.BLACK)
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(imageView)
            addView(textView)
        }

        card.addView(container)
        simplifiedOptionsContainer.addView(card)

        // More pronounced entrance animation
        card.alpha = 0f
        card.scaleX = 0.5f
        card.scaleY = 0.5f
        card.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setStartDelay(index * 200L)
            .start()
    }

    private fun handleOptionClick(card: CardView, selectedIndex: Int, selectedWord: String,
                                  correctKeyword: String, responseTime: Long) {

        // Prevent clicks on disabled cards
        if (!card.isClickable || isAnswerSelected) {
            Log.d(TAG, "⛔ Click ignored - card not clickable or answer already selected")
            return
        }

        // Prevent too-fast clicks (minimum 800ms after options appear)
        if (responseTime < 800) {
            Log.d(TAG, "⚡ Too fast click: ${responseTime}ms - ignoring")
            Toast.makeText(this, "Take your time! Look carefully.", Toast.LENGTH_SHORT).show()
            return
        }

        // Stop the timer
        isTimerActive = false
        timeoutHandler?.removeCallbacksAndMessages(null)


        Log.d(TAG, "🖱️ Option $selectedIndex clicked: $selectedWord, Time: ${responseTime}ms")
        isAnswerSelected = true

        // Disable all cards to prevent multiple clicks
        disableAllCards()

        val isCorrect = selectedIndex == correctAnswerIndex
        gameMaster.trackResponse(responseStartTime, isCorrect, selectedWord, correctKeyword)

        if (isCorrect) {
            consecutiveWrongAnswers = 0
            // Track success even in confidence builder mode
            if (isConfidenceBuilderMode) {
                confidenceBuilderSuccesses++
                Log.d(TAG, "🎯 Confidence builder success! Total: $confidenceBuilderSuccesses")
            }
        } else {
            consecutiveWrongAnswers++
            if (isConfidenceBuilderMode) {
                Log.d(TAG, "😓 Confidence builder attempt failed")
            }
        }

        // Play sound
        try {
            if (isCorrect) correctSound.start() else wrongSound.start()
        } catch (e: Exception) {
            Log.w(TAG, "Could not play sound")
        }

        if (isCorrect) {
            Log.d(TAG, "✅ Correct!")
            score++
            showFeedback(true, card)

            // Exit confidence builder on success
            if (isConfidenceBuilderMode) {
                isConfidenceBuilderMode = false
                therapeuticMessage.visibility = View.GONE
                simplifiedOptionsContainer.visibility = View.GONE
                optionsGrid.visibility = View.VISIBLE
                confidenceBuilderOverlay.visibility = View.GONE  // 🔴 ADD THIS
            }
        } else {
            Log.d(TAG, "❌ Wrong!")
            showFeedback(false, card)
            // Highlight correct answer but still show it's wrong
            highlightCorrectAnswer()
        }

        updateScore()

        // Show feedback, then next button
        Handler(Looper.getMainLooper()).postDelayed({
            hideFeedbackAndShowNextButton()
        }, 1500) // 1.5 seconds delay
    }

    private fun handleTimeout() {
        if (!isAnswerSelected) {
            isAnswerSelected = true
            isTimerActive = false
            timeoutHandler?.removeCallbacksAndMessages(null)
            Log.d(TAG, "💡 Showing hint after timeout")

            therapeuticMessage.text = "Look carefully! The correct answer is highlighted."
            therapeuticMessage.visibility = View.VISIBLE

            // Highlight correct answer but STAY CLICKABLE
            val correctCard = if (isConfidenceBuilderMode) {
                simplifiedOptionsContainer.getChildAt(correctAnswerIndex) as? androidx.cardview.widget.CardView
            } else {
                optionsGrid.getChildAt(correctAnswerIndex) as? androidx.cardview.widget.CardView
            }

            correctCard?.let {
                // SIMPLE FIX: Just use background color, no border
                it.setCardBackgroundColor(Color.parseColor("#E8F5E9")) // Very light green
                it.isClickable = true // Keep it clickable!
                it.isEnabled = true // Make sure it's enabled
            }

            // Auto-proceed after longer delay
            Handler(Looper.getMainLooper()).postDelayed({
                if (isAnswerSelected) {
                    hideFeedbackAndShowNextButton()
                }
            }, 5000) // Give 5 more seconds after hint
        }
    }

    private fun disableAllCards() {
        if (isConfidenceBuilderMode) {
            for (i in 0 until simplifiedOptionsContainer.childCount) {
                val child = simplifiedOptionsContainer.getChildAt(i)
                if (child is CardView) {
                    child.isClickable = false
                }
            }
        } else {
            for (i in 0 until optionsGrid.childCount) {
                val child = optionsGrid.getChildAt(i)
                if (child is CardView) {
                    child.isClickable = false
                }
            }
        }
    }

    private fun highlightCorrectAnswer() {
        val correctCard = if (isConfidenceBuilderMode) {
            simplifiedOptionsContainer.getChildAt(correctAnswerIndex) as? CardView
        } else {
            optionsGrid.getChildAt(correctAnswerIndex) as? CardView
        }

        correctCard?.let {
            it.setCardBackgroundColor(Color.GREEN)
            it.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(100)
                .start()
        }
    }

    private fun showFeedback(isCorrect: Boolean, selectedCard: CardView) {
        if (isCorrect) {
            selectedCard.setCardBackgroundColor(Color.GREEN)
            try {
                feedbackIcon.setImageResource(R.drawable.correct_answer)
            } catch (e: android.content.res.Resources.NotFoundException) {
                feedbackIcon.setImageResource(android.R.drawable.ic_menu_report_image)
            }
            feedbackText.text = "Excellent!"
            feedbackText.setTextColor(Color.parseColor("#4CAF50"))

            selectedCard.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(100)
                .withEndAction {
                    selectedCard.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
        } else {
            selectedCard.setCardBackgroundColor(Color.RED)
            try {
                feedbackIcon.setImageResource(R.drawable.delete)
            } catch (e: android.content.res.Resources.NotFoundException) {
                feedbackIcon.setImageResource(android.R.drawable.ic_delete)
            }
            feedbackText.text = "Try again!"
            feedbackText.setTextColor(Color.parseColor("#F44336"))

            val shake = TranslateAnimation(0f, 20f, 0f, 0f)
            shake.duration = 50
            shake.repeatCount = 4
            shake.repeatMode = TranslateAnimation.REVERSE
            selectedCard.startAnimation(shake)
        }

        feedbackIcon.visibility = View.VISIBLE
        feedbackText.visibility = View.VISIBLE
        feedbackOverlay.visibility = View.VISIBLE

        feedbackOverlay.alpha = 0f
        feedbackOverlay.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        if (!::feedbackContainer.isInitialized) {
            // Try to find it again
            feedbackContainer = findViewById(R.id.feedbackContainer)
        }

        // 🔴 FIX THIS - Use feedbackContainer variable instead of getChildAt(0)
        feedbackContainer.scaleX = 0.5f
        feedbackContainer.scaleY = 0.5f
        feedbackContainer.alpha = 0f

        feedbackContainer.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(500)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    private fun hideFeedbackAndShowNextButton() {
        feedbackOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                feedbackOverlay.visibility = View.GONE
                feedbackIcon.visibility = View.GONE
                feedbackText.visibility = View.GONE
                therapeuticMessage.visibility = View.GONE

                // 🔴 CRITICAL FIX - Reset feedback container properties
                if (::feedbackContainer.isInitialized) {
                    feedbackContainer.scaleX = 1f
                    feedbackContainer.scaleY = 1f
                    feedbackContainer.alpha = 1f
                }
            }
            .start()

        nextButton.visibility = View.VISIBLE
        nextButton.alpha = 0f
        nextButton.translationY = 50f
        nextButton.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .start()
    }

    private fun updateScore() {
        scoreText.text = "$score/$totalRounds"

        scoreText.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(200)
            .withEndAction {
                scoreText.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun updateProgressDots() {
        for (i in 0 until progressContainer.childCount) {
            val dot = progressContainer.getChildAt(i)
            dot.setBackgroundColor(
                when {
                    i == currentRound -> Color.parseColor("#4CAF50")
                    i < currentRound -> Color.parseColor("#FF6B35")
                    else -> Color.parseColor("#BDBDBD")
                }
            )
            dot.background.setAlpha(200)
        }
    }

    private fun onNextButtonClick() {
        nextButton.visibility = View.GONE

        // 🔴 FIX: Only increment currentRound for NORMAL rounds
        if (!isConfidenceBuilderMode) {
            currentRound++
            Log.d(TAG, "✅ Normal round completed. Progress: $currentRound/$totalRounds")
        } else {
            Log.d(TAG, "🔄 Confidence builder round completed - not counting toward total")
            // Reset confidence builder mode but DON'T increment round counter
            isConfidenceBuilderMode = false
            therapeuticMessage.visibility = View.GONE
            confidenceBuilderOverlay.visibility = View.GONE
            simplifiedOptionsContainer.visibility = View.GONE
            optionsGrid.visibility = View.VISIBLE
        }

        startNewRound()
    }

    private fun navigateToScoreboard() {
        Log.d(TAG, "Navigating to scoreboard. Score: $score, Total rounds: $totalRounds")
        try {
            val intent = Intent(this, RMScoreboardActivity::class.java)
            intent.putExtra("SCORE", score)
            intent.putExtra("TOTAL_ROUNDS", totalRounds)
            intent.putExtra("SONG_TITLE", currentSongTitle)

            // Pass session metrics for therapeutic analysis
            val sessionMetrics = gameMaster.getSessionSummary()
            intent.putExtra("SESSION_METRICS", sessionMetrics)

            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to scoreboard: ${e.message}", e)
            Toast.makeText(this, "Error loading scoreboard", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Releasing media players")
        try {
            mediaPlayer.release()
            correctSound.release()
            wrongSound.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing media players", e)
        }
    }
}