package com.chirathi.voicebridge

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import android.widget.ImageView
import android.widget.FrameLayout

/**
 * RoutineSelectionActivity — updated.
 *
 *  Changes:
 *   1. Age 8-10 guide now uses ASGuidanceAboveActivity (drag-drop guide),
 *      not the tap guide (ASGuide_BelowActivity). Separate session flag used.
 *   2. Unlocked routines shown in PINK (matching Morning Routine style),
 *      not blue.
 *   3. "My Stickers" button preserved.
 *   4. Sticker count display and routine unlock logic unchanged.
 */
class RoutineSelectionActivity : AppCompatActivity() {

    private lateinit var auth   : FirebaseAuth
    private val db              = Firebase.firestore
    private val TAG             = "RoutineSelection"

    private lateinit var morningRoutineLayout  : LinearLayout
    private lateinit var localTimeRoutineLayout: LinearLayout
    private lateinit var schoolRoutineLayout   : LinearLayout
    private lateinit var mainContainer         : ConstraintLayout

    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routine_selection)

        // In onCreate(), after setContentView:
        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            startActivity(Intent(this, GameDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }

        findViewById<FrameLayout>(R.id.stickerChip).setOnClickListener {
            startActivity(Intent(this, StickerGalleryActivity::class.java))
        }

        sharedPrefs = getSharedPreferences("unlocked_routines", Context.MODE_PRIVATE)

        morningRoutineLayout   = findViewById(R.id.morningRoutineLayout)
        localTimeRoutineLayout = findViewById(R.id.localTimeRoutineLayout)
        schoolRoutineLayout    = findViewById(R.id.schoolRoutineLayout)
        mainContainer          = findViewById(R.id.mainContainer)

        auth = FirebaseAuth.getInstance()

        StickerManager.updateRoutineUnlocks(this)
        checkRoutineUnlock()

//        addStickerGalleryButton()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        StickerManager.updateRoutineUnlocks(this)
        checkRoutineUnlock()
        updateStickerCountDisplay()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sticker gallery button
    // ─────────────────────────────────────────────────────────────────────────

//    private fun addStickerGalleryButton() {
//        if (mainContainer.findViewWithTag<View>("sticker_btn") != null) return
//
//        val btn = androidx.appcompat.widget.AppCompatButton(this).apply {
//            text = "My Stickers"
//            textSize = 16f
//            tag = "sticker_btn"
//            setTextColor(ContextCompat.getColor(this@RoutineSelectionActivity, android.R.color.white))
//            setBackgroundResource(R.drawable.rounded_button_background)
//            setOnClickListener {
//                startActivity(Intent(this@RoutineSelectionActivity, StickerGalleryActivity::class.java))
//            }
//        }
//
//        val lp = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
//            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
//            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
//        ).apply {
//            topToTop    = ConstraintLayout.LayoutParams.PARENT_ID
//            endToEnd    = ConstraintLayout.LayoutParams.PARENT_ID
//            topMargin   = 48
//            rightMargin = 24
//        }
//        mainContainer.addView(btn, lp)
//    }

    // ─────────────────────────────────────────────────────────────────────────
    // Routine unlock — pink tint for unlocked routines
    // ─────────────────────────────────────────────────────────────────────────

    /** Pink colour matching the Morning Routine box */
    private val PINK_UNLOCKED = Color.parseColor("#F48FB1")

    private fun checkRoutineUnlock() {
        runOnUiThread {
            try {
                val prefs = getSharedPreferences("unlocked_routines", Context.MODE_PRIVATE)

                if (prefs.getBoolean("routine_1", false)) {
                    localTimeRoutineLayout.isEnabled = true
                    localTimeRoutineLayout.alpha     = 1.0f
                    // Pink background tint instead of blue
                    localTimeRoutineLayout.backgroundTintList =
                        ColorStateList.valueOf(PINK_UNLOCKED)
                    setTagVisibility("lockedTag1", "availableTag1", hidden = true)
                }

                if (prefs.getBoolean("routine_2", false)) {
                    schoolRoutineLayout.isEnabled = true
                    schoolRoutineLayout.alpha     = 1.0f
                    schoolRoutineLayout.backgroundTintList =
                        ColorStateList.valueOf(PINK_UNLOCKED)
                    setTagVisibility("lockedTag2", "availableTag2", hidden = true)
                }

                updateStickerCountDisplay()

            } catch (e: Exception) {
                Log.e(TAG, "checkRoutineUnlock error: ${e.message}")
            }
        }
    }

    private fun setTagVisibility(lockedId: String, availableId: String, hidden: Boolean) {
        val res = resources; val pkg = packageName
        val lockedResId    = res.getIdentifier(lockedId,    "id", pkg)
        val availableResId = res.getIdentifier(availableId, "id", pkg)
        if (lockedResId    != 0) findViewById<TextView>(lockedResId)?.visibility    = if (hidden) View.GONE else View.VISIBLE
        if (availableResId != 0) findViewById<TextView>(availableResId)?.visibility = if (hidden) View.VISIBLE else View.GONE
    }

    private fun updateStickerCountDisplay() {
        try {
            val count    = StickerManager.stickerCount(this)
            val infoText = findViewById<TextView>(R.id.infoTextView) ?: return
            infoText.text = when {
                count >= 6 -> "All routines unlocked! 🎉 You have $count stickers!"
                count >= 3 -> "You have $count stickers! Collect ${6 - count} more for School Routine!"
                else       -> "You have $count stickers. Collect ${3 - count} more for Bedtime Routine!"
            }
        } catch (_: Exception) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Click listeners
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupClickListeners() {
        morningRoutineLayout.setOnClickListener {
            animateClick(morningRoutineLayout)
            Handler(Looper.getMainLooper()).postDelayed({
                checkAgeAndNavigate("Morning Routine", morningRoutineLayout)
            }, 120)
        }

        localTimeRoutineLayout.setOnClickListener {
            val unlocked = getSharedPreferences("unlocked_routines", Context.MODE_PRIVATE)
                .getBoolean("routine_1", false)
            if (unlocked) {
                animateClick(localTimeRoutineLayout)
                Handler(Looper.getMainLooper()).postDelayed({
                    checkAgeAndNavigate("Local-Time Routine", localTimeRoutineLayout)
                }, 120)
            } else {
                animateLocked(localTimeRoutineLayout)
                showLockedToast("Local-Time Routine")
            }
        }

        schoolRoutineLayout.setOnClickListener {
            val unlocked = getSharedPreferences("unlocked_routines", Context.MODE_PRIVATE)
                .getBoolean("routine_2", false)
            if (unlocked) {
                animateClick(schoolRoutineLayout)
                Handler(Looper.getMainLooper()).postDelayed({
                    checkAgeAndNavigate("School Routine", schoolRoutineLayout)
                }, 120)
            } else {
                animateLocked(schoolRoutineLayout)
                showLockedToast("School Routine")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Age-based navigation
    // ─────────────────────────────────────────────────────────────────────────

    private fun checkAgeAndNavigate(routineName: String, clickedView: View) {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java)); finish(); return
        }

        clickedView.alpha = 0.7f; clickedView.isEnabled = false

        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                clickedView.alpha = 1.0f; clickedView.isEnabled = true

                if (!document.exists()) {
                    Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java)); finish()
                    return@addOnSuccessListener
                }

                val ageString = document.getString("age")
                if (ageString.isNullOrEmpty()) {
                    Toast.makeText(this, "Age information missing", Toast.LENGTH_SHORT).show()
                    navigateToDefaultActivity(routineName); return@addOnSuccessListener
                }

                val routineId = routineId(routineName)
                val age       = ageString.toIntOrNull() ?: run {
                    navigateToDefaultActivity(routineName); return@addOnSuccessListener
                }
                navigateBasedOnAge(age, routineId)
            }
            .addOnFailureListener { e ->
                clickedView.alpha = 1.0f; clickedView.isEnabled = true
                Log.e(TAG, "Firestore error: ${e.message}")
                navigateToDefaultActivity(routineName)
            }
    }

    private fun navigateBasedOnAge(age: Int, routineId: Int) {
        when (age) {
            in 6..7  -> showTapGuidance(routineId, age)
            in 8..10 -> showDragAndDropGuidance(routineId, age)
            else     -> {
                Toast.makeText(this, "Age $age outside 6-10 range, defaulting", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, ActivitySequenceUnderActivity::class.java).apply {
                    putExtra("SELECTED_ROUTINE_ID", routineId); putExtra("USER_AGE", age)
                })
            }
        }
    }

    /**
     * Age 6-7 — tap guide shown ONCE per session via ASGuide_BelowActivity.
     */
    private fun showTapGuidance(routineId: Int, age: Int) {
        if (ASGuide_BelowActivity.hasBeenShownThisSession) {
            startActivity(Intent(this, ActivitySequenceUnderActivity::class.java).apply {
                putExtra("SELECTED_ROUTINE_ID", routineId); putExtra("USER_AGE", age)
            })
        } else {
            startActivity(Intent(this, ASGuide_BelowActivity::class.java).apply {
                putExtra("SELECTED_ROUTINE_ID", routineId); putExtra("USER_AGE", age)
            })
        }
    }

    /**
     * Age 8-10 — drag-drop guide shown ONCE per session via ASGuidanceAboveActivity.
     * Uses its own session flag so the two guides don't interfere.
     */
    private fun showDragAndDropGuidance(routineId: Int, age: Int) {
        if (ASGuidanceAboveActivity.hasBeenShownThisSession) {
            // Skip guide — go straight to drag-drop game
            startActivity(Intent(this, ActivitySequenceOverActivity::class.java).apply {
                putExtra("SELECTED_ROUTINE_ID", routineId); putExtra("USER_AGE", age)
            })
        } else {
            startActivity(Intent(this, ASGuidanceAboveActivity::class.java).apply {
                putExtra("SELECTED_ROUTINE_ID", routineId); putExtra("USER_AGE", age)
            })
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Animations & helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun animateClick(view: View) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
            .withEndAction { view.animate().scaleX(1f).scaleY(1f).setDuration(100).start() }.start()
    }

    private fun animateLocked(view: View) {
        view.animate().scaleX(1.05f).scaleY(1.05f).alpha(0.7f).setDuration(100)
            .withEndAction { view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start() }.start()
    }

    private fun showLockedToast(routineName: String) {
        val count = StickerManager.stickerCount(this)
        val msg   = when (routineName) {
            "Local-Time Routine" -> if (count < 3) "Collect ${3 - count} more stickers to unlock!" else "$routineName is locked."
            "School Routine"     -> if (count < 6) "Collect ${6 - count} more stickers to unlock!" else "$routineName is locked."
            else                 -> "$routineName is locked."
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun routineId(name: String) = when (name) {
        "Morning Routine"    -> 0
        "Local-Time Routine" -> 1
        "School Routine"     -> 2
        else                 -> 0
    }

    private fun navigateToDefaultActivity(routineName: String) {
        startActivity(Intent(this, ActivitySequenceUnderActivity::class.java).apply {
            putExtra("SELECTED_ROUTINE_ID", routineId(routineName))
        })
    }

    companion object {
        fun Int.dpToPx(context: Context): Int =
            (this * context.resources.displayMetrics.density).toInt()
    }
}