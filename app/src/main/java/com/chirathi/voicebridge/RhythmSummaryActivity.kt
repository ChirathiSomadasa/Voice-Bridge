package com.chirathi.voicebridge

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.animation.TranslateAnimation
import android.widget.*
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

class RhythmSummaryActivity : AppCompatActivity() {

    private lateinit var currentSongTitle: String

    // UI Components
    private lateinit var pandaImage: ImageView
    private lateinit var wordTitle: TextView
    private lateinit var scoreText: TextView
    private lateinit var progressContainer: LinearLayout
    private lateinit var optionsGrid: GridLayout
    private lateinit var feedbackContainer: LinearLayout
    private lateinit var feedbackIcon: ImageView
    private lateinit var feedbackText: TextView
    private lateinit var nextButton: Button

    private var currentRound = 0
    private var score = 0
    private var totalRounds = 5
    private var correctAnswerIndex = 0
    private var isAnswerSelected = false

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

    // Distractor images map
    private val distractorImages = mapOf(
        "car" to R.drawable.proud,
        "plane" to R.drawable.raincoat,
        "bicycle" to R.drawable.row,
        "ocean" to R.drawable.shoe,
        "lake" to R.drawable.sound,
        "waterfall" to R.drawable.snak,
        "moon" to R.drawable.speaker,
        "sun" to R.drawable.sorry,
        "planet" to R.drawable.spoon,
        "juice" to R.drawable.sticker,
        "milk" to R.drawable.stop,
        "soda" to R.drawable.thanks,
        "mountain" to R.drawable.tv,
        "valley" to R.drawable.water,
        "forest" to R.drawable.zoo,
        "hat" to R.drawable.shy,
        "helmet" to R.drawable.greedy,
        "cap" to R.drawable.proud,
        "apple" to R.drawable.angry,
        "ball" to R.drawable.sad,
        "cat" to R.drawable.happy
    )

    data class GameRound(val keyword: String, val correctImageRes: Int, val options: List<Pair<String, Int>>)

    private lateinit var currentKeywords: List<Keyword>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rythm_summary)

        // Initialize UI components
        initializeViews()

        // Get song title from intent
        currentSongTitle = intent.getStringExtra("SONG_TITLE") ?: "Row Row Row Your Boat"

        try {
            // Initialize sound effects
            mediaPlayer = MediaPlayer.create(this, R.raw.button_click)
            correctSound = MediaPlayer.create(this, R.raw.correct_sound)
            wrongSound = MediaPlayer.create(this, R.raw.wrong_sound)
        } catch (e: Exception) {
            // If sound files don't exist, create silent media players
            mediaPlayer = MediaPlayer()
            correctSound = MediaPlayer()
            wrongSound = MediaPlayer()
        }

        // Setup based on song
        setupSongData()
        setupUI()
        startNewRound()

        // Set next button click listener
        nextButton.setOnClickListener {
            onNextButtonClick()
        }
    }

    private fun initializeViews() {
        pandaImage = findViewById(R.id.pandaImage)
        wordTitle = findViewById(R.id.wordTitle)
        scoreText = findViewById(R.id.scoreText)
        progressContainer = findViewById(R.id.progressContainer)
        optionsGrid = findViewById(R.id.optionsGrid)
        feedbackContainer = findViewById(R.id.feedbackContainer)
        feedbackIcon = findViewById(R.id.feedbackIcon)
        feedbackText = findViewById(R.id.feedbackText)
        nextButton = findViewById(R.id.nextButton)
    }

    private fun setupSongData() {
        // Get keywords from the song that was played
        currentKeywords = when (currentSongTitle) {
            "Row Row Row Your Boat" -> {
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
                listOf(
                    Keyword("hill", R.drawable.hill_image, 10500, 11000),
                    Keyword("water", R.drawable.water_image, 12500, 14000),
                    Keyword("crown", R.drawable.crown_image, 14500, 16000)
                )
            }
            else -> {
                listOf(
                    Keyword("boat", R.drawable.boat_image, 11000, 12000),
                    Keyword("stream", R.drawable.stream_image, 13000, 15000),
                    Keyword("dream", R.drawable.dream_image, 18000, 20000)
                )
            }
        }
    }

    private fun setupUI() {
        // Setup panda animation
        animatePanda()

        // Setup progress dots
        setupProgressDots()

        // Update score display
        updateScore()
    }

    private fun animatePanda() {
        val bounceAnimator = ObjectAnimator.ofFloat(pandaImage, "translationY", 0f, -20f, 0f)
        bounceAnimator.duration = 1000
        bounceAnimator.repeatCount = ObjectAnimator.INFINITE
        bounceAnimator.repeatMode = ObjectAnimator.REVERSE
        bounceAnimator.start()
    }

    private fun setupProgressDots() {
        progressContainer.removeAllViews()
        for (i in 0 until totalRounds) {
            val dot = View(this)
            val size = 16.dpToPx() // Use dpToPx function instead of dimens
            val params = LinearLayout.LayoutParams(size, size)
            params.marginEnd = if (i < totalRounds - 1) 8.dpToPx() else 0
            dot.layoutParams = params

            // Use solid colors instead of drawables for now
            dot.setBackgroundColor(
                when {
                    i == currentRound -> Color.parseColor("#4CAF50") // Green for active
                    i < currentRound -> Color.parseColor("#FF6B35") // Orange for completed
                    else -> Color.parseColor("#BDBDBD") // Grey for inactive
                }
            )
            dot.background.setAlpha(200)
            progressContainer.addView(dot)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun startNewRound() {
        if (currentRound >= totalRounds) {
            // Game finished, navigate to scoreboard
            navigateToScoreboard()
            return
        }

        isAnswerSelected = false
        feedbackContainer.visibility = View.GONE
        nextButton.visibility = View.GONE

        // Clear previous options
        optionsGrid.removeAllViews()

        // Get random keyword for this round
        val keyword = currentKeywords.random()
        val correctImageRes = keywordImages[keyword.word] ?: R.drawable.ic_placeholder

        // Generate wrong options
        val wrongOptions = generateWrongOptions(keyword.word)

        // Create game round
        val gameRound = createGameRound(keyword.word, correctImageRes, wrongOptions)

        // Display the word to find
        wordTitle.text = "Find: ${keyword.word.uppercase()}"

        // Create and display options
        displayOptions(gameRound)

        // Update progress dots
        updateProgressDots()
    }

    private fun generateWrongOptions(keyword: String): List<Pair<String, Int>> {
        val wrongOptions = mutableListOf<Pair<String, Int>>()

        // Get distractors - use common distractors for now
        val distractors = when (keyword) {
            "boat" -> listOf("car", "plane", "bicycle")
            "stream" -> listOf("ocean", "lake", "waterfall")
            "star" -> listOf("moon", "sun", "planet")
            "water" -> listOf("juice", "milk", "soda")
            "hill" -> listOf("mountain", "valley", "forest")
            "crown" -> listOf("hat", "helmet", "cap")
            else -> listOf("apple", "ball", "cat")
        }

        // Convert distractors to image resources
        distractors.forEach { distractor ->
            val imageRes = distractorImages[distractor] ?: R.drawable.ic_placeholder
            wrongOptions.add(Pair(distractor, imageRes))
        }

        return wrongOptions
    }

    private fun createGameRound(keyword: String, correctImageRes: Int, wrongOptions: List<Pair<String, Int>>): GameRound {
        // Combine correct and wrong options, then shuffle
        val allOptions = mutableListOf(
            Pair(keyword, correctImageRes)
        )
        allOptions.addAll(wrongOptions.take(3)) // Take only 3 wrong options

        // Shuffle the options
        val shuffledOptions = allOptions.shuffled()

        // Find the index of correct answer after shuffling
        correctAnswerIndex = shuffledOptions.indexOfFirst { it.first == keyword }

        return GameRound(keyword, correctImageRes, shuffledOptions)
    }

    private fun displayOptions(gameRound: GameRound) {
        val columnCount = 2
        val rowCount = 2
        val optionSize = resources.displayMetrics.widthPixels / 2 - 48.dpToPx()

        for (i in gameRound.options.indices) {
            val option = gameRound.options[i]

            // Create card view for option
            val card = CardView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = optionSize
                    height = optionSize
                    columnSpec = GridLayout.spec(i % columnCount, 1f)
                    rowSpec = GridLayout.spec(i / columnCount, 1f)
                    setMargins(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                }
                radius = 16.dpToPx().toFloat()
                cardElevation = 4.dpToPx().toFloat()
                isClickable = true
                tag = i

                setOnClickListener {
                    if (!isAnswerSelected) {
                        handleOptionClick(this, i)
                    }
                }
            }

            // Create image view inside card

            val imageView = ImageView(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(option.second)
                isClickable = false
            }

            // Add image to card and card to grid
            card.addView(imageView)
            optionsGrid.addView(card)

            // Add entrance animation
            card.alpha = 0f
            card.translationY = 100f
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(i * 100L)
                .start()
        }
    }

    private fun handleOptionClick(card: CardView, selectedIndex: Int) {
        isAnswerSelected = true

        try {
            mediaPlayer.start()
        } catch (e: Exception) {
            // Ignore if sound fails
        }

        // Disable all cards
        for (i in 0 until optionsGrid.childCount) {
            val childCard = optionsGrid.getChildAt(i) as CardView
            childCard.isClickable = false
        }

        if (selectedIndex == correctAnswerIndex) {
            // Correct answer
            score++
            showFeedback(true, card)
            try {
                correctSound.start()
            } catch (e: Exception) {
                // Ignore if sound fails
            }
        } else {
            // Wrong answer
            showFeedback(false, card)
            try {
                wrongSound.start()
            } catch (e: Exception) {
                // Ignore if sound fails
            }

            // Highlight correct answer
            val correctCard = optionsGrid.getChildAt(correctAnswerIndex) as CardView
            correctCard.setCardBackgroundColor(Color.GREEN)
            correctCard.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(300)
                .start()
        }

        // Update score
        updateScore()

        // Show next button after delay
        Handler(Looper.getMainLooper()).postDelayed({
            nextButton.visibility = View.VISIBLE
            nextButton.alpha = 0f
            nextButton.translationY = 50f
            nextButton.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start()
        }, 1500)
    }

    private fun showFeedback(isCorrect: Boolean, selectedCard: CardView) {
        if (isCorrect) {
            selectedCard.setCardBackgroundColor(Color.GREEN)
            feedbackIcon.setImageResource(android.R.drawable.ic_menu_report_image) // Use system icon for now
            feedbackText.text = "Excellent!"
            feedbackText.setTextColor(Color.GREEN)

            // Celebration animation for correct answer
            selectedCard.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(300)
                .withEndAction {
                    selectedCard.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .start()
                }
                .start()
        } else {
            selectedCard.setCardBackgroundColor(Color.RED)
            feedbackIcon.setImageResource(android.R.drawable.ic_delete) // Use system icon for now
            feedbackText.text = "Try Again!"
            feedbackText.setTextColor(Color.RED)

            // Shake animation for wrong answer
            val shake = TranslateAnimation(0f, 20f, 0f, 0f)
            shake.duration = 50
            shake.repeatCount = 4
            shake.repeatMode = TranslateAnimation.REVERSE
            selectedCard.startAnimation(shake)
        }

        // Show feedback with animation
        feedbackIcon.visibility = View.VISIBLE
        feedbackText.visibility = View.VISIBLE
        feedbackContainer.visibility = View.VISIBLE

        feedbackContainer.alpha = 0f
        feedbackContainer.translationY = 50f
        feedbackContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    private fun updateScore() {
        scoreText.text = "$score/$totalRounds"

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
        currentRound++
        startNewRound()
    }

    private fun navigateToScoreboard() {
        val intent = Intent(this, RMScoreboardActivity::class.java)
        intent.putExtra("SCORE", score)
        intent.putExtra("TOTAL_ROUNDS", totalRounds)
        intent.putExtra("SONG_TITLE", currentSongTitle)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        correctSound.release()
        wrongSound.release()
    }
}