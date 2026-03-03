package com.chirathi.voicebridge

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

/**
 * AllCorrectGrandPrizeActivity — Therapeutic Virtual Friend Reveal
 *
 * ══ PURPOSE ════════════════════════════════════════════════════════════════
 * This is NOT a performance reward screen.
 * It is a THERAPEUTIC RESPONSE screen — the virtual friend unlocked here is
 * chosen by the ML model based on the child's emotional and behavioural state
 * during the session, specifically the model's `friendAction` output.
 *
 * ══ FRIEND MAP (from Python label generator) ═══════════════════════════════
 *
 *  friendAction = 1, 2, or 5  ← HIGH FRUSTRATION was detected
 *  ─────────────────────────────────────────────────────────────────────────
 *  Model rule: frustration > 0.7 → random choice of [1, 2, 5]
 *
 *  These three all deliver comfort but with different flavours:
 *
 *    1 → Blanket Panda  "I see you worked really hard. Come, let's rest."
 *        Drawable: panda_blanket  (panda wrapped in a cosy blanket)
 *        Tone: warm, quiet, de-escalating
 *        Verse: a short calming breathing cue + reassurance
 *
 *    2 → Hug Panda      "You never gave up. That's the bravest thing."
 *        Drawable: panda_hug  (panda with arms open wide)
 *        Tone: warm, celebratory of persistence
 *        Verse: emphasises effort over outcome
 *
 *    5 → Star Panda     "Every try makes you stronger. You're my star."
 *        Drawable: panda_star  (panda holding a glowing star)
 *        Tone: motivational, identity-building
 *        Verse: reframes struggle as growth
 *
 *  friendAction = 9            ← HIGH PERFORMANCE detected
 *  ─────────────────────────────────────────────────────────────────────────
 *  Model rule: alpha > 8.5 (accuracy × speed × age factor all high)
 *
 *    9 → Trophy Panda   "You were AMAZING today. A true champion!"
 *        Drawable: panda_trophy  (panda holding a golden trophy)
 *        Tone: celebratory, energetic
 *        Verse: affirms excellence and speed
 *
 *  default (0 / unknown — should not normally reach this screen)
 *  ─────────────────────────────────────────────────────────────────────────
 *    0 → Happy Panda    general celebration fallback
 *        Drawable: panda_happy
 *
 * ══ DRAWABLES NEEDED ═══════════════════════════════════════════════════════
 *   res/drawable/panda_blanket.png   ← most important, for frustration comfort
 *   res/drawable/panda_hug.png
 *   res/drawable/panda_star.png
 *   res/drawable/panda_trophy.png
 *   res/drawable/panda_happy.png
 *   All fall back to panda_confused if missing — no crash.
 */
class AllCorrectGrandPrizeActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // ── Views ─────────────────────────────────────────────────────────────────
    private lateinit var feedbackImage:  ImageView
    private lateinit var friendName:     TextView   // big friendly name e.g. "Blanket Panda"
    private lateinit var verseText:      TextView   // the calming / celebratory verse
    private lateinit var btnOk:          Button
    private lateinit var mainLayout:     ConstraintLayout
    private lateinit var giftBoxIcon:    ImageView

    // ── TTS ───────────────────────────────────────────────────────────────────
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false
    private var pendingSpeak: String? = null

    // ── Friend data (resolved from intent) ────────────────────────────────────
    private var friendAction = 0
    private var ageGroup     = 6
    private lateinit var friend: TherapeuticFriend

    companion object {
        private const val TAG = "GrandPrize"
    }

    // =========================================================================
    // Therapeutic Friend descriptor
    // =========================================================================

    /**
     * Everything needed to display one specific therapeutic friend.
     *
     * @param drawableRes   Image of the friend character
     * @param name          Name shown in the large headline, e.g. "Blanket Panda"
     * @param verse         The therapeutic message shown below the image.
     *                      Short, warm, and age-appropriate.
     *                      For frustration friends: calming, reassuring language.
     *                      For high-performance friend: celebratory, energetic.
     * @param spokenVerse   TTS version (may differ slightly — no emojis, better rhythm)
     * @param okLabel       Dismiss button text
     * @param animStyle     Controls the reveal animation style
     */
    data class TherapeuticFriend(
        val drawableRes:  Int,
        val name:         String,
        val verse:        String,
        val spokenVerse:  String,
        val okLabel:      String,
        val animStyle:    AnimStyle
    )

    enum class AnimStyle {
        SOFT,       // slow, gentle reveal — for frustration/comfort friends
        ENERGETIC   // bouncy, fast reveal — for high-performance friend
    }

    // =========================================================================
    // onCreate
    // =========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_all_correct_grand_prize)

        friendAction = intent.getIntExtra("FRIEND_ACTION", 0)
        ageGroup     = intent.getIntExtra("AGE_GROUP", 6)

        Log.d(TAG, "friendAction=$friendAction ageGroup=$ageGroup")

        friend = resolveFriend(friendAction, ageGroup)
        Log.d(TAG, "Resolved friend: ${friend.name}")

        tts = TextToSpeech(this, this)

        // Bind views — your layout must have these IDs
        feedbackImage = findViewById(R.id.feedbackImage)
        friendName    = findViewById(R.id.title)
        verseText     = findViewById(R.id.unlockedText)
        btnOk         = findViewById(R.id.btn_ok)
        mainLayout    = findViewById(R.id.main)

        // Apply friend content
        feedbackImage.setImageResource(friend.drawableRes)
        friendName.text = friend.name
        verseText.text  = friend.verse
        btnOk.text      = friend.okLabel

        // Hide until animation reveals them
        feedbackImage.visibility = View.GONE
        friendName.visibility    = View.GONE
        verseText.visibility     = View.GONE
        btnOk.visibility         = View.GONE

        // Build animated gift box overlay
        giftBoxIcon = ImageView(this)
        giftBoxIcon.setImageResource(R.drawable.giftbox)
        giftBoxIcon.layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topToTop       = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            startToStart   = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd       = ConstraintLayout.LayoutParams.PARENT_ID
        }
        mainLayout.addView(giftBoxIcon)

        mainLayout.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mainLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    startGiftAnimation()
                }
            }
        )

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }
    }

    // =========================================================================
    // Friend resolution — maps model's friendAction to therapeutic content
    // =========================================================================

    private fun resolveFriend(action: Int, age: Int): TherapeuticFriend {
        val isYoung = age <= 7  // younger children get simpler, shorter language

        fun drawable(name: String): Int {
            val id = resources.getIdentifier(name, "drawable", packageName)
            return if (id != 0) id else R.drawable.panda_confused
        }

        return when (action) {

            // ── 1: BLANKET PANDA ──────────────────────────────────────────────
            // Triggered: frustration > 0.7 (model assigned action=1)
            // Therapy goal: de-escalation, physical calm, reassurance that
            //               struggling is normal and safe.
            // Visual: panda wrapped snugly in a soft blanket, gentle eyes
            1 -> TherapeuticFriend(
                drawableRes = drawable("panda_blanket"),
                name = if (isYoung) "Mochi" else "Mochi",
                verse       = if (isYoung)
                    "\"Take a big breath with me… in… and out.\n\n" +
                            "I could see how hard you were trying. That's really brave.\n\n" +
                            "You don't have to be perfect. I'm here, and I'm proud of you.\""
                else
                    "\"Let's breathe together for a moment. In… and slowly out.\n\n" +
                            "I noticed how much effort you put in today, even when it felt tough.\n\n" +
                            "Feeling frustrated just means you care. That matters. You did well.\"",
                spokenVerse = if (isYoung)
                    "Take a big breath with me. In. And out. I could see how hard you were trying. " +
                            "That is really brave. You don't have to be perfect. I'm here, and I'm proud of you."
                else
                    "Let's breathe together for a moment. In. And slowly out. " +
                            "I noticed how much effort you put in today, even when it felt tough. " +
                            "Feeling frustrated just means you care. That matters. You did well.",
                okLabel     = "Thank you, Mochi",
                animStyle   = AnimStyle.SOFT
            )

            // ── 2: HUG PANDA ─────────────────────────────────────────────────
            // Triggered: frustration > 0.7 (model assigned action=2)
            // Therapy goal: validate persistence, reframe outcome vs effort,
            //               warm physical-affection metaphor.
            // Visual: panda with arms wide open, warm smile
            2 -> TherapeuticFriend(
                drawableRes = drawable("panda_hug"),
                name        = if (isYoung) "Senu the Snuggle Panda" else "Senu",
                verse       = if (isYoung)
                    "\"You kept going, even when it was hard!\n\n" +
                            "That's the bravest thing you can do.\n\n" +
                            "Come here — this hug is just for you, because you never gave up!\""
                else
                    "\"You kept going, even when things got difficult.\n\n" +
                            "A lot of people give up when it's hard — you didn't. That's real courage.\n\n" +
                            "This hug is for you, because trying hard matters more than getting it right.\"",
                spokenVerse = if (isYoung)
                    "You kept going, even when it was hard! That is the bravest thing you can do. " +
                            "This hug is just for you, because you never gave up!"
                else
                    "You kept going, even when things got difficult. " +
                            "A lot of people give up when it is hard — you didn't. That is real courage. " +
                            "This hug is for you, because trying hard matters more than getting it right.",
                okLabel     = "I love hugs!",
                animStyle   = AnimStyle.SOFT
            )

            // ── 5: STAR PANDA ─────────────────────────────────────────────────
            // Triggered: frustration > 0.7 (model assigned action=5)
            // Therapy goal: identity-building, reframe struggle as growth,
            //               motivate without invalidating the difficulty.
            // Visual: panda holding a glowing star, confident pose
            5 -> TherapeuticFriend(
                drawableRes = drawable("panda_star"),
                name        = if (isYoung) "Lumi" else "Lumi",
                verse       = if (isYoung)
                    "\"Do you know what I see? A star.\n\n" +
                            "Every time you try — even the tricky tries — you grow a little bigger.\n\n" +
                            "You worked SO hard today. You're my star!\""
                else
                    "\"Every attempt you made today — the right ones and the tricky ones — " +
                            "all of them made your brain stronger.\n\n" +
                            "That's what stars are made of: effort, not just easy wins.\n\n" +
                            "You earned this. Keep shining.\"",
                spokenVerse = if (isYoung)
                    "Do you know what I see? A star. Every time you try, even the tricky tries, " +
                            "you grow a little bigger. You worked so hard today. You are my star!"
                else
                    "Every attempt you made today, the right ones and the tricky ones, " +
                            "all of them made your brain stronger. That is what stars are made of: effort, not just easy wins. " +
                            "You earned this. Keep shining.",
                okLabel     = "I'm a star!",
                animStyle   = AnimStyle.SOFT
            )

            // ── 9: TROPHY PANDA ──────────────────────────────────────────────
            // Triggered: alpha > 8.5 (high accuracy + fast responses + age factor)
            // Therapy goal: celebrate excellence, reinforce positive identity,
            //               energise for next session.
            // Visual: panda proudly holding a golden trophy, big smile
            9 -> TherapeuticFriend(
                drawableRes = drawable("panda_trophy"),
                name        = if (isYoung) "Tutu" else "Tutu",
                verse       = if (isYoung)
                    "\"WOW! You were absolutely AMAZING today!\n\n" +
                            "You answered fast and you got it right — that's superstar stuff!\n\n" +
                            "This trophy is YOURS. You totally earned it!\""
                else
                    "\"That was an outstanding performance today!\n\n" +
                            "Your speed and accuracy were at champion level — not many players reach that.\n\n" +
                            "This trophy is yours. Own it — you completely earned it.\"",
                spokenVerse = if (isYoung)
                    "Wow! You were absolutely amazing today! You answered fast and you got it right. " +
                            "That is superstar stuff! This trophy is yours. You totally earned it!"
                else
                    "That was an outstanding performance today! " +
                            "Your speed and accuracy were at champion level. Not many players reach that. " +
                            "This trophy is yours. Own it. You completely earned it.",
                okLabel     = "I'm a champion!",
                animStyle   = AnimStyle.ENERGETIC
            )

            // ── Default / Happy Panda (fallback) ──────────────────────────────
            // Should not normally appear since this screen only opens when
            // UNLOCK_GIFT = true (friendAction > 0). Acts as a safety net.
            else -> TherapeuticFriend(
                drawableRes = drawable("panda_happy"),
                name        = if (isYoung) "Pinky" else "Pinky",
                verse       = if (isYoung)
                    "\"You did it! I'm SO happy for you!\n\n" +
                            "You played today and that makes me so proud.\n\n" +
                            "See you next time, friend!\""
                else
                    "\"Great session today!\n\n" +
                            "You showed up, you tried, and that always counts.\n\n" +
                            "Your Happy Panda is proud of you — see you next time!\"",
                spokenVerse = if (isYoung)
                    "You did it! I am so happy for you! You played today and that makes me so proud. " +
                            "See you next time, friend!"
                else
                    "Great session today! You showed up, you tried, and that always counts. " +
                            "Your Happy Panda is proud of you. See you next time!",
                okLabel     = "Bye, Panda!",
                animStyle   = AnimStyle.SOFT
            )
        }
    }

    // =========================================================================
    // Animation — style adapts to the friend's emotional tone
    // =========================================================================

    private fun startGiftAnimation() {
        giftBoxIcon.x = (mainLayout.width  - giftBoxIcon.width)  / 2f
        giftBoxIcon.y = (mainLayout.height - giftBoxIcon.height) / 2f

        when (friend.animStyle) {
            AnimStyle.SOFT      -> startSoftGiftAnimation()
            AnimStyle.ENERGETIC -> startEnergeticGiftAnimation()
        }
    }

    /**
     * SOFT animation — for frustration-triggered comfort friends (actions 1, 2, 5).
     * Slow shake → gentle glow expand → slow panda drift in from below.
     * Deliberately calming: slow easing, no sudden movements.
     */
    private fun startSoftGiftAnimation() {
        // Gentle slow rock (not a sharp shake)
        val rockAnim = ObjectAnimator.ofFloat(giftBoxIcon, "rotation", -6f, 6f, -4f, 4f, 0f)
        rockAnim.duration     = 1400
        rockAnim.repeatCount  = 1
        rockAnim.interpolator = LinearInterpolator()

        // Soft expand and fade out
        val expandAnim = ObjectAnimator.ofPropertyValuesHolder(
            giftBoxIcon,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.3f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.3f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("alpha",  1f, 0.8f, 0f)
        )
        expandAnim.duration     = 1000
        expandAnim.interpolator = DecelerateInterpolator()
        expandAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mainLayout.removeView(giftBoxIcon)
                revealFriendSoft()
            }
        })

        rockAnim.start()
        Handler(Looper.getMainLooper()).postDelayed({ expandAnim.start() }, 2900)
    }

    /**
     * ENERGETIC animation — for high-performance Trophy Panda (action 9).
     * Fast sharp shake → big explosive expand → bouncy panda pop-in.
     */
    private fun startEnergeticGiftAnimation() {
        val shakeAnim = ObjectAnimator.ofFloat(
            giftBoxIcon, "rotation",
            -12f, 12f, -12f, 12f, -8f, 8f, 0f
        )
        shakeAnim.duration     = 900
        shakeAnim.repeatCount  = 2
        shakeAnim.interpolator = LinearInterpolator()

        val expandAnim = ObjectAnimator.ofPropertyValuesHolder(
            giftBoxIcon,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 2f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 2f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("alpha",  1f, 1f, 0f)
        )
        expandAnim.duration     = 700
        expandAnim.interpolator = AccelerateInterpolator()
        expandAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mainLayout.removeView(giftBoxIcon)
                revealFriendEnergetic()
            }
        })

        shakeAnim.start()
        Handler(Looper.getMainLooper()).postDelayed({ expandAnim.start() }, 2000)
    }

    // ── SOFT reveal (comfort friends) ─────────────────────────────────────────

    private fun revealFriendSoft() {
        // Panda drifts in gently from below
        feedbackImage.visibility    = View.VISIBLE
        feedbackImage.alpha         = 0f
        feedbackImage.translationY  = 80f
        feedbackImage.scaleX        = 0.85f
        feedbackImage.scaleY        = 0.85f
        feedbackImage.animate()
            .alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
            .setDuration(900)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Gentle sway after settling
        Handler(Looper.getMainLooper()).postDelayed({
            ObjectAnimator.ofFloat(feedbackImage, "rotation", -3f, 3f, -2f, 2f, 0f).apply {
                duration     = 800
                interpolator = LinearInterpolator()
                start()
            }
        }, 1000)

        // Friend name fades in
        Handler(Looper.getMainLooper()).postDelayed({ fadeIn(friendName, 700) }, 500)

        // Verse fades in — give it more time so the child settles before reading
        Handler(Looper.getMainLooper()).postDelayed({ fadeIn(verseText, 800) }, 1100)

        // OK button soft fade-in (not a bounce — staying calm)
        Handler(Looper.getMainLooper()).postDelayed({
            fadeIn(btnOk, 600)
            btnOk.setOnClickListener { animateDismiss() }
        }, 1900)

        // Speak the verse after a calm pause
        Handler(Looper.getMainLooper()).postDelayed({
            speakText(friend.spokenVerse)
        }, 1500)
    }

    // ── ENERGETIC reveal (Trophy Panda) ───────────────────────────────────────

    private fun revealFriendEnergetic() {
        // Big bounce pop-in
        feedbackImage.visibility = View.VISIBLE
        feedbackImage.scaleX     = 0.1f
        feedbackImage.scaleY     = 0.1f
        feedbackImage.alpha      = 0f
        ObjectAnimator.ofPropertyValuesHolder(
            feedbackImage,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.1f, 1.5f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.1f, 1.5f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("alpha",  0f,   1f,   1f)
        ).apply {
            duration     = 1200
            interpolator = BounceInterpolator()
            start()
        }

        // Excited wiggle
        Handler(Looper.getMainLooper()).postDelayed({
            ObjectAnimator.ofFloat(feedbackImage, "rotation", -8f, 8f, -5f, 5f, 0f).apply {
                duration     = 600
                interpolator = LinearInterpolator()
                start()
            }
        }, 1300)

        // Pulse glow for champion
        Handler(Looper.getMainLooper()).postDelayed({ addChampionPulse() }, 1600)

        Handler(Looper.getMainLooper()).postDelayed({ fadeIn(friendName, 500) }, 350)
        Handler(Looper.getMainLooper()).postDelayed({ fadeIn(verseText, 600) }, 750)
        Handler(Looper.getMainLooper()).postDelayed({
            btnOk.visibility = View.VISIBLE
            btnOk.scaleX     = 0f
            btnOk.scaleY     = 0f
            ObjectAnimator.ofPropertyValuesHolder(
                btnOk,
                android.animation.PropertyValuesHolder.ofFloat("scaleX", 0f, 1.3f, 1f),
                android.animation.PropertyValuesHolder.ofFloat("scaleY", 0f, 1.3f, 1f)
            ).apply {
                duration     = 800
                interpolator = BounceInterpolator()
                start()
            }
            btnOk.setOnClickListener { animateDismiss() }
        }, 1100)

        Handler(Looper.getMainLooper()).postDelayed({
            speakText(friend.spokenVerse)
        }, 1300)
    }

    private fun addChampionPulse() {
        ObjectAnimator.ofPropertyValuesHolder(
            feedbackImage,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.07f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.07f, 1f)
        ).apply {
            duration     = 900
            repeatCount  = 5
            repeatMode   = ValueAnimator.REVERSE
            start()
        }
    }

    // ── Shared helper ─────────────────────────────────────────────────────────

    private fun fadeIn(view: View, durationMs: Long) {
        view.visibility = View.VISIBLE
        view.alpha      = 0f
        view.animate().alpha(1f).setDuration(durationMs).start()
    }

    // =========================================================================
    // Dismiss
    // =========================================================================

    private fun animateDismiss() {
        friendName.visibility = View.GONE
        verseText.visibility  = View.GONE
        btnOk.visibility      = View.GONE

        ObjectAnimator.ofPropertyValuesHolder(
            feedbackImage,
            android.animation.PropertyValuesHolder.ofFloat("alpha",  1f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 0.5f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 0.5f)
        ).apply {
            duration     = 800
            interpolator = AccelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    setResult(RESULT_OK, Intent().apply { putExtra("PRIZE_UNLOCKED", true) })
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            })
            start()
        }
    }

    // =========================================================================
    // TTS
    // =========================================================================

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US)
            tts.setPitch(1.5f)       // slightly warmer/higher for children
            tts.setSpeechRate(0.85f) // slower — especially important for calming content
            isTtsReady = true
            pendingSpeak?.let { speakText(it); pendingSpeak = null }
        }
    }

    private fun speakText(text: String) {
        val clean = text.replace(Regex("[^a-zA-Z0-9 !.,?'\\-\n]"), " ").trim()
        if (isTtsReady) {
            tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "verse")
        } else {
            pendingSpeak = clean
        }
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        clearAnimations()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        clearAnimations()
    }

    private fun clearAnimations() {
        try { giftBoxIcon.clearAnimation()   } catch (_: Exception) {}
        try { feedbackImage.clearAnimation() } catch (_: Exception) {}
        try { friendName.clearAnimation()    } catch (_: Exception) {}
        try { verseText.clearAnimation()     } catch (_: Exception) {}
        try { btnOk.clearAnimation()         } catch (_: Exception) {}
    }
}