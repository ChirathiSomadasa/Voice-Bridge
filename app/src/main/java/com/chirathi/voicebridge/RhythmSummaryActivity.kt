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
    private var totalRounds = 5
    private var correctAnswerIndex = 0
    private var isAnswerSelected = false
    private var isConfidenceBuilderMode = false
    private var individualLatencyBuffer: Long = 3000
    private var therapeuticIntent: TherapeuticIntent? = null

    // 🔴 FIX: Added these trackers
    private var confidenceBuilderSuccesses = 0
    private var consecutiveWrongAnswers = 0
    private var isConfidenceBuilderRoundActive = false // Separate flag for active round

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

            gameMaster = GameMasterModel(this)
            initializeViews()
            Log.d(TAG, "Views initialized successfully")

            currentSongTitle = intent.getStringExtra("SONG_TITLE") ?: "Row Row Row Your Boat"
            Log.d(TAG, "Received song title: $currentSongTitle")

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

            setupSongData()
            Log.d(TAG, "Song data setup complete")

            individualLatencyBuffer = gameMaster.getIndividualizedLatencyBuffer()
            Log.d(TAG, "Individual latency buffer: ${individualLatencyBuffer}ms")

            setupUI()
            Log.d(TAG, "UI setup complete")

            Handler(Looper.getMainLooper()).postDelayed({
                startNewRound()
                Log.d(TAG, "First round started")
            }, 1000)

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
            createTherapeuticComponentsIfMissing()
        }
    }

    private fun createTherapeuticComponentsIfMissing() {
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
            adjustUIComplexity()
            animatePanda()
            setupProgressDots()
            updateScore()
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupUI: ${e.message}", e)
        }
    }

    private fun adjustUIComplexity() {
        val intent = therapeuticIntent ?: return

        val columnCount = when {
            intent.uiComplexity < 0.4f -> 1
            intent.uiComplexity < 0.6f -> 2
            else -> 2
        }

        optionsGrid.columnCount = columnCount
        optionsGrid.rowCount = if (columnCount == 1) 4 else 2

        wordTitle.textSize = when {
            intent.uiComplexity < 0.4f -> 24f
            intent.uiComplexity < 0.6f -> 28f
            else -> 32f
        }

        if (intent.uiComplexity < 0.5f) {
            findViewById<View>(R.id.rootLayout).setBackgroundColor(Color.WHITE)
            wordTitle.setTextColor(Color.BLACK)
        }

        totalRounds = when (intent.sessionDuration) {
            in 0..5 -> 3
            in 6..10 -> 5
            in 11..15 -> 7
            else -> 5
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
        Log.d(TAG, "🎮 Starting new round ${currentRound + 1}/$totalRounds (Confidence mode: $isConfidenceBuilderRoundActive)")

        if (currentRound >= totalRounds && !isConfidenceBuilderRoundActive) {
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

        // 🔴 FIX: Check if we should activate confidence builder
        if (shouldActivateConfidenceBuilder()) {
            activateConfidenceBuilderMode()
            startResponseTimer()
            return
        }

        // Normal round setup
        val keyword = getUniqueKeyword()
        val correctImageRes = keywordImages[keyword.word] ?: android.R.drawable.ic_dialog_info
        val wrongOptions = generateDiagnosticDistractors(keyword.word)
        val gameRound = createGameRound(keyword.word, correctImageRes, wrongOptions)

        wordTitle.text = "Find: ${keyword.word.uppercase()}"
        displayOptions(gameRound)
        updateProgressDots()
        optionsGrid.isEnabled = true

        startResponseTimer()
    }

    private fun startResponseTimer() {
        timeoutHandler?.removeCallbacksAndMessages(null)
        isTimerActive = true

        // 🔴 FIX: Different delays for different modes
        val optionsVisibleDelay = if (isConfidenceBuilderRoundActive) 300 else 1500
        val timeoutDuration = if (isConfidenceBuilderRoundActive) 10000 else 5000
        val minResponseTime = if (isConfidenceBuilderRoundActive) 300 else 800

        Handler(Looper.getMainLooper()).postDelayed({
            responseStartTime = System.currentTimeMillis()
            Log.d(TAG, "⏰ Timer started at ${responseStartTime} - options now visible")

            // Store the visible time separately for "too fast" detection
            val optionsVisibleTime = responseStartTime

            timeoutHandler = Handler(Looper.getMainLooper())
            timeoutHandler?.postDelayed({
                if (!isAnswerSelected && isTimerActive) {
                    Log.d(TAG, "⏰ Response timeout triggered after ${timeoutDuration}ms")
                    handleTimeout()
                }
            }, timeoutDuration.toLong())
        }, optionsVisibleDelay.toLong())
    }

    private fun shouldActivateConfidenceBuilder(): Boolean {
        if (currentRound == 0) return false
        if (isConfidenceBuilderRoundActive) return false // Don't reactivate if already in a confidence builder round

        // Check for 2 consecutive wrong answers in NORMAL mode
        if (consecutiveWrongAnswers >= 2) {
            Log.d(TAG, "🎯 2 consecutive wrong answers - activating confidence builder WITHOUT counting as round")
            return true
        }

        return therapeuticIntent?.primaryGoal == "build_confidence"
    }

    private fun activateConfidenceBuilderMode() {
        Log.d(TAG, "Activating Confidence Builder Mode")
        isConfidenceBuilderMode = true
        isConfidenceBuilderRoundActive = true // 🔴 FIX: Set the active flag

        therapeuticMessage.text = "Let's try an easier one! You can do it!"
        therapeuticMessage.visibility = View.VISIBLE

        confidenceBuilderOverlay.visibility = View.VISIBLE
        simplifiedOptionsContainer.visibility = View.VISIBLE
        optionsGrid.visibility = View.GONE
        simplifiedOptionsContainer.removeAllViews()

        val keyword = getUniqueKeyword()
        val correctImageRes = keywordImages[keyword.word] ?: android.R.drawable.ic_dialog_info

        // 🔴 FIX: Always keep correct answer at index 0
        val simpleOptions = listOf(
            Pair(keyword.word, correctImageRes),  // Index 0 = correct
            Pair("apple", android.R.drawable.ic_menu_help)  // Index 1 = wrong
        )

        correctAnswerIndex = 0 // Explicitly set correct answer index

        val gameRound = GameRound(keyword.word, correctImageRes, simpleOptions)
        displaySimplifiedOptions(gameRound)

        wordTitle.text = "Tap the ${keyword.word}:"
        individualLatencyBuffer = (individualLatencyBuffer * 1.5f).toLong()

        Log.d(TAG, "Confidence builder activated with keyword: ${keyword.word}")
        Log.d(TAG, "Correct answer index set to: $correctAnswerIndex")
    }

    private fun generateDiagnosticDistractors(keyword: String): List<Pair<String, Int>> {
        val diagnosticDistractors = gameMaster.getDiagnosticDistractors(keyword, 3)
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
        if (availableKeywords.isEmpty() || usedKeywords.size >= currentKeywords.size * 0.7) {
            availableKeywords = currentKeywords.toMutableList()
            usedKeywords.clear()
            Log.d(TAG, "🔄 Reset available keywords pool")
        }

        val available = availableKeywords.filter { it.word !in usedKeywords }

        val keyword = if (available.isNotEmpty()) {
            available.random()
        } else {
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
        val allOptions = mutableListOf(Pair(keyword, correctImageRes))
        allOptions.addAll(wrongOptions.take(3))
        val shuffledOptions = allOptions.shuffled()
        correctAnswerIndex = shuffledOptions.indexOfFirst { it.first == keyword }
        return GameRound(keyword, correctImageRes, shuffledOptions)
    }

    private fun displayOptions(gameRound: GameRound) {
        val columnCount = optionsGrid.columnCount
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

        // 🔴 FIX: Don't shuffle - maintain order
        for (i in gameRound.options.indices) {
            val option = gameRound.options[i]
            createSimplifiedOptionCard(i, option.first, option.second, optionSize, gameRound.keyword)
        }

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
            radius = 24.dpToPx().toFloat()
            cardElevation = 8.dpToPx().toFloat()
            isClickable = true
            tag = index

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

        if (!card.isClickable || isAnswerSelected) {
            Log.d(TAG, "⛔ Click ignored - card not clickable or answer already selected")
            return
        }

        val minResponseTime = if (isConfidenceBuilderRoundActive) 300 else 800
        val optionsVisibleDelay = if (isConfidenceBuilderRoundActive) 300 else 1500
        val timeFromOptionsVisible = System.currentTimeMillis() - (responseStartTime - optionsVisibleDelay)
        Log.d(TAG, "🖱️ Option $selectedIndex clicked: $selectedWord, Time from options: ${timeFromOptionsVisible}ms")
        val isHighlightedCorrectCard = selectedIndex == correctAnswerIndex &&
                (card.cardBackgroundColor.defaultColor == Color.parseColor("#E8F5E9") ||
                        card.cardBackgroundColor.defaultColor == Color.GREEN)

        if (isHighlightedCorrectCard) {
            card.setCardBackgroundColor(Color.WHITE) // or default color
        }

        if (responseTime < minResponseTime && !isHighlightedCorrectCard) {
            Log.d(TAG, "⚡ Too fast click: ${responseTime}ms - ${if (isConfidenceBuilderRoundActive) "allowing with warning" else "ignoring"}")

            if (isConfidenceBuilderRoundActive) {
                Toast.makeText(this, "Take a deep breath, you've got this!", Toast.LENGTH_SHORT).show()
                // Don't return - allow the click to process
            } else {
                Toast.makeText(this, "Take your time! Look carefully.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        isTimerActive = false
        timeoutHandler?.removeCallbacksAndMessages(null)

        Log.d(TAG, "🖱️ Option $selectedIndex clicked: $selectedWord, Time: ${responseTime}ms")
        isAnswerSelected = true

        disableAllCards()

        val isCorrect = selectedIndex == correctAnswerIndex
        gameMaster.trackResponse(responseStartTime, isCorrect, selectedWord, correctKeyword)

        // 🔴 FIX: Track consecutive wrong answers properly
        if (isCorrect) {
            consecutiveWrongAnswers = 0
            if (isConfidenceBuilderRoundActive) {
                confidenceBuilderSuccesses++
                Log.d(TAG, "🎯 Confidence builder success! Total: $confidenceBuilderSuccesses")
            }
            score++ // Increment score for correct answers in ALL modes
        } else {
            consecutiveWrongAnswers++
            Log.d(TAG, "❌ Wrong answer - Consecutive wrong: $consecutiveWrongAnswers")
        }

        try {
            if (isCorrect) correctSound.start() else wrongSound.start()
        } catch (e: Exception) {
            Log.w(TAG, "Could not play sound")
        }

        if (isCorrect) {
            Log.d(TAG, "✅ Correct! Score: $score")
            showFeedback(true, card)

            // 🔴 FIX: Exit confidence builder mode on success
            if (isConfidenceBuilderRoundActive) {
                Log.d(TAG, "🎉 Confidence builder success - exiting mode")
                isConfidenceBuilderMode = false
                isConfidenceBuilderRoundActive = false
                therapeuticMessage.visibility = View.GONE
                simplifiedOptionsContainer.visibility = View.GONE
                optionsGrid.visibility = View.VISIBLE
                confidenceBuilderOverlay.visibility = View.GONE
            }
        } else {
            Log.d(TAG, "❌ Wrong!")
            showFeedback(false, card)
            highlightCorrectAnswer()
        }

        updateScore()

        Handler(Looper.getMainLooper()).postDelayed({
            hideFeedbackAndShowNextButton()
        }, 1500)
    }

    private fun handleTimeout() {
        if (!isAnswerSelected) {
            isTimerActive = false
            timeoutHandler?.removeCallbacksAndMessages(null)
            Log.d(TAG, "💡 Showing hint after timeout")

            therapeuticMessage.text = "Look carefully! The correct answer is highlighted."
            therapeuticMessage.visibility = View.VISIBLE

            val correctCard = if (isConfidenceBuilderRoundActive) {
                simplifiedOptionsContainer.getChildAt(correctAnswerIndex) as? CardView
            } else {
                optionsGrid.getChildAt(correctAnswerIndex) as? CardView
            }

            correctCard?.let {
                it.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                it.isClickable = true
                it.isEnabled = true
            }

            Handler(Looper.getMainLooper()).postDelayed({
                if (!isAnswerSelected) {
                    Log.d(TAG, "⏰ Auto-proceeding after timeout with no answer")
                    hideFeedbackAndShowNextButton()
                }
            }, 5000)
        }
    }

    private fun disableAllCards() {
        if (isConfidenceBuilderRoundActive) {
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
        val correctCard = if (isConfidenceBuilderRoundActive) {
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
            feedbackContainer = findViewById(R.id.feedbackContainer)
        }

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

        // 🔴 FIX: Properly handle round counting
        if (isConfidenceBuilderRoundActive) {
            Log.d(TAG, "🔄 Confidence builder round completed - not counting toward total")
            // Reset all confidence builder flags
            isConfidenceBuilderMode = false
            isConfidenceBuilderRoundActive = false
            therapeuticMessage.visibility = View.GONE
            confidenceBuilderOverlay.visibility = View.GONE
            simplifiedOptionsContainer.visibility = View.GONE
            optionsGrid.visibility = View.VISIBLE

            // Don't increment currentRound
        } else {
            currentRound++
            Log.d(TAG, "✅ Normal round completed. Progress: $currentRound/$totalRounds")
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