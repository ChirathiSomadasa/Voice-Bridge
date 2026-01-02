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
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.animation.TranslateAnimation
import android.widget.*
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

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

    private var currentRound = 0
    private var score = 0
    private var totalRounds = 5
    private var correctAnswerIndex = 0
    private var isAnswerSelected = false

    // Track used keywords to avoid repetition
    private val usedKeywords = mutableSetOf<String>()

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var correctSound: MediaPlayer
    private lateinit var wrongSound: MediaPlayer

    // Define Keyword data class
    data class Keyword(val word: String, val imageRes: Int, val startTime: Int, val endTime: Int)

    private val keywordImages = mapOf(
        "boat" to R.drawable.boat_image,
        "stream" to R.drawable.stream_image,
        "dream" to R.drawable.dream_image,
        "creek" to R.drawable.creek,
        "mouse" to R.drawable.mouse_image,
        "squeak" to R.drawable.squeak,
        "river" to R.drawable.river_image,
        "polar bear" to R.drawable.polar_bear_image,
        "crocodile" to R.drawable.crocodile,
        "scream" to R.drawable.scream_image,
        "star" to R.drawable.star_image,
        "world" to R.drawable.world,
        "diamond" to R.drawable.diamond_image,
        "sun" to R.drawable.sun,
        "light" to R.drawable.light,
        "night" to R.drawable.moon,
        "traveller" to R.drawable.traveller,
        "dark blue sky" to R.drawable.dark_blue_sky,
        "window" to R.drawable.window,
        "eyes" to R.drawable.eyes,
        "hill" to R.drawable.hill_image,
        "water" to R.drawable.water_image,
        "crown" to R.drawable.crown_image
    )

    // Distractor images map - using other song images as distractors
    private val distractorImages = mapOf(
        // For "Row Row Row Your Boat" song
        "car" to R.drawable.car,
        "plane" to R.drawable.plane,
        "bicycle" to R.drawable.bicycle_image,
        "ocean" to R.drawable.boat_image,
        "lake" to R.drawable.stream_image,
        "waterfall" to R.drawable.river_image,
        "mountain" to R.drawable.hill_image,
        "valley" to R.drawable.dream_image,
        "forest" to R.drawable.creek,

        // For "Twinkle Twinkle Little Star" song
        "moon" to R.drawable.moon,
        "planet" to R.drawable.planet,
        "sky" to R.drawable.dark_blue_sky,
        "cloud" to R.drawable.window,
        "eye" to R.drawable.eyes,
        "night sky" to R.drawable.star_image,

        // General distractors
        "juice" to R.drawable.sticker,
        "helmet" to R.drawable.helmet_image,
        "apple" to R.drawable.apple_image,
        "ball" to R.drawable.ball_image,
        "cat" to R.drawable.cat_image,
        "hat" to R.drawable.crown_image,
        "cap" to R.drawable.traveller
    )

    data class GameRound(val keyword: String, val correctImageRes: Int, val options: List<Pair<String, Int>>)

    private lateinit var currentKeywords: List<Keyword>
    private lateinit var availableKeywords: MutableList<Keyword>

    // Define the water-related keywords that shouldn't appear together
    private val waterKeywords = listOf("stream", "river", "creek")

    // Validated image caches
    private val validatedKeywordImages = mutableMapOf<String, Int>()
    private val validatedDistractorImages = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting RhythmSummaryActivity")

        try {
            setContentView(R.layout.activity_rythm_summary)
            Log.d(TAG, "Layout set successfully: activity_rythm_summary")

            // Initialize UI components
            initializeViews()
            Log.d(TAG, "Views initialized successfully")

            // Get song title from intent
            currentSongTitle = intent.getStringExtra("SONG_TITLE") ?: "Row Row Row Your Boat"
            Log.d(TAG, "Received song title: $currentSongTitle")

            try {
                // Initialize sound effects
                mediaPlayer = MediaPlayer.create(this, R.raw.button_click)
                correctSound = MediaPlayer.create(this, R.raw.correct_sound)
                wrongSound = MediaPlayer.create(this, R.raw.wrong_sound)
                Log.d(TAG, "Sound effects initialized")
            } catch (e: Exception) {
                Log.w(TAG, "Sound files not found, using silent players")
                mediaPlayer = MediaPlayer()
                correctSound = MediaPlayer()
                wrongSound = MediaPlayer()
            }

            // Validate and cache all images
            validateAllImages()
            Log.d(TAG, "Image validation complete")

            // Setup based on song
            setupSongData()
            Log.d(TAG, "Song data setup complete")

            setupUI()
            Log.d(TAG, "UI setup complete")

            startNewRound()
            Log.d(TAG, "First round started")

            // Set next button click listener
            nextButton.setOnClickListener {
                Log.d(TAG, "Next button clicked")
                onNextButtonClick()
            }

            Log.d(TAG, "RhythmSummaryActivity initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "FATAL ERROR in onCreate: ${e.message}", e)
            e.printStackTrace()

            // Show error to user
            Toast.makeText(this, "Error loading game: ${e.message}", Toast.LENGTH_LONG).show()

            // Try to go back
            try {
                finish()
            } catch (e2: Exception) {
                Log.e(TAG, "Error finishing activity: ${e2.message}", e2)
            }
        }
    }

    private fun validateAllImages() {
        Log.d(TAG, "Validating all images...")

        // Validate keyword images
        keywordImages.forEach { (word, resId) ->
            if (isDrawableValid(resId)) {
                validatedKeywordImages[word] = resId
                Log.d(TAG, "✓ Valid keyword image: $word ($resId)")
            } else {
                Log.w(TAG, "✗ Invalid keyword image: $word ($resId)")
                // Fallback to system drawable
                validatedKeywordImages[word] = android.R.drawable.ic_dialog_info
            }
        }

        // Validate distractor images
        distractorImages.forEach { (word, resId) ->
            if (isDrawableValid(resId)) {
                validatedDistractorImages[word] = resId
                Log.d(TAG, "✓ Valid distractor image: $word ($resId)")
            } else {
                Log.w(TAG, "✗ Invalid distractor image: $word ($resId)")
                // Fallback to system drawable
                validatedDistractorImages[word] = android.R.drawable.ic_dialog_info
            }
        }

        Log.d(TAG, "Image validation complete. Keywords: ${validatedKeywordImages.size}, Distractors: ${validatedDistractorImages.size}")
    }

    private fun isDrawableValid(resId: Int): Boolean {
        return try {
            val drawable = ContextCompat.getDrawable(this, resId)
            drawable != null
        } catch (e: Resources.NotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun initializeViews() {
        Log.d(TAG, "Initializing views...")
        try {
            pandaImage = findViewById(R.id.pandaImage)
            Log.d(TAG, "Found pandaImage: $pandaImage")

            wordTitle = findViewById(R.id.wordTitle)
            Log.d(TAG, "Found wordTitle: $wordTitle")

            scoreText = findViewById(R.id.scoreText)
            Log.d(TAG, "Found scoreText: $scoreText")

            progressContainer = findViewById(R.id.progressContainer)
            Log.d(TAG, "Found progressContainer: $progressContainer")

            optionsGrid = findViewById(R.id.optionsGrid)
            Log.d(TAG, "Found optionsGrid: $optionsGrid")

            feedbackOverlay = findViewById(R.id.feedbackOverlay)
            Log.d(TAG, "Found feedbackOverlay: $feedbackOverlay")

            feedbackIcon = findViewById(R.id.feedbackIcon)
            Log.d(TAG, "Found feedbackIcon: $feedbackIcon")

            feedbackText = findViewById(R.id.feedbackText)
            Log.d(TAG, "Found feedbackText: $feedbackText")

            nextButton = findViewById(R.id.nextButton)
            Log.d(TAG, "Found nextButton: $nextButton")

            Log.d(TAG, "All views initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "ERROR initializing views: ${e.message}", e)
            throw RuntimeException("Failed to initialize views: ${e.message}", e)
        }
    }

    private fun setupSongData() {
        Log.d(TAG, "Setting up song data for: $currentSongTitle")
        // Get keywords from the song that was played
        currentKeywords = when (currentSongTitle) {
            "Row Row Row Your Boat" -> {
                Log.d(TAG, "Loading Row Row Row Your Boat keywords")
                listOf(
                    Keyword("boat", R.drawable.boat_image, 11000, 12000),
                    Keyword("stream", R.drawable.stream_image, 13000, 15000),
                    Keyword("dream", R.drawable.dream_image, 18000, 20000),
                    Keyword("creek", R.drawable.creek, 22000, 25000),
                    Keyword("mouse", R.drawable.mouse_image, 25000, 27000),
                    Keyword("river", R.drawable.river_image, 33000, 35000),
                    Keyword("polar bear", R.drawable.polar_bear_image, 35000, 38000),
                    Keyword("crocodile", R.drawable.crocodile, 46000, 49000)
                )
            }
            "Twinkle Twinkle Little Star" -> {
                Log.d(TAG, "Loading Twinkle Twinkle Little Star keywords")
                listOf(
                    Keyword("star", R.drawable.star_image, 8000, 10000),
                    Keyword("world", R.drawable.world, 16000, 18000),
                    Keyword("diamond", R.drawable.diamond_image, 19000, 20000),
                    Keyword("sun", R.drawable.sun, 40000, 42000),
                    Keyword("light", R.drawable.light, 48000, 50000),
                    Keyword("night", R.drawable.moon, 53000, 55000),
                    Keyword("traveller", R.drawable.traveller, 67000, 69000),
                    Keyword("dark blue sky", R.drawable.dark_blue_sky, 100000, 102000),
                    Keyword("window", R.drawable.window, 104000, 106000),
                    Keyword("eyes", R.drawable.eyes, 109000, 111000)
                )
            }
            "Jack and Jill" -> {
                Log.d(TAG, "Loading Jack and Jill keywords")
                listOf(
                    Keyword("hill", R.drawable.hill_image, 10500, 11000),
                    Keyword("water", R.drawable.water_image, 12500, 14000),
                    Keyword("crown", R.drawable.crown_image, 14500, 16000)
                )
            }
            else -> {
                Log.w(TAG, "Unknown song title, using default keywords")
                listOf(
                    Keyword("boat", R.drawable.boat_image, 11000, 12000),
                    Keyword("stream", R.drawable.stream_image, 13000, 15000),
                    Keyword("dream", R.drawable.dream_image, 18000, 20000)
                )
            }
        }

        // Create a mutable copy for available keywords
        availableKeywords = currentKeywords.toMutableList()
        Log.d(TAG, "Available keywords: ${availableKeywords.size}")
    }

    private fun setupUI() {
        Log.d(TAG, "Setting up UI")
        try {
            // Setup panda animation
            animatePanda()
            Log.d(TAG, "Panda animation started")

            // Setup progress dots
            setupProgressDots()
            Log.d(TAG, "Progress dots setup")

            // Update score display
            updateScore()
            Log.d(TAG, "Score display updated")

        } catch (e: Exception) {
            Log.e(TAG, "Error in setupUI: ${e.message}", e)
        }
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
            Log.d(TAG, "Cleared progress container")

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
        Log.d(TAG, "Starting new round. Current round: $currentRound, Total rounds: $totalRounds")

        if (currentRound >= totalRounds) {
            Log.d(TAG, "Game finished! Score: $score/$totalRounds")
            // Game finished, navigate to scoreboard
            navigateToScoreboard()
            return
        }

        isAnswerSelected = false
        nextButton.visibility = View.GONE
        Log.d(TAG, "Reset round state")

        // Clear previous options
        optionsGrid.removeAllViews()
        Log.d(TAG, "Cleared previous options")

        // Get a keyword that hasn't been used yet
        val keyword = getUniqueKeyword()
        val correctImageRes = validatedKeywordImages[keyword.word] ?: android.R.drawable.ic_dialog_info
        Log.d(TAG, "Selected keyword: ${keyword.word}, validated image res: $correctImageRes")

        // Generate wrong options - use other song images as distractors
        val wrongOptions = generateWrongOptions(keyword.word)
        Log.d(TAG, "Generated ${wrongOptions.size} wrong options")

        // Create game round
        val gameRound = createGameRound(keyword.word, correctImageRes, wrongOptions)
        Log.d(TAG, "Game round created with ${gameRound.options.size} options")

        // Display the word to find
        wordTitle.text = "Find: ${keyword.word.uppercase()}"
        Log.d(TAG, "Word title set: ${wordTitle.text}")

        // Create and display options
        displayOptions(gameRound)
        Log.d(TAG, "Options displayed")

        // Update progress dots
        updateProgressDots()
        Log.d(TAG, "Progress dots updated")
    }

    private fun getUniqueKeyword(): Keyword {
        Log.d(TAG, "Getting unique keyword. Used: ${usedKeywords.size}, Available: ${availableKeywords.size}")

        // If we've used all keywords, reset and start over
        if (availableKeywords.isEmpty()) {
            Log.d(TAG, "All keywords used, resetting...")
            availableKeywords = currentKeywords.toMutableList()
            usedKeywords.clear()
        }

        // Get a random keyword that hasn't been used
        val available = availableKeywords.filter { it.word !in usedKeywords }
        val keyword = if (available.isNotEmpty()) {
            available.random()
        } else {
            // If all have been used, get any random one
            availableKeywords.random()
        }

        // Mark as used
        usedKeywords.add(keyword.word)
        availableKeywords.remove(keyword)
        Log.d(TAG, "Selected keyword: ${keyword.word}")

        return keyword
    }

    private fun generateWrongOptions(keyword: String): List<Pair<String, Int>> {
        Log.d(TAG, "Generating wrong options for: $keyword")
        val wrongOptions = mutableListOf<Pair<String, Int>>()
        val usedWrongOptions = mutableSetOf<String>()

        // Step 1: Get all possible wrong options from the same song
        val sameSongWrongOptions = mutableListOf<Pair<String, Int>>()

        // Filter current keywords for wrong options (excluding the correct keyword)
        for (kw in currentKeywords) {
            if (kw.word != keyword) {
                // Check if this is a water keyword when we already have one in the set
                val isWaterKeyword = waterKeywords.contains(kw.word)
                val currentWaterKeywordsInSet = wrongOptions.filter { waterKeywords.contains(it.first) }

                // If current keyword is a water keyword, only add if we don't already have a water keyword
                if (isWaterKeyword) {
                    if (currentWaterKeywordsInSet.isEmpty()) {
                        val imageRes = validatedKeywordImages[kw.word] ?: android.R.drawable.ic_dialog_info
                        sameSongWrongOptions.add(Pair(kw.word, imageRes))
                    }
                } else {
                    val imageRes = validatedKeywordImages[kw.word] ?: android.R.drawable.ic_dialog_info
                    sameSongWrongOptions.add(Pair(kw.word, imageRes))
                }
            }
        }

        // Shuffle and add up to 2 same-song wrong options (ensuring no duplicates)
        val shuffledSameSong = sameSongWrongOptions.shuffled()
        var addedCount = 0

        for (option in shuffledSameSong) {
            if (addedCount >= 2) break
            if (option.first !in usedWrongOptions) {
                wrongOptions.add(option)
                usedWrongOptions.add(option.first)
                addedCount++
            }
        }

        Log.d(TAG, "Added $addedCount same-song wrong options")

        // Step 2: If we need more options, add from unrelated categories using validated distractors
        if (wrongOptions.size < 3) {
            val neededCount = 3 - wrongOptions.size

            // Get unrelated distractor words from validated distractors
            val unrelatedDistractorWords = when {
                keyword == "boat" -> listOf("car", "plane", "bicycle", "train", "bus")
                keyword in listOf("star", "sun", "moon", "light") -> listOf("flower", "tree", "house", "mountain", "cloud")
                keyword in listOf("hill", "mountain") -> listOf("valley", "forest", "desert", "plain", "cave")
                keyword in listOf("water", "crown") -> listOf("hat", "helmet", "cap", "shield", "sword")
                waterKeywords.contains(keyword) -> {
                    // For water keywords, use other water-related but distinct terms
                    listOf("boat", "ocean", "lake", "waterfall", "pond", "sea")
                }
                else -> listOf("apple", "ball", "cat", "dog", "bird", "fish")
            }.filter { it in validatedDistractorImages } // Filter to only validated images

            var unrelatedAdded = 0
            for (unrelatedWord in unrelatedDistractorWords.shuffled()) {
                if (unrelatedAdded >= neededCount) break

                // Make sure it's not a duplicate and not the correct keyword
                if (unrelatedWord != keyword &&
                    unrelatedWord !in usedWrongOptions &&
                    !(waterKeywords.contains(keyword) && waterKeywords.contains(unrelatedWord))) {

                    val imageRes = validatedDistractorImages[unrelatedWord] ?: android.R.drawable.ic_dialog_info
                    wrongOptions.add(Pair(unrelatedWord, imageRes))
                    usedWrongOptions.add(unrelatedWord)
                    unrelatedAdded++
                }
            }

            Log.d(TAG, "Added $unrelatedAdded unrelated wrong options")
        }

        // Step 3: Final validation - ensure we have exactly 3 unique wrong options
        val validatedOptions = mutableListOf<Pair<String, Int>>()
        val finalSeen = mutableSetOf<String>()

        for (option in wrongOptions) {
            if (option.first != keyword &&
                option.first !in finalSeen &&
                !(waterKeywords.contains(keyword) && waterKeywords.contains(option.first))) {

                // Final validation of the image resource
                val finalResId = if (isDrawableValid(option.second)) {
                    option.second
                } else {
                    Log.w(TAG, "Image resource ${option.second} for '${option.first}' is invalid, using fallback")
                    android.R.drawable.ic_dialog_info
                }

                validatedOptions.add(Pair(option.first, finalResId))
                finalSeen.add(option.first)

                if (validatedOptions.size >= 3) break
            }
        }

        // Step 4: If we still don't have 3 options, add safe defaults
        while (validatedOptions.size < 3) {
            val safeDefaults = listOf(
                Pair("apple", android.R.drawable.ic_menu_help),
                Pair("ball", android.R.drawable.ic_menu_help),
                Pair("cat", android.R.drawable.ic_menu_help)
            )

            for (default in safeDefaults) {
                if (validatedOptions.size >= 3) break
                if (default.first != keyword && default.first !in finalSeen) {
                    validatedOptions.add(default)
                    finalSeen.add(default.first)
                }
            }
        }

        Log.d(TAG, "Final wrong options: ${validatedOptions.map { it.first }} with resources: ${validatedOptions.map { it.second }}")
        return validatedOptions.take(3)
    }

    private fun createGameRound(keyword: String, correctImageRes: Int, wrongOptions: List<Pair<String, Int>>): GameRound {
        Log.d(TAG, "Creating game round for keyword: $keyword")
        Log.d(TAG, "Correct image resource ID: $correctImageRes (valid: ${isDrawableValid(correctImageRes)})")

        // Step 1: Validate and deduplicate wrong options
        val validatedWrongOptions = mutableListOf<Pair<String, Int>>()
        val seenWords = mutableSetOf<String>()

        for ((index, option) in wrongOptions.withIndex()) {
            Log.d(TAG, "Wrong option $index: word='${option.first}', resId=${option.second}, valid=${isDrawableValid(option.second)}")

            if (option.first != keyword && option.first !in seenWords) {
                // Additional check for water keywords
                if (waterKeywords.contains(keyword) && waterKeywords.contains(option.first)) {
                    Log.d(TAG, "Skipping water keyword ${option.first} as ${keyword} is also a water keyword")
                    continue
                }

                // Validate the image resource
                val validResId = if (isDrawableValid(option.second)) {
                    option.second
                } else {
                    Log.w(TAG, "Invalid resource for '${option.first}', using fallback")
                    android.R.drawable.ic_menu_help
                }

                validatedWrongOptions.add(Pair(option.first, validResId))
                seenWords.add(option.first)

                if (validatedWrongOptions.size >= 3) break
            }
        }

        Log.d(TAG, "Validated wrong options: ${validatedWrongOptions.map { it.first }}")

        // Step 2: Ensure we have exactly 3 wrong options
        val finalWrongOptions = if (validatedWrongOptions.size >= 3) {
            validatedWrongOptions.take(3)
        } else {
            // Add safe defaults if needed
            val defaultOptions = listOf(
                Pair("apple", android.R.drawable.ic_menu_help),
                Pair("ball", android.R.drawable.ic_menu_help),
                Pair("cat", android.R.drawable.ic_menu_help)
            )

            val finalOptions = validatedWrongOptions.toMutableList()
            for (default in defaultOptions) {
                if (finalOptions.size >= 3) break
                if (default.first != keyword && default.first !in seenWords) {
                    finalOptions.add(default)
                    seenWords.add(default.first)
                }
            }
            finalOptions.take(3)
        }

        Log.d(TAG, "Final wrong options count: ${finalWrongOptions.size}")

        // Step 3: Validate correct image resource
        val validatedCorrectImageRes = if (isDrawableValid(correctImageRes)) {
            correctImageRes
        } else {
            Log.w(TAG, "Correct image resource $correctImageRes is invalid, using fallback")
            android.R.drawable.ic_dialog_info
        }

        // Step 4: Combine correct and wrong options, then shuffle
        val allOptions = mutableListOf(
            Pair(keyword, validatedCorrectImageRes)
        )
        allOptions.addAll(finalWrongOptions)

        // Shuffle the options
        val shuffledOptions = allOptions.shuffled()

        // Step 5: Find the index of correct answer after shuffling
        correctAnswerIndex = shuffledOptions.indexOfFirst { it.first == keyword }

        // Step 6: Final validation - ensure no duplicates in shuffled options
        val finalOptions = mutableListOf<Pair<String, Int>>()
        val finalSeen = mutableSetOf<String>()

        for (option in shuffledOptions) {
            if (option.first !in finalSeen) {
                // Final validation of each image
                val finalResId = if (isDrawableValid(option.second)) {
                    option.second
                } else {
                    android.R.drawable.ic_menu_help
                }
                finalOptions.add(Pair(option.first, finalResId))
                finalSeen.add(option.first)
            }
        }

        // Re-find correct answer index after deduplication
        correctAnswerIndex = finalOptions.indexOfFirst { it.first == keyword }

        Log.d(TAG, "Correct answer index: $correctAnswerIndex")
        Log.d(TAG, "Final options: ${finalOptions.map { "${it.first}(${it.second})" }}")

        return GameRound(keyword, validatedCorrectImageRes, finalOptions)
    }

    private fun displayOptions(gameRound: GameRound) {
        Log.d(TAG, "Displaying ${gameRound.options.size} options")
        val columnCount = 2
        val rowCount = 2
        val optionSize = resources.displayMetrics.widthPixels / 2 - 48.dpToPx()
        Log.d(TAG, "Option size: $optionSize")

        for (i in gameRound.options.indices) {
            val option = gameRound.options[i]
            Log.d(TAG, "Creating option $i: ${option.first} with resource ${option.second}")

            createOptionCard(i, option.first, option.second, optionSize, gameRound.keyword)
        }
        Log.d(TAG, "All options displayed")
    }

    private fun createOptionCard(index: Int, word: String, imageResId: Int, size: Int, correctKeyword: String) {
        // Create card view for option
        val card = CardView(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = size
                height = size
                columnSpec = GridLayout.spec(index % 2, 1f)
                rowSpec = GridLayout.spec(index / 2, 1f)
                setMargins(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
            }
            radius = 16.dpToPx().toFloat()
            cardElevation = 4.dpToPx().toFloat()
            isClickable = true
            tag = index

            // Set background color
            setCardBackgroundColor(Color.WHITE)

            setOnClickListener {
                Log.d(TAG, "Option $index clicked: $word")
                if (!isAnswerSelected) {
                    handleOptionClick(this, index)
                }
            }
        }

        // Create image view
        val imageView = ImageView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER

            // Set image with error handling
            try {
                val drawable = ContextCompat.getDrawable(this@RhythmSummaryActivity, imageResId)
                if (drawable != null) {
                    setImageResource(imageResId)
                    Log.d(TAG, "Successfully loaded image for $word: $imageResId")
                } else {
                    Log.e(TAG, "Drawable is null for resource: $imageResId")
                    setImageResource(android.R.drawable.ic_menu_help)
                }
            } catch (e: Resources.NotFoundException) {
                Log.e(TAG, "Resource not found for $word: $imageResId", e)
                setImageResource(android.R.drawable.ic_menu_help)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image for $word: $imageResId", e)
                setImageResource(android.R.drawable.ic_menu_help)
            }

            isClickable = false
            adjustViewBounds = true
            setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())

            // Post check to verify image loaded
            post {
                if (drawable == null) {
                    Log.e(TAG, "Image failed to load for: $word, resId: $imageResId")
                    // Try to load a different image
                    setImageResource(android.R.drawable.ic_dialog_alert)
                }
            }
        }

        // Add text label below image for debugging
        val textView = TextView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = word
            textSize = 12f
            setTextColor(Color.BLACK)
            gravity = android.view.Gravity.CENTER
            visibility = View.GONE // Hide in production, use for debugging
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            addView(imageView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addView(textView)
        }

        card.addView(container)
        optionsGrid.addView(card)

        // Add entrance animation
        card.alpha = 0f
        card.translationY = 100f
        card.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(index * 100L)
            .start()
    }

    private fun handleOptionClick(card: CardView, selectedIndex: Int) {
        Log.d(TAG, "Handling option click. Selected: $selectedIndex, Correct: $correctAnswerIndex")
        isAnswerSelected = true

        try {
            mediaPlayer.start()
            Log.d(TAG, "Button click sound played")
        } catch (e: Exception) {
            Log.w(TAG, "Could not play button sound: ${e.message}")
        }

        // Disable all cards
        for (i in 0 until optionsGrid.childCount) {
            val childCard = optionsGrid.getChildAt(i) as CardView
            childCard.isClickable = false
        }
        Log.d(TAG, "All cards disabled")

        if (selectedIndex == correctAnswerIndex) {
            Log.d(TAG, "Correct answer!")
            // Correct answer
            score++
            showFeedback(true, card)
            try {
                correctSound.start()
                Log.d(TAG, "Correct sound played")
            } catch (e: Exception) {
                Log.w(TAG, "Could not play correct sound: ${e.message}")
            }
        } else {
            Log.d(TAG, "Wrong answer!")
            // Wrong answer
            showFeedback(false, card)
            try {
                wrongSound.start()
                Log.d(TAG, "Wrong sound played")
            } catch (e: Exception) {
                Log.w(TAG, "Could not play wrong sound: ${e.message}")
            }

            // Highlight correct answer
            val correctCard = optionsGrid.getChildAt(correctAnswerIndex) as CardView
            correctCard.setCardBackgroundColor(Color.GREEN)
            correctCard.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(100)
                .start()
        }

        // Update score
        updateScore()
        Log.d(TAG, "Score updated: $score/$totalRounds")

        // Show feedback overlay for 2 seconds, then show next button
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Showing next button after feedback")
            hideFeedbackAndShowNextButton()
        }, 1000)
    }

    private fun showFeedback(isCorrect: Boolean, selectedCard: CardView) {
        Log.d(TAG, "Showing feedback. Is correct: $isCorrect")
        if (isCorrect) {
            selectedCard.setCardBackgroundColor(Color.GREEN)

            // Try to use custom drawable, fallback to system drawable
            try {
                feedbackIcon.setImageResource(R.drawable.correct_answer)
                Log.d(TAG, "Set correct answer icon")
            } catch (e: android.content.res.Resources.NotFoundException) {
                Log.w(TAG, "Custom correct icon not found, using system icon")
                feedbackIcon.setImageResource(android.R.drawable.ic_menu_report_image)
            }

            feedbackText.text = "Excellent!"
            feedbackText.setTextColor(Color.parseColor("#4CAF50"))

            // Celebration animation for correct answer
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

            // Try to use custom drawable, fallback to system drawable
            try {
                feedbackIcon.setImageResource(R.drawable.delete)
                Log.d(TAG, "Set wrong answer icon")
            } catch (e: android.content.res.Resources.NotFoundException) {
                Log.w(TAG, "Custom delete icon not found, using system icon")
                feedbackIcon.setImageResource(android.R.drawable.ic_delete)
            }

            feedbackText.text = "Wrong!"
            feedbackText.setTextColor(Color.parseColor("#F44336"))

            // Shake animation for wrong answer
            val shake = TranslateAnimation(0f, 20f, 0f, 0f)
            shake.duration = 50
            shake.repeatCount = 4
            shake.repeatMode = TranslateAnimation.REVERSE
            selectedCard.startAnimation(shake)
        }

        // Show feedback overlay with animation
        feedbackIcon.visibility = View.VISIBLE
        feedbackText.visibility = View.VISIBLE
        feedbackOverlay.visibility = View.VISIBLE
        Log.d(TAG, "Feedback overlay shown")

        // Animate the overlay appearance
        feedbackOverlay.alpha = 0f
        feedbackOverlay.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        // Animate the feedback container
        val feedbackContainer = feedbackOverlay.getChildAt(0) as LinearLayout
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
        Log.d(TAG, "Hiding feedback and showing next button")
        // Hide feedback overlay with animation
        feedbackOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                feedbackOverlay.visibility = View.GONE
                feedbackIcon.visibility = View.GONE
                feedbackText.visibility = View.GONE
                Log.d(TAG, "Feedback overlay hidden")
            }
            .start()

        // Show next button
        nextButton.visibility = View.VISIBLE
        nextButton.alpha = 0f
        nextButton.translationY = 50f
        nextButton.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .start()
        Log.d(TAG, "Next button shown")
    }

    private fun updateScore() {
        scoreText.text = "$score/$totalRounds"
        Log.d(TAG, "Score text updated: ${scoreText.text}")

        // Animate score update
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
        Log.d(TAG, "Updating progress dots. Current round: $currentRound")
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
        Log.d(TAG, "Next button clicked, moving to round ${currentRound + 1}")
        // Hide next button
        nextButton.visibility = View.GONE

        currentRound++
        startNewRound()
    }

    private fun navigateToScoreboard() {
        Log.d(TAG, "Navigating to scoreboard. Score: $score, Total rounds: $totalRounds")
        try {
            val intent = Intent(this, RMScoreboardActivity::class.java)
            intent.putExtra("SCORE", score)
            intent.putExtra("TOTAL_ROUNDS", totalRounds)
            intent.putExtra("SONG_TITLE", currentSongTitle)
            Log.d(TAG, "Starting RMScoreboardActivity")
            startActivity(intent)
            finish()
            Log.d(TAG, "RhythmSummaryActivity finished")
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to scoreboard: ${e.message}", e)
            Toast.makeText(this, "Error loading scoreboard: ${e.message}", Toast.LENGTH_LONG).show()
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
            Log.d(TAG, "Media players released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing media players: ${e.message}", e)
        }
    }
}