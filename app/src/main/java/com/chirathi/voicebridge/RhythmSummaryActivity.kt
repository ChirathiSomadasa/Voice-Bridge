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
import kotlin.math.max

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
    private lateinit var levelBadge: androidx.cardview.widget.CardView
    private lateinit var levelIndicator: TextView
    private lateinit var strategyIndicator: TextView

    // Game Master Model
    private lateinit var gameMaster: GameMasterModel

    // Game State
    private var currentRound = 0
    private var score = 0
    private var totalRounds = 5
    private var correctAnswerIndex = 0
    private var isAnswerSelected = false
    private var isConfidenceBuilderMode = false
    private var isConfidenceBuilderRoundActive = false
    private var individualLatencyBuffer: Long = 3000

    // Model-based decisions
    private var currentModelDecision: ModelDecision? = null
    private var currentDifficultyLevel = 1
    private var currentDistractorStrategy: DistractorStrategy? = null
    private var currentTherapeuticIntent: TherapeuticIntent? = null

    // Performance tracking
    private var consecutiveCorrectAtLevel = 0
    private var consecutiveWrongAnswers = 0
    private var responseStartTime: Long = 0
    private val responseTimes = mutableListOf<Long>()
    private val touchPatterns = mutableListOf<Pair<Float, Float>>()

    // Keywords
    private val usedKeywords = mutableSetOf<String>()
    private lateinit var currentKeywords: List<Keyword>
    private lateinit var availableKeywords: MutableList<Keyword>

    // Audio
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var correctSound: MediaPlayer
    private lateinit var wrongSound: MediaPlayer
    private lateinit var instructionsText: TextView
    private lateinit var rootLayout: View
    private lateinit var feedbackContainer: LinearLayout

    // Timer
    private var isTimerActive = false
    private var timeoutHandler: Handler? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting RhythmSummaryActivity")

        try {
            setContentView(R.layout.activity_rythm_summary)

            // Initialize Game Master
            gameMaster = GameMasterModel(this)

            // Initialize Views
            initializeViews()

            // Get intent data
            currentSongTitle = intent.getStringExtra("SONG_TITLE") ?: "Row Row Row Your Boat"
            currentTherapeuticIntent = intent.getSerializableExtra("THERAPEUTIC_INTENT") as? TherapeuticIntent

            // Initialize audio
            initializeAudio()

            // Setup song data
            setupSongData()

            // Get model prediction for this session
            getModelDecision()

            // Setup UI
            setupUI()

            // Start first round
            Handler(Looper.getMainLooper()).postDelayed({
                startNewRound()
            }, 1000)

            nextButton.setOnClickListener {
                onNextButtonClick()
            }

            Log.d(TAG, "RhythmSummaryActivity initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "FATAL ERROR in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading game: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // =========== INITIALIZATION METHODS ===========

    private fun getModelDecision() {
        // Prepare input features for the model
        val inputFeatures = floatArrayOf(
            6f, // Age
            0f, // Previous score
            0f, // Previous accuracy
            0f, // Previous response time
            0f, // Previous frustration
            0f, // Previous engagement
            0f, // Previous level
            0f  // Previous strategy
        )

        currentModelDecision = gameMaster.predictWithBehavioralContext(inputFeatures)
        currentDifficultyLevel = currentModelDecision?.recommendedLevel ?: 1
        currentDistractorStrategy = currentModelDecision?.distractorStrategy
        currentTherapeuticIntent = currentModelDecision?.therapeuticIntent

        Log.d(TAG, "🤖 Model Decision:")
        Log.d(TAG, "   Level: $currentDifficultyLevel")
        Log.d(TAG, "   Strategy: ${currentDistractorStrategy?.type}")
        Log.d(TAG, "   Intent: ${currentTherapeuticIntent?.primaryGoal}")
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

            // Level Badge components
            levelBadge = findViewById(R.id.levelBadge)
            levelIndicator = findViewById(R.id.levelIndicator)

            // Strategy Toast
            strategyIndicator = findViewById(R.id.strategyIndicator)

            // Therapeutic UI components
            confidenceBuilderOverlay = findViewById(R.id.confidenceBuilderOverlay) ?: createConfidenceBuilderOverlay()
            simplifiedOptionsContainer = findViewById(R.id.simplifiedOptionsContainer) ?: createSimplifiedOptionsContainer()
            therapeuticMessage = findViewById(R.id.therapeuticMessage) ?: createTherapeuticMessage()

            Log.d(TAG, "All views initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "ERROR initializing views: ${e.message}", e)
            createTherapeuticComponentsIfMissing()
        }
    }

    private fun initializeAudio() {
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
            updateLevelBadge()
            individualLatencyBuffer = gameMaster.getIndividualizedLatencyBuffer()
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupUI: ${e.message}", e)
        }
    }

    // =========== UI COMPLEXITY ===========

    private fun adjustUIComplexity() {
        val intent = currentTherapeuticIntent ?: return

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
            rootLayout.setBackgroundColor(Color.WHITE)
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

    // =========== LEVEL BADGE METHODS ===========

    private fun updateLevelBadge() {
        try {
            // Update text and colors based on level - FIXED: Use list instead of nested pairs
            val levelData = when (currentDifficultyLevel) {
                1 -> listOf("🌱", "Beginner", "⭐", "#FF6B35")
                2 -> listOf("🔍", "Explorer", "⭐⭐", "#4A90E2")
                3 -> listOf("🏆", "Master", "⭐⭐⭐", "#9B59B6")
                4 -> listOf("👑", "Genius", "⭐⭐⭐⭐", "#F1C40F")
                else -> listOf("🎯", "Level $currentDifficultyLevel", "⭐", "#FF6B35")
            }
            levelIndicator.text = levelData[1]
            levelBadge.setCardBackgroundColor(Color.parseColor(levelData[3]))

            // Bounce animation
            levelBadge.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(200)
                .withEndAction {
                    levelBadge.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
                .start()

            Log.d(TAG, "Level badge updated: ${levelData[1]} ${levelData[2]}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating level badge: ${e.message}", e)
        }
    }

    // =========== STRATEGY TOAST METHODS ===========

    private fun showStrategyToast() {
        try {
            val strategyText = when (currentDistractorStrategy?.type) {
                "random" -> "🎯 Easy Peasy!"
                "phonetic" -> "🔊 Listen for Rhymes!"
                "semantic" -> "📚 Same Meaning!"
                "visual" -> "👁️ Look at Shapes!"
                "mixed" -> "🔄 Super Mix!"
                "diagnostic" -> "🎓 You're Improving!"
                else -> "🎯 Let's Learn!"
            }

            strategyIndicator.text = strategyText
            strategyIndicator.visibility = View.VISIBLE

            // Fade in
            strategyIndicator.animate()
                .alpha(1f)
                .setDuration(500)
                .withEndAction {
                    // Stay for 2.5 seconds then fade out
                    Handler(Looper.getMainLooper()).postDelayed({
                        strategyIndicator.animate()
                            .alpha(0f)
                            .setDuration(500)
                            .withEndAction {
                                strategyIndicator.visibility = View.GONE
                            }
                            .start()
                    }, 2500)
                }
                .start()

            Log.d(TAG, "Strategy toast shown: $strategyText")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing strategy toast: ${e.message}", e)
        }
    }

    private fun animatePanda() {
        try {
            val bounceAnimator = ObjectAnimator.ofFloat(pandaImage, "translationY", 0f, -20f, 0f)
            bounceAnimator.duration = 1000
            bounceAnimator.repeatCount = ObjectAnimator.INFINITE
            bounceAnimator.repeatMode = ObjectAnimator.REVERSE
            bounceAnimator.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error animating panda: ${e.message}", e)
        }
    }

    // =========== PROGRESS DOTS ===========

    private fun setupProgressDots() {
        try {
            progressContainer.removeAllViews()
            for (i in 0 until totalRounds) {
                val dot = View(this)
                val size = 16.dpToPx()
                val params = LinearLayout.LayoutParams(size, size)
                params.marginEnd = if (i < totalRounds - 1) 8.dpToPx() else 0
                dot.layoutParams = params

                // Make dots circular
                dot.background = ContextCompat.getDrawable(this, R.drawable.circular_dot)
                dot.setBackgroundColor(
                    when {
                        i == currentRound -> Color.parseColor("#4CAF50")  // Green = current
                        i < currentRound -> Color.parseColor("#FF6B35")   // Orange = completed
                        else -> Color.parseColor("#BDBDBD")               // Grey = upcoming
                    }
                )
                dot.background.setAlpha(200)
                progressContainer.addView(dot)
            }
            Log.d(TAG, "Setup $totalRounds progress dots")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up progress dots: ${e.message}", e)
        }
    }

    private fun updateProgressDots() {
        try {
            // Update colors of existing dots based on current round
            for (i in 0 until progressContainer.childCount) {
                val dot = progressContainer.getChildAt(i)
                dot.setBackgroundColor(
                    when {
                        i == currentRound -> Color.parseColor("#4CAF50")  // Green = current
                        i < currentRound -> Color.parseColor("#FF6B35")   // Orange = completed
                        else -> Color.parseColor("#BDBDBD")               // Grey = upcoming
                    }
                )
                dot.background.setAlpha(200)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating progress dots: ${e.message}", e)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    // =========== GAME ROUND MANAGEMENT ===========

    private fun startNewRound() {
        Log.d(TAG, "🎮 Starting round ${currentRound + 1}/$totalRounds")
        Log.d(TAG, "📊 Level: $currentDifficultyLevel, Strategy: ${currentDistractorStrategy?.type}")

        if (currentRound >= totalRounds && !isConfidenceBuilderRoundActive) {
            navigateToScoreboard()
            return
        }

        // Reset state
        isAnswerSelected = false
        nextButton.visibility = View.GONE

        // Clear views
        optionsGrid.removeAllViews()
        simplifiedOptionsContainer.removeAllViews()
        simplifiedOptionsContainer.visibility = View.GONE
        confidenceBuilderOverlay.visibility = View.GONE
        optionsGrid.visibility = View.VISIBLE
        therapeuticMessage.visibility = View.GONE

        // Check for confidence builder
        if (shouldActivateConfidenceBuilder()) {
            activateConfidenceBuilderMode()
            startResponseTimer()
            return
        }

        // Get model decision for this round
        if (currentRound > 0) {
            // Update model with performance
            val inputFeatures = floatArrayOf(
                6f,
                score.toFloat(),
                score.toFloat() / maxOf(1, currentRound),
                if (responseTimes.isNotEmpty()) responseTimes.average().toFloat() / 1000f else 0f,
                gameMaster.getSessionSummary().behavioralProfile?.frustrationLevel ?: 0f,
                gameMaster.getSessionSummary().behavioralProfile?.engagementScore ?: 0f,
                currentDifficultyLevel.toFloat(),
                currentDistractorStrategy?.hashCode()?.toFloat() ?: 0f
            )
            currentModelDecision = gameMaster.predictWithBehavioralContext(inputFeatures)
            currentDifficultyLevel = currentModelDecision?.recommendedLevel ?: currentDifficultyLevel
            currentDistractorStrategy = currentModelDecision?.distractorStrategy ?: currentDistractorStrategy

            // Update UI with new model decisions
            updateLevelBadge()
            showStrategyToast()
        }

        // Generate round with model-based distractors
        val keyword = getUniqueKeyword()
        val correctImageRes = keywordImages[keyword.word] ?: 0

        // Use model's strategy to generate distractors
        val wrongOptions = if (currentDistractorStrategy != null) {
            gameMaster.generateDistractorsByStrategy(
                keyword.word,
                currentDistractorStrategy!!,
                3
            )
        } else {
            gameMaster.generateDistractorsByLevel(keyword.word, currentDifficultyLevel, 3)
        }

        val gameRound = createGameRound(keyword.word, correctImageRes, wrongOptions)

        wordTitle.text = "Find: ${keyword.word.uppercase()}"
        displayOptions(gameRound)
        updateProgressDots()
        optionsGrid.isEnabled = true

        startResponseTimer()
    }

    private fun shouldActivateConfidenceBuilder(): Boolean {
        if (currentRound == 0) return false
        if (isConfidenceBuilderRoundActive) return false

        val therapeuticIntent = currentTherapeuticIntent
        val frustrationLevel = gameMaster.getSessionSummary().behavioralProfile?.frustrationLevel ?: 0f

        return when {
            consecutiveWrongAnswers >= 2 -> true
            therapeuticIntent?.primaryGoal == "build_confidence" &&
                    (therapeuticIntent.uiComplexity < 0.5f) -> true
            frustrationLevel > 0.7f -> true
            else -> false
        }
    }

    private fun activateConfidenceBuilderMode() {
        Log.d(TAG, "🎯 Activating Confidence Builder Mode")
        isConfidenceBuilderMode = true
        isConfidenceBuilderRoundActive = true

        therapeuticMessage.text = "Let's try an easier one! You can do it!"
        therapeuticMessage.visibility = View.VISIBLE

        confidenceBuilderOverlay.visibility = View.VISIBLE
        simplifiedOptionsContainer.visibility = View.VISIBLE
        optionsGrid.visibility = View.GONE
        simplifiedOptionsContainer.removeAllViews()

        val keyword = getUniqueKeyword()
        val correctImageRes = keywordImages[keyword.word] ?: 0

        // Only 2 options in confidence builder mode
        val simpleOptions = listOf(
            Pair(keyword.word, correctImageRes),
            Pair("apple", KeywordImageMapper.getImageResource("apple"))
        )

        correctAnswerIndex = 0
        val gameRound = GameRound(keyword.word, correctImageRes, simpleOptions)
        displaySimplifiedOptions(gameRound)

        wordTitle.text = "Tap the ${keyword.word}:"
        individualLatencyBuffer = (individualLatencyBuffer * 1.5f).toLong()
    }

    private fun getUniqueKeyword(): Keyword {
        if (availableKeywords.isEmpty() || usedKeywords.size >= currentKeywords.size * 0.7) {
            availableKeywords = currentKeywords.toMutableList()
            usedKeywords.clear()
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
        return keyword
    }

    private fun createGameRound(keyword: String, correctImageRes: Int, wrongOptions: List<Pair<String, Int>>): GameRound {
        val allOptions = mutableListOf(Pair(keyword, correctImageRes))
        allOptions.addAll(wrongOptions.take(3))
        val shuffledOptions = allOptions.shuffled()
        correctAnswerIndex = shuffledOptions.indexOfFirst { it.first == keyword }
        return GameRound(keyword, correctImageRes, shuffledOptions)
    }

    // =========== DISPLAY METHODS ===========

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

        for (i in gameRound.options.indices) {
            val option = gameRound.options[i]
            createSimplifiedOptionCard(i, option.first, option.second, optionSize, gameRound.keyword)
        }
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

        val contentView = KeywordImageMapper.createOptionCardView(
            context = this,
            word = word,
            imageResId = imageResId,
            size = size,
            isSimplified = false
        )

        card.addView(contentView)
        optionsGrid.addView(card)

        // Animation
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
                gravity = Gravity.CENTER
                marginStart = if (index == 0) 0 else 16.dpToPx()
            }
            radius = 24.dpToPx().toFloat()
            cardElevation = 8.dpToPx().toFloat()
            isClickable = true
            tag = index
            setCardBackgroundColor(
                if (index == 0) Color.parseColor("#E3F2FD")
                else Color.parseColor("#FFF3E0")
            )

            setOnClickListener {
                if (!isAnswerSelected) {
                    val responseTime = System.currentTimeMillis() - responseStartTime
                    responseTimes.add(responseTime)
                    handleOptionClick(this, index, word, correctKeyword, responseTime)
                }
            }
        }

        val contentView = KeywordImageMapper.createSimplifiedCardView(
            context = this,
            word = word,
            imageResId = imageResId,
            size = size
        )

        card.addView(contentView)
        simplifiedOptionsContainer.addView(card)

        // Animation
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

    // =========== TIMER METHODS ===========

    private fun startResponseTimer() {
        timeoutHandler?.removeCallbacksAndMessages(null)
        isTimerActive = true

        val optionsVisibleDelay = if (isConfidenceBuilderRoundActive) 300 else 1500
        val timeoutDuration = if (isConfidenceBuilderRoundActive) 10000 else
            (individualLatencyBuffer * 1.5f).toLong()

        Handler(Looper.getMainLooper()).postDelayed({
            responseStartTime = System.currentTimeMillis()
            timeoutHandler = Handler(Looper.getMainLooper())
            timeoutHandler?.postDelayed({
                if (!isAnswerSelected && isTimerActive) {
                    handleTimeout()
                }
            }, timeoutDuration)
        }, optionsVisibleDelay.toLong())
    }

    private fun handleTimeout() {
        if (!isAnswerSelected) {
            isTimerActive = false
            timeoutHandler?.removeCallbacksAndMessages(null)

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
                    hideFeedbackAndShowNextButton()
                }
            }, 3000)
        }
    }

    // =========== CLICK HANDLING ===========

    private fun handleOptionClick(card: CardView, selectedIndex: Int, selectedWord: String,
                                  correctKeyword: String, responseTime: Long) {

        if (!card.isClickable || isAnswerSelected) return

        val minResponseTime = if (isConfidenceBuilderRoundActive) 300 else 800
        if (responseTime < minResponseTime && !isConfidenceBuilderRoundActive) {
            Toast.makeText(this, "Take your time! Look carefully.", Toast.LENGTH_SHORT).show()
            return
        }

        isTimerActive = false
        timeoutHandler?.removeCallbacksAndMessages(null)
        isAnswerSelected = true
        disableAllCards()

        val isCorrect = selectedIndex == correctAnswerIndex

        // Track response with Game Master
        gameMaster.trackResponse(
            responseStartTime,
            isCorrect,
            selectedWord,
            correctKeyword,
            currentDifficultyLevel
        )

        if (isCorrect) {
            score++
            consecutiveCorrectAtLevel++
            consecutiveWrongAnswers = 0

            if (isConfidenceBuilderRoundActive) {
                isConfidenceBuilderMode = false
                isConfidenceBuilderRoundActive = false
                therapeuticMessage.visibility = View.GONE
                simplifiedOptionsContainer.visibility = View.GONE
                optionsGrid.visibility = View.VISIBLE
                confidenceBuilderOverlay.visibility = View.GONE
            }

            showFeedback(true, card)
            playSound(correctSound)

        } else {
            consecutiveWrongAnswers++
            consecutiveCorrectAtLevel = 0
            showFeedback(false, card)
            highlightCorrectAnswer()
            playSound(wrongSound)
        }

        // Update difficulty level based on model recommendation
        val newLevel = gameMaster.getRecommendedLevel()
        if (newLevel != currentDifficultyLevel) {
            currentDifficultyLevel = newLevel
            updateLevelBadge()
            showLevelChangeMessage()
        }

        updateScore()
        updateProgressDots()

        Handler(Looper.getMainLooper()).postDelayed({
            hideFeedbackAndShowNextButton()
        }, 1500)
    }

    private fun disableAllCards() {
        if (isConfidenceBuilderRoundActive) {
            for (i in 0 until simplifiedOptionsContainer.childCount) {
                (simplifiedOptionsContainer.getChildAt(i) as? CardView)?.isClickable = false
            }
        } else {
            for (i in 0 until optionsGrid.childCount) {
                (optionsGrid.getChildAt(i) as? CardView)?.isClickable = false
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

    // =========== FEEDBACK METHODS ===========

    private fun showFeedback(isCorrect: Boolean, selectedCard: CardView) {
        if (isCorrect) {
            selectedCard.setCardBackgroundColor(Color.GREEN)
            try {
                feedbackIcon.setImageResource(R.drawable.correct_answer)
            } catch (e: Exception) {
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
            } catch (e: Exception) {
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

                feedbackContainer.scaleX = 1f
                feedbackContainer.scaleY = 1f
                feedbackContainer.alpha = 1f
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

    private fun playSound(mediaPlayer: MediaPlayer) {
        try {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
                mediaPlayer.prepare()
            }
            mediaPlayer.start()
        } catch (e: Exception) {
            Log.w(TAG, "Could not play sound")
        }
    }

    // =========== UI UPDATE METHODS ===========

    private fun showLevelChangeMessage() {
        val message = when (currentDifficultyLevel) {
            1 -> "Starting as Beginner! 🌱"
            2 -> "Now an Explorer! 🔍"
            3 -> "You're a Master! 🏆"
            4 -> "Absolute Genius! 👑"
            else -> "Level changed to $currentDifficultyLevel"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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

    // =========== NAVIGATION ===========

    private fun onNextButtonClick() {
        nextButton.visibility = View.GONE

        if (isConfidenceBuilderRoundActive) {
            isConfidenceBuilderMode = false
            isConfidenceBuilderRoundActive = false
            therapeuticMessage.visibility = View.GONE
            confidenceBuilderOverlay.visibility = View.GONE
            simplifiedOptionsContainer.visibility = View.GONE
            optionsGrid.visibility = View.VISIBLE
        } else {
            currentRound++
        }

        startNewRound()
    }

    private fun navigateToScoreboard() {
        Log.d(TAG, "Navigating to scoreboard. Score: $score/$totalRounds")
        try {
            val intent = Intent(this, RMScoreboardActivity::class.java)
            intent.putExtra("SCORE", score)
            intent.putExtra("TOTAL_ROUNDS", totalRounds)
            intent.putExtra("SONG_TITLE", currentSongTitle)
            intent.putExtra("FINAL_LEVEL", currentDifficultyLevel)
            intent.putExtra("FINAL_STRATEGY", currentDistractorStrategy?.type)
            intent.putExtra("FINAL_LEVEL_NAME", levelIndicator.text)

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

    // =========== UI COMPONENT CREATION ===========

    private fun createConfidenceBuilderOverlay(): FrameLayout {
        return FrameLayout(this).apply {
            id = R.id.confidenceBuilderOverlay
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#80FFFFFF"))
            visibility = View.GONE
            (rootLayout as ViewGroup).addView(this)
        }
    }

    private fun createSimplifiedOptionsContainer(): LinearLayout {
        return LinearLayout(this).apply {
            id = R.id.simplifiedOptionsContainer
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            confidenceBuilderOverlay.addView(this)
        }
    }

    private fun createTherapeuticMessage(): TextView {
        return TextView(this).apply {
            id = R.id.therapeuticMessage
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
            textSize = 18f
            setTextColor(Color.parseColor("#4CAF50"))
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
            confidenceBuilderOverlay.addView(this)
        }
    }

    private fun createTherapeuticComponentsIfMissing() {
        confidenceBuilderOverlay = createConfidenceBuilderOverlay()
        simplifiedOptionsContainer = createSimplifiedOptionsContainer()
        therapeuticMessage = createTherapeuticMessage()
    }

    // =========== TOUCH & LIFECYCLE ===========

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                gameMaster.trackTouch(event.x, event.y, System.currentTimeMillis())
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            timeoutHandler?.removeCallbacksAndMessages(null)
            mediaPlayer.release()
            correctSound.release()
            wrongSound.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources", e)
        }
    }
}