package com.chirathi.voicebridge

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
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
 * ══ ANIMATION DESIGN PHILOSOPHY ══════════════════════════════════════════════
 *
 * Each panda has its OWN reveal choreography that mirrors its emotional identity.
 * There are no generic "soft" or "energetic" buckets — every character entrance
 * is tuned to reinforce the therapeutic message it carries.
 *
 *  MOCHI  (action 1) — BLANKET PANDA
 *  ─────────────────────────────────────────────────────────────────────────
 *  Entrance: A warm amber radial glow blooms behind the panda first, like a
 *  lamp turning on in a dark room. The panda then rises slowly from a slight
 *  downward offset and fades in. No sharp movements — every easing curve is
 *  decelerate only. The glow pulses very gently (±20% opacity) thereafter.
 *  Metaphor: being wrapped in something warm.
 *
 *  SENU   (action 2) — HUG PANDA
 *  ─────────────────────────────────────────────────────────────────────────
 *  Entrance: The panda scales in from 0.25 with an OvershootInterpolator so
 *  it slightly over-expands (like arms opening wide) then settles. A soft
 *  pink radial glow expands simultaneously, timed to peak at the same moment
 *  the arms "open". After settling, a single slow heartbeat pulse (1.0→1.06→1.0)
 *  repeats twice — warmth, not excitement.
 *  Metaphor: someone opening their arms to embrace you.
 *
 *  LUMI   (action 5) — STAR PANDA
 *  ─────────────────────────────────────────────────────────────────────────
 *  Entrance: A golden-white radial glow fades in BEFORE the panda appears.
 *  The panda then cross-fades in while gently rotating from –8° to 0°, as if
 *  a star is drifting into position. Three quick shimmer-pulses (scale 1→1.04→1
 *  over 200 ms each) fire during the fade, simulating a twinkle. The glow
 *  continues pulsing between 60–100% opacity on a 1.8 s loop.
 *  Metaphor: a star appearing in the night sky.
 *
 *  TUTU   (action 9) — TROPHY PANDA
 *  ─────────────────────────────────────────────────────────────────────────
 *  Entrance: The panda rises from y+280 dp to rest position with a custom
 *  spring: DecelerateInterpolator(2f) so it slows dramatically at the top —
 *  like a champion stepping onto a podium. A cool golden spotlight (top-to-
 *  transparent gradient oval) descends from above in sync. After landing, a
 *  single confident flex (1.0→1.12→1.0) over 500 ms — not a bounce, a flex.
 *  Metaphor: mounting the winner's podium.
 *
 *  PINKY  (default)  — HAPPY PANDA
 *  ─────────────────────────────────────────────────────────────────────────
 *  Entrance: Simple petal-bloom: scale 0→1.15→1.0 with a bouncy easing, and
 *  a mild multi-colour pastel glow that fades after reveal. Light and fun.
 *  Metaphor: a flower blooming.
 */
class AllCorrectGrandPrizeActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // ── Views ─────────────────────────────────────────────────────────────────
    private lateinit var feedbackImage: ImageView
    private lateinit var friendName:    TextView
    private lateinit var verseText:     TextView
    private lateinit var btnOk:         Button
    private lateinit var mainLayout:    ConstraintLayout
    private lateinit var giftBoxIcon:   ImageView

    /** Radial-glow view added behind the panda — tracked for cleanup */
    private var glowView: View? = null
    /** Running glow pulse animator — stopped on pause/destroy */
    private var glowPulse: ValueAnimator? = null

    // ── TTS ───────────────────────────────────────────────────────────────────
    private lateinit var tts: TextToSpeech
    private var isTtsReady  = false
    private var pendingSpeak: String? = null

    // ── Friend data ───────────────────────────────────────────────────────────
    private var friendAction = 0
    private var ageGroup     = 6
    private lateinit var friend: TherapeuticFriend

    companion object {
        private const val TAG = "GrandPrize"
    }

    // =========================================================================
    // Data model
    // =========================================================================

    data class TherapeuticFriend(
        val drawableRes: Int,
        val name:        String,
        val verse:       String,
        val spokenVerse: String,
        val okLabel:     String,
        val animStyle:   AnimStyle
    )

    /**
     * One enum value per character — each maps to its own reveal choreography.
     *
     *  BLANKET  →  warm amber glow + slow upward drift
     *  HUG      →  pink glow + arms-opening overshoot expand
     *  STAR     →  gold-white glow first, then panda twinkle-fades in
     *  TROPHY   →  podium rise + spotlight + single confident flex
     *  HAPPY    →  pastel bloom (default fallback)
     */
    enum class AnimStyle { BLANKET, HUG, STAR, TROPHY, HAPPY }

    // =========================================================================
    // onCreate
    // =========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_all_correct_grand_prize)

        friendAction = intent.getIntExtra("FRIEND_ACTION", 0)
        ageGroup     = intent.getIntExtra("AGE_GROUP", 6)
        Log.d(TAG, "friendAction=$friendAction  ageGroup=$ageGroup")

        friend = resolveFriend(friendAction, ageGroup)
        Log.d(TAG, "Resolved friend: ${friend.name}  animStyle=${friend.animStyle}")

        tts = TextToSpeech(this, this)

        feedbackImage = findViewById(R.id.feedbackImage)
        friendName    = findViewById(R.id.title)
        verseText     = findViewById(R.id.unlockedText)
        btnOk         = findViewById(R.id.btn_ok)
        mainLayout    = findViewById(R.id.main)

        feedbackImage.setImageResource(friend.drawableRes)
        friendName.text = friend.name
        verseText.text  = friend.verse
        btnOk.text      = friend.okLabel

        feedbackImage.visibility = View.GONE
        friendName.visibility    = View.GONE
        verseText.visibility     = View.GONE
        btnOk.visibility         = View.GONE

        // Build the gift-box overlay
        giftBoxIcon = ImageView(this).apply {
            setImageResource(R.drawable.giftbox)
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop       = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart   = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd       = ConstraintLayout.LayoutParams.PARENT_ID
            }
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
    // Friend resolution
    // =========================================================================

    private fun resolveFriend(action: Int, age: Int): TherapeuticFriend {
        val isYoung = age <= 7
        fun drawable(name: String): Int {
            val id = resources.getIdentifier(name, "drawable", packageName)
            return if (id != 0) id else R.drawable.panda_confused
        }

        return when (action) {

            1 -> TherapeuticFriend(
                drawableRes = drawable("panda_blanket"),
                name        = "Mochi",
                verse       = if (isYoung)
                    "\"Take a big breath with me… in… and out.\n\nI could see how hard you were trying. I'm proud of you.\""
                else
                    "\"Let's breathe together for a moment. In… and slowly out.\n\nI noticed how much effort you put in today, and you did well.\"",
                spokenVerse = if (isYoung)
                    "Take a big breath with me. In. And out. I could see how hard you were trying. And I'm proud of you."
                else
                    "Let's breathe together for a moment. In. And slowly out. I noticed how much effort you put in today. You did well.",
                okLabel   = "Thank you, Mochi",
                animStyle = AnimStyle.BLANKET
            )

            2 -> TherapeuticFriend(
                drawableRes = drawable("panda_hug"),
                name        = if (isYoung) "Senu the Snuggle Panda" else "Senu",
                verse       = if (isYoung)
                    "\"You kept going, even when it was hard!\n\nCome here — this hug is just for you, because you never gave up!\""
                else
                    "\"You kept going, even when things got difficult.\n\nThis hug is for you, because trying hard always counts.\"",
                spokenVerse = if (isYoung)
                    "You kept going, even when it was hard! This hug is just for you, because you never gave up!"
                else
                    "You kept going, even when things got difficult. This hug is for you.",
                okLabel   = "I love hugs!",
                animStyle = AnimStyle.HUG
            )

            5 -> TherapeuticFriend(
                drawableRes = drawable("panda_star"),
                name        = "Lumi",
                verse       = if (isYoung)
                    "\"Do you know what I see? A star.\n\nYou worked SO hard today. You're my star!\""
                else
                    "\"Every attempt you made today — the right ones and the hard ones — all of them made you stronger.\n\nYou earned this. Keep shining.\"",
                spokenVerse = if (isYoung)
                    "Do you know what I see? A star. You worked so hard today. You are my star!"
                else
                    "Every attempt you made today, the right ones and the hard ones, all of them made you stronger. You earned this. Keep shining.",
                okLabel   = "I'm a star!",
                animStyle = AnimStyle.STAR
            )

            9 -> TherapeuticFriend(
                drawableRes = drawable("panda_trophy"),
                name        = if (isYoung) "Tutu" else "Tutu",
                verse       = if (isYoung)
                    "\"WOW! You were absolutely AMAZING today!\n\nYou answered fast and you got it right!\n\nThis trophy is YOURS!\""
                else
                    "\"That was a great performance today!\n\nThis trophy is yours. Own it — you completely earned it.\"",
                spokenVerse = if (isYoung)
                    "Wow! You were absolutely amazing today! This trophy is yours. You totally earned it!"
                else
                    "That was a great performance today! This trophy is yours. Own it. You completely earned it.",
                okLabel   = "I'm a champion!",
                animStyle = AnimStyle.TROPHY
            )

            else -> TherapeuticFriend(
                drawableRes = drawable("panda_happy"),
                name        = if (isYoung) "Pinky" else "Pinky",
                verse       = if (isYoung)
                    "\"You did it! I'm SO happy for you!\n\nYou played today and that makes me so proud.\n\nSee you next time, friend!\""
                else
                    "\"Great session today!\n\nYou showed up, you tried, and that always counts.\n\nYour Happy Panda is proud of you — see you next time!\"",
                spokenVerse = if (isYoung)
                    "You did it! I am so happy for you! You played today and that makes me so proud. See you next time, friend!"
                else
                    "Great session today! You showed up, you tried, and that always counts. Your Happy Panda is proud of you. See you next time!",
                okLabel   = "Bye, Panda!",
                animStyle = AnimStyle.HAPPY
            )
        }
    }

    // =========================================================================
    // Gift-box animation
    // =========================================================================

    private fun startGiftAnimation() {
        giftBoxIcon.x = (mainLayout.width  - giftBoxIcon.width)  / 2f
        giftBoxIcon.y = (mainLayout.height - giftBoxIcon.height) / 2f

        // Trophy Panda gets a quicker, more energetic unboxing.
        // All other pandas get the calm unboxing to stay consistent with their tone.
        if (friend.animStyle == AnimStyle.TROPHY) {
            startEnergeticUnboxing()
        } else {
            startCalmUnboxing()
        }
    }

    /** Slow rock → gentle glow-expand → fades away calmly */
    private fun startCalmUnboxing() {
        val rock = ObjectAnimator.ofFloat(giftBoxIcon, "rotation", -6f, 6f, -4f, 4f, 0f).apply {
            duration     = 1400
            repeatCount  = 1
            interpolator = LinearInterpolator()
        }

        val expand = ObjectAnimator.ofPropertyValuesHolder(
            giftBoxIcon,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.3f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.3f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("alpha",  1f, 0.8f, 0f)
        ).apply {
            duration     = 1000
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mainLayout.removeView(giftBoxIcon)
                    dispatchCharacterReveal()
                }
            })
        }

        rock.start()
        delay(2900) { expand.start() }
    }

    /** Sharp shake → explosive burst → clears */
    private fun startEnergeticUnboxing() {
        val shake = ObjectAnimator.ofFloat(
            giftBoxIcon, "rotation",
            -14f, 14f, -14f, 14f, -8f, 8f, 0f
        ).apply {
            duration     = 900
            repeatCount  = 2
            interpolator = LinearInterpolator()
        }

        val burst = ObjectAnimator.ofPropertyValuesHolder(
            giftBoxIcon,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 2.2f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 2.2f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("alpha",  1f,   1f, 0f)
        ).apply {
            duration     = 650
            interpolator = AccelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mainLayout.removeView(giftBoxIcon)
                    dispatchCharacterReveal()
                }
            })
        }

        shake.start()
        delay(2000) { burst.start() }
    }

    // =========================================================================
    // Character reveal dispatcher
    // =========================================================================

    private fun dispatchCharacterReveal() {
        when (friend.animStyle) {
            AnimStyle.BLANKET -> revealBlanketPanda()
            AnimStyle.HUG     -> revealHugPanda()
            AnimStyle.STAR    -> revealStarPanda()
            AnimStyle.TROPHY  -> revealTrophyPanda()
            AnimStyle.HAPPY   -> revealHappyPanda()
        }
    }

    // =========================================================================
    // ── MOCHI — BLANKET PANDA ────────────────────────────────────────────────
    //
    //  1. Amber radial glow blooms from behind, like a warm lamp turning on.
    //  2. Panda rises slowly from y+40dp, fading in. No sudden moves.
    //  3. Glow settles into a very soft breath-like pulse (±20% opacity).
    //  4. Text and button drift in with long, restful delays.
    // =========================================================================

    private fun revealBlanketPanda() {
        val glowColor = Color.argb(110, 255, 180, 60) // warm amber

        // 1. Glow blooms first — before the panda appears
        val glow = buildRadialGlow(glowColor, scaleFactor = 1.7f)
        glow.alpha = 0f
        ObjectAnimator.ofFloat(glow, "alpha", 0f, 1f).apply {
            duration     = 1200
            interpolator = DecelerateInterpolator()
            start()
        }

        // 2. Panda drifts upward and fades in after the glow has established itself
        delay(600) {
            feedbackImage.visibility   = View.VISIBLE
            feedbackImage.alpha        = 0f
            feedbackImage.translationY = dpToPx(40f)
            feedbackImage.scaleX       = 0.92f
            feedbackImage.scaleY       = 0.92f
            feedbackImage.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f).scaleY(1f)
                .setDuration(1100)
                .setInterpolator(DecelerateInterpolator(2.5f))
                .start()
        }

        // 3. Glow breathes gently after the panda has settled
        delay(1900) {
            glowPulse = ObjectAnimator.ofFloat(glow, "alpha", 1f, 0.55f).apply {
                duration    = 2200
                repeatCount = ValueAnimator.INFINITE
                repeatMode  = ValueAnimator.REVERSE
                interpolator = DecelerateInterpolator()
                start()
            }
        }

        // 4. Text — calm, long pauses (child should breathe before reading)
        delay(800)  { fadeIn(friendName, 800) }
        delay(1600) { fadeIn(verseText, 1000) }
        delay(2500) {
            fadeIn(btnOk, 700)
            btnOk.setOnClickListener { animateDismiss() }
        }

        // 5. Spoken verse with a calm leading pause
        delay(1800) { speakText(friend.spokenVerse) }
    }

    // =========================================================================
    // ── SENU — HUG PANDA ─────────────────────────────────────────────────────
    //
    //  1. Soft rose-pink glow expands outward — like arms spreading wide.
    //  2. Panda scales from 0.2 with OvershootInterpolator (arms "opening").
    //  3. After settling: two gentle heartbeat pulses (1→1.06→1 each).
    //  4. Text fades in with warm, unhurried timing.
    // =========================================================================

    private fun revealHugPanda() {
        val glowColor = Color.argb(100, 255, 145, 175) // rose pink

        // 1. Pink glow expands — arms opening metaphor
        val glow = buildRadialGlow(glowColor, scaleFactor = 1.9f)
        glow.alpha  = 0f
        glow.scaleX = 0.3f
        glow.scaleY = 0.3f
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(glow, "alpha",  0f, 1f).setDuration(900) as Animator,
                ObjectAnimator.ofFloat(glow, "scaleX", 0.3f, 1f).setDuration(900) as Animator,
                ObjectAnimator.ofFloat(glow, "scaleY", 0.3f, 1f).setDuration(900) as Animator
            )
            interpolator = DecelerateInterpolator(1.5f)
            start()
        }

        // 2. Panda scales in with gentle overshoot (arms opening wide)
        feedbackImage.visibility = View.VISIBLE
        feedbackImage.alpha      = 0f
        feedbackImage.scaleX     = 0.2f
        feedbackImage.scaleY     = 0.2f
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(feedbackImage, "scaleX", 0.2f, 1f).setDuration(1000) as Animator,
                ObjectAnimator.ofFloat(feedbackImage, "scaleY", 0.2f, 1f).setDuration(1000) as Animator,
                ObjectAnimator.ofFloat(feedbackImage, "alpha",  0f,   1f).setDuration(600) as Animator
            )
            interpolator = OvershootInterpolator(1.6f)
            start()
        }

        // 3. Heartbeat pulse — twice, then the glow settles to soft breathe
        delay(1200) { heartbeatPulse(feedbackImage, times = 2) }
        delay(1500) {
            glowPulse = ObjectAnimator.ofFloat(glow, "alpha", 1f, 0.45f).apply {
                duration     = 1800
                repeatCount  = ValueAnimator.INFINITE
                repeatMode   = ValueAnimator.REVERSE
                interpolator = DecelerateInterpolator()
                start()
            }
        }

        // 4. Text
        delay(500)  { fadeIn(friendName, 700) }
        delay(1100) { fadeIn(verseText, 800) }
        delay(2000) {
            fadeIn(btnOk, 600)
            btnOk.setOnClickListener { animateDismiss() }
        }

        delay(1400) { speakText(friend.spokenVerse) }
    }

    // =========================================================================
    // ── LUMI — STAR PANDA ────────────────────────────────────────────────────
    //
    //  1. Gold-white radial glow fades in FIRST — the star's light arrives
    //     before you see the star itself.
    //  2. Panda cross-fades in while slowly rotating from –8° to 0° (drifting
    //     into position, like a star finding its place in the sky).
    //  3. Three quick shimmer-pulses (scale 1→1.04→1, 200 ms each) fire
    //     during the fade — the "twinkle" effect.
    //  4. Glow settles into a continuous 1.8 s pulse between 55–100% opacity.
    //  5. Text fades in with star-appropriate unhurried grace.
    // =========================================================================

    private fun revealStarPanda() {
        val glowColor = Color.argb(130, 255, 240, 100) // golden-white

        // 1. Glow appears first — "the light before the star"
        val glow = buildRadialGlow(glowColor, scaleFactor = 2.0f)
        glow.alpha = 0f
        ObjectAnimator.ofFloat(glow, "alpha", 0f, 1f).apply {
            duration     = 1000
            interpolator = DecelerateInterpolator()
            start()
        }

        // 2. Panda fades in with a slow rotation — star drifting into position
        delay(700) {
            feedbackImage.visibility  = View.VISIBLE
            feedbackImage.alpha       = 0f
            feedbackImage.rotation    = -8f
            feedbackImage.scaleX      = 0.88f
            feedbackImage.scaleY      = 0.88f
            feedbackImage.animate()
                .alpha(1f)
                .rotation(0f)
                .scaleX(1f).scaleY(1f)
                .setDuration(1000)
                .setInterpolator(DecelerateInterpolator(2f))
                .start()

            // 3. Three twinkle shimmer pulses during the fade-in
            shimmerPulse(feedbackImage, delayMs = 100)
            shimmerPulse(feedbackImage, delayMs = 350)
            shimmerPulse(feedbackImage, delayMs = 600)
        }

        // 4. Glow settles into slow continuous pulse
        delay(1800) {
            glowPulse = ObjectAnimator.ofFloat(glow, "alpha", 1f, 0.55f).apply {
                duration     = 1800
                repeatCount  = ValueAnimator.INFINITE
                repeatMode   = ValueAnimator.REVERSE
                interpolator = LinearInterpolator()
                start()
            }
        }

        // 5. Text — a little unhurried, like stargazing
        delay(600)  { fadeIn(friendName, 750) }
        delay(1200) { fadeIn(verseText, 900) }
        delay(2100) {
            fadeIn(btnOk, 650)
            btnOk.setOnClickListener { animateDismiss() }
        }

        delay(1500) { speakText(friend.spokenVerse) }
    }

    // =========================================================================
    // ── TUTU — TROPHY PANDA ──────────────────────────────────────────────────
    //
    //  1. A golden spotlight (top → transparent radial gradient) descends from
    //     above and locks onto centre — the podium light arriving.
    //  2. Panda rises from y+280dp with DecelerateInterpolator(2.5f) — a slow,
    //     deliberate, champion's step onto the podium. No bounce.
    //  3. After landing: one confident flex (1→1.12→1 over 500 ms) — not
    //     a bounce, a single deliberate "trophy lift".
    //  4. Spotlight glow pulses with a slow 3 s loop.
    //  5. Name and verse arrive faster than other pandas — energy and pride.
    // =========================================================================

    private fun revealTrophyPanda() {
        val glowColor = Color.argb(120, 255, 210, 40) // champion gold

        // 1. Spotlight descends from above
        val spotlight = buildRadialGlow(glowColor, scaleFactor = 1.6f)
        spotlight.alpha        = 0f
        spotlight.translationY = dpToPx(-80f) // starts above centre
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(spotlight, "alpha",        0f, 1f).setDuration(900) as Animator,
                ObjectAnimator.ofFloat(spotlight, "translationY", dpToPx(-80f), 0f).setDuration(900) as Animator
            )
            interpolator = DecelerateInterpolator(2f)
            start()
        }

        // 2. Panda rises from below — deliberate podium step
        delay(300) {
            feedbackImage.visibility   = View.VISIBLE
            feedbackImage.alpha        = 0f
            feedbackImage.translationY = dpToPx(280f)
            feedbackImage.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(1100)
                .setInterpolator(DecelerateInterpolator(2.5f))
                .start()
        }

        // 3. One confident trophy-lift flex after landing
        delay(1500) {
            ObjectAnimator.ofPropertyValuesHolder(
                feedbackImage,
                android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.12f, 1f),
                android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.12f, 1f)
            ).apply {
                duration     = 500
                interpolator = DecelerateInterpolator()
                start()
            }
        }

        // 4. Spotlight slow pulse
        delay(2100) {
            glowPulse = ObjectAnimator.ofFloat(spotlight, "alpha", 1f, 0.5f).apply {
                duration     = 2800
                repeatCount  = ValueAnimator.INFINITE
                repeatMode   = ValueAnimator.REVERSE
                interpolator = LinearInterpolator()
                start()
            }
        }

        // 5. Text — quicker than comfort pandas (energy and pride)
        delay(400)  { fadeIn(friendName, 500) }
        delay(850)  { fadeIn(verseText, 600) }
        delay(1600) {
            btnOk.visibility = View.VISIBLE
            btnOk.scaleX     = 0f
            btnOk.scaleY     = 0f
            ObjectAnimator.ofPropertyValuesHolder(
                btnOk,
                android.animation.PropertyValuesHolder.ofFloat("scaleX", 0f, 1.2f, 1f),
                android.animation.PropertyValuesHolder.ofFloat("scaleY", 0f, 1.2f, 1f)
            ).apply {
                duration     = 600
                interpolator = OvershootInterpolator(1.4f)
                start()
            }
            btnOk.setOnClickListener { animateDismiss() }
        }

        delay(1300) { speakText(friend.spokenVerse) }
    }

    // =========================================================================
    // ── PINKY — HAPPY PANDA (default) ────────────────────────────────────────
    //
    //  Simple petal-bloom: scale 0→1.18→1.0 + alpha fade + pastel glow ring.
    //  Light, uncomplicated, cheerful.
    // =========================================================================

    private fun revealHappyPanda() {
        val glowColor = Color.argb(90, 160, 220, 255) // soft sky blue

        val glow = buildRadialGlow(glowColor, scaleFactor = 1.65f)
        glow.alpha = 0f

        feedbackImage.visibility = View.VISIBLE
        feedbackImage.scaleX     = 0f
        feedbackImage.scaleY     = 0f
        feedbackImage.alpha      = 0f

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(feedbackImage, "scaleX", 0f, 1.18f, 1f).setDuration(900) as Animator,
                ObjectAnimator.ofFloat(feedbackImage, "scaleY", 0f, 1.18f, 1f).setDuration(900) as Animator,
                ObjectAnimator.ofFloat(feedbackImage, "alpha",  0f, 1f).setDuration(600) as Animator,
                ObjectAnimator.ofFloat(glow, "alpha",  0f, 0.85f).setDuration(700) as Animator
            )
            interpolator = DecelerateInterpolator(1.8f)
            start()
        }

        delay(1100) {
            glowPulse = ObjectAnimator.ofFloat(glow, "alpha", 0.85f, 0.3f).apply {
                duration     = 2000
                repeatCount  = ValueAnimator.INFINITE
                repeatMode   = ValueAnimator.REVERSE
                interpolator = DecelerateInterpolator()
                start()
            }
        }

        delay(400)  { fadeIn(friendName, 700) }
        delay(900)  { fadeIn(verseText, 800) }
        delay(1700) {
            fadeIn(btnOk, 600)
            btnOk.setOnClickListener { animateDismiss() }
        }

        delay(1200) { speakText(friend.spokenVerse) }
    }

    // =========================================================================
    // Glow helper — builds a radial-gradient oval view behind the panda
    // =========================================================================

    /**
     * Creates a circular radial glow view (color centre → transparent edge),
     * inserts it directly behind [feedbackImage] in the layout, and stores a
     * reference in [glowView] for cleanup.
     *
     * @param centerColor  ARGB colour of the glow core
     * @param scaleFactor  How much bigger than the feedbackImage the glow should be
     */
    private fun buildRadialGlow(centerColor: Int, scaleFactor: Float = 1.8f): View {
        val refSize  = feedbackImage.width.coerceAtLeast(feedbackImage.height)
        val glowSize = (refSize * scaleFactor).toInt().coerceAtLeast(dpToPx(220f).toInt())

        val gradient = GradientDrawable().apply {
            shape         = GradientDrawable.OVAL
            gradientType  = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = glowSize / 2f
            colors        = intArrayOf(centerColor, Color.TRANSPARENT)
        }

        val view = View(this).apply {
            background = gradient
            layoutParams = ConstraintLayout.LayoutParams(glowSize, glowSize).apply {
                topToTop       = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart   = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd       = ConstraintLayout.LayoutParams.PARENT_ID
            }
        }

        // Insert immediately behind the panda image (so panda draws on top)
        val pandaIndex = mainLayout.indexOfChild(feedbackImage)
        mainLayout.addView(view, if (pandaIndex >= 0) pandaIndex else mainLayout.childCount)

        glowView = view
        return view
    }

    // =========================================================================
    // Animation micro-helpers
    // =========================================================================

    /**
     * A single gentle heartbeat pulse: scale 1→1.06→1 over ~600 ms.
     * @param times  How many pulses to fire in sequence.
     */
    private fun heartbeatPulse(target: View, times: Int = 1) {
        repeat(times) { i ->
            delay((i * 650).toLong()) {
                ObjectAnimator.ofPropertyValuesHolder(
                    target,
                    android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.06f, 1f),
                    android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.06f, 1f)
                ).apply {
                    duration     = 600
                    interpolator = DecelerateInterpolator()
                    start()
                }
            }
        }
    }

    /**
     * A single quick shimmer-twinkle: scale 1→1.04→1 over 200 ms.
     * Fire at different [delayMs] offsets to create a multi-twinkle effect.
     */
    private fun shimmerPulse(target: View, delayMs: Long) {
        delay(delayMs) {
            ObjectAnimator.ofPropertyValuesHolder(
                target,
                android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.04f, 1f),
                android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.04f, 1f)
            ).apply {
                duration     = 200
                interpolator = LinearInterpolator()
                start()
            }
        }
    }

    private fun fadeIn(view: View, durationMs: Long) {
        view.visibility = View.VISIBLE
        view.alpha      = 0f
        view.animate().alpha(1f).setDuration(durationMs).start()
    }

    private fun dpToPx(dp: Float): Float =
        dp * resources.displayMetrics.density

    /** Syntactic sugar for Handler.postDelayed */
    private fun delay(ms: Long, block: () -> Unit) =
        Handler(Looper.getMainLooper()).postDelayed(block, ms)

    // =========================================================================
    // Dismiss
    // =========================================================================

    private fun animateDismiss() {
        friendName.visibility = View.GONE
        verseText.visibility  = View.GONE
        btnOk.visibility      = View.GONE

        glowPulse?.cancel()

        // Panda and glow shrink and fade together
        val pandaShrink = ObjectAnimator.ofPropertyValuesHolder(
            feedbackImage,
            android.animation.PropertyValuesHolder.ofFloat("alpha",  1f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 0.4f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 0.4f)
        ).apply { duration = 750; interpolator = AccelerateInterpolator() }

        val glowFade = glowView?.let {
            ObjectAnimator.ofFloat(it, "alpha", it.alpha, 0f).apply { duration = 600 }
        }

        AnimatorSet().apply {
            if (glowFade != null) playTogether(pandaShrink, glowFade) else play(pandaShrink)
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
            tts.setPitch(1.5f)
            tts.setSpeechRate(0.85f)
            isTtsReady = true
            pendingSpeak?.let { speakText(it); pendingSpeak = null }
        }
    }

    private fun speakText(text: String) {
        val clean = text.replace(Regex("[^a-zA-Z0-9 !.,?'\\-\n]"), " ").trim()
        if (isTtsReady) tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "verse")
        else pendingSpeak = clean
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        glowPulse?.cancel()
        clearAnimations()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        glowPulse?.cancel()
        clearAnimations()
    }

    private fun clearAnimations() {
        listOf(giftBoxIcon, feedbackImage, friendName, verseText, btnOk, glowView)
            .filterNotNull()
            .forEach { try { it.clearAnimation() } catch (_: Exception) {} }
    }
}