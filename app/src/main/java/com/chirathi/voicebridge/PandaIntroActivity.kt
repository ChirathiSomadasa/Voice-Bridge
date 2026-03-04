package com.chirathi.voicebridge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class PandaIntroActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var skipButton: TextView
    private lateinit var adventureText: TextView
    private lateinit var sparkle1: ImageView
    private lateinit var sparkle2: ImageView
    private lateinit var sparkle3: ImageView
    private lateinit var butterfly1: ImageView
    private lateinit var butterfly2: ImageView

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private val handler = Handler(Looper.getMainLooper())

    // ── TTS ───────────────────────────────────────────────────────────────────
    private var tts: android.speech.tts.TextToSpeech? = null
    private var isTtsReady = false
    private var userName = "friend"

    // ── FIX: cache age from first Firestore load so navigation is instant ─────
    private var cachedAge: Int = 6
    private var ageLoaded: Boolean = false

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panda_intro)

        auth = FirebaseAuth.getInstance()

        videoView     = findViewById(R.id.videoView)
        skipButton    = findViewById(R.id.skipButton)
        adventureText = findViewById(R.id.adventureText)
        sparkle1      = findViewById(R.id.sparkle1)
        sparkle2      = findViewById(R.id.sparkle2)
        sparkle3      = findViewById(R.id.sparkle3)
        butterfly1    = findViewById(R.id.butterfly1)
        butterfly2    = findViewById(R.id.butterfly2)

        val uri = Uri.parse("android.resource://$packageName/${R.raw.game_intro}")
        videoView.setVideoURI(uri)

        // ── FIX 1: init TTS FIRST before video starts, no extra delay ─────────
        initTts()

        // Load user name + age in one Firestore call
        loadUserData()

        videoView.start()
        startSparkleAndButterflyAnimations()

        skipButton.setOnClickListener {
            navigateToMoodMatch()
        }

        // ── FIX 2: use cached age on completion — no second Firestore call ─────
        videoView.setOnCompletionListener {
            navigateToMoodMatch()
        }
    }

    // =========================================================================
    // TTS — speaks immediately when engine ready, no extra Handler delay
    // =========================================================================

    private fun initTts() {
        tts = android.speech.tts.TextToSpeech(this) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts?.language    = java.util.Locale.US
                tts?.setPitch(1.4f)
                tts?.setSpeechRate(0.80f)
                isTtsReady = true
                startVoiceSequence()   // no extra delay
            }
        }
    }

    /**
     * Voice sequence — total ~9 seconds.
     *
     *  0ms    → "Hi"
     *  1500ms → "I am Emo"
     *  3200ms → "I am so happy to see you"
     *  5200ms → (child's name)
     *  6500ms → "Can you help me guess the emotion?"
     *  8200ms → "We will have so much fun!"
     */
    private fun startVoiceSequence() {
        if (!isTtsReady) return

        speakPhrase("Hi", "hi")
        handler.postDelayed({ speakPhrase("I am so happy to see you", "happy")          }, 600)
        handler.postDelayed({ speakPhrase(userName, "username")                         }, 1000)
        handler.postDelayed({ speakPhrase("Can you help me guess the emotion?", "help") }, 2000)
        handler.postDelayed({ speakPhrase("We will have so much fun!", "fun")           }, 4200)
    }

    private fun speakPhrase(text: String, utteranceId: String) {
        tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_ADD, null, utteranceId)
    }

    // =========================================================================
    // Firestore — loads name AND age in one call, caches both
    // =========================================================================

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: run {
            adventureText.text = "Your\nAdventure"
            return
        }

        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Name
                    val firstName = document.getString("firstName")
                    if (!firstName.isNullOrEmpty()) {
                        adventureText.text = "$firstName's\nAdventure"
                        userName = firstName
                    } else {
                        adventureText.text = "Your\nAdventure"
                    }

                    // Age — cached so navigation needs no second network call
                    val ageString = document.getString("age")
                    if (ageString != null) {
                        try {
                            cachedAge = ageString.toInt()
                            ageLoaded = true
                        } catch (_: NumberFormatException) {
                            ageLoaded = false
                        }
                    }
                } else {
                    adventureText.text = "Your\nAdventure"
                }
            }
            .addOnFailureListener {
                adventureText.text = "Your\nAdventure"
            }
    }

    // =========================================================================
    // Navigation — instant, uses cachedAge, no Firestore call
    // =========================================================================

    private fun navigateToMoodMatch() {
        videoView.stopPlayback()
        stopAllAnimations()

        startActivity(Intent(this, MoodMatchSevenDownActivity::class.java).apply {
            putExtra("AGE_GROUP", cachedAge)
        })
        finish()
    }

    // =========================================================================
    // Sparkle & butterfly animations
    // =========================================================================

    private fun startSparkleAndButterflyAnimations() {
        handler.postDelayed({
            sparkle1.visibility = View.VISIBLE
            sparkle2.visibility = View.VISIBLE
            sparkle3.visibility = View.VISIBLE

            (sparkle1.drawable as? android.graphics.drawable.AnimationDrawable)?.start()
            (sparkle2.drawable as? android.graphics.drawable.AnimationDrawable)?.start()
            (sparkle3.drawable as? android.graphics.drawable.AnimationDrawable)?.start()

            startFloatingAnimation(sparkle1, 2000, -30f)
            startFloatingAnimation(sparkle2, 2500, -40f)
            startFloatingAnimation(sparkle3, 1800, -25f)
        }, 500)

        handler.postDelayed({
            butterfly1.visibility = View.VISIBLE
            butterfly2.visibility = View.VISIBLE

            (butterfly1.drawable as? android.graphics.drawable.AnimationDrawable)?.start()
            (butterfly2.drawable as? android.graphics.drawable.AnimationDrawable)?.start()

            startButterflyAnimation(butterfly1, true)
            startButterflyAnimation(butterfly2, false)
        }, 1000)
    }

    private fun startFloatingAnimation(view: View, duration: Long, floatAmount: Float) {
        val floatAnim = TranslateAnimation(0f, 0f, 0f, floatAmount).apply {
            this.duration  = duration
            repeatCount    = Animation.INFINITE
            repeatMode     = Animation.REVERSE
            interpolator   = AccelerateDecelerateInterpolator()
        }
        val rotateAnim = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            this.duration = duration * 2
            repeatCount   = Animation.INFINITE
        }
        AnimationSet(true).apply {
            addAnimation(floatAnim)
            addAnimation(rotateAnim)
            view.startAnimation(this)
        }
    }

    private fun startButterflyAnimation(view: View, fromRight: Boolean) {
        val screenWidth = resources.displayMetrics.widthPixels
        val startX = if (fromRight) screenWidth.toFloat() else -view.width.toFloat()
        val endX   = if (fromRight) -view.width.toFloat() else screenWidth.toFloat()

        val flyAnim = TranslateAnimation(
            startX, endX,
            0f, (Math.random() * 200 - 100).toFloat()
        ).apply {
            duration     = 4000 + (Math.random() * 2000).toLong()
            repeatCount  = Animation.INFINITE
            interpolator = LinearInterpolator()
        }
        val flapAnim = RotateAnimation(
            -10f, 10f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration    = 300
            repeatCount = Animation.INFINITE
            repeatMode  = Animation.REVERSE
        }
        AnimationSet(true).apply {
            addAnimation(flyAnim)
            addAnimation(flapAnim)
            view.startAnimation(this)
        }
    }

    // =========================================================================
    // Stop everything cleanly
    // =========================================================================

    private fun stopAllAnimations() {
        handler.removeCallbacksAndMessages(null)
        tts?.stop()

        (sparkle1.drawable   as? android.graphics.drawable.AnimationDrawable)?.stop()
        (sparkle2.drawable   as? android.graphics.drawable.AnimationDrawable)?.stop()
        (sparkle3.drawable   as? android.graphics.drawable.AnimationDrawable)?.stop()
        (butterfly1.drawable as? android.graphics.drawable.AnimationDrawable)?.stop()
        (butterfly2.drawable as? android.graphics.drawable.AnimationDrawable)?.stop()

        sparkle1.clearAnimation()
        sparkle2.clearAnimation()
        sparkle3.clearAnimation()
        butterfly1.clearAnimation()
        butterfly2.clearAnimation()
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override fun onResume() {
        super.onResume()
        if (!videoView.isPlaying) videoView.start()
    }

    override fun onPause() {
        super.onPause()
        videoView.pause()
        stopAllAnimations()
    }

    override fun onDestroy() {
        super.onDestroy()
        videoView.stopPlayback()
        tts?.stop()
        tts?.shutdown()
        handler.removeCallbacksAndMessages(null)
    }
}