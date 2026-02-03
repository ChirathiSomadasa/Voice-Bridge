package com.chirathi.voicebridge

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
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

class RoutineSelectionActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private val TAG = "RoutineSelectionDebug"

    // Declare views as lateinit variables
    private lateinit var morningRoutineLayout: LinearLayout
    private lateinit var localTimeRoutineLayout: LinearLayout
    private lateinit var schoolRoutineLayout: LinearLayout
    private lateinit var mainContainer: ConstraintLayout

    // SharedPreferences for sticker tracking
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routine_selection)

        // Initialize SharedPreferences
        sharedPrefs = getSharedPreferences("unlocked_routines", Context.MODE_PRIVATE)

        // Initialize views using findViewById
        morningRoutineLayout = findViewById(R.id.morningRoutineLayout)
        localTimeRoutineLayout = findViewById(R.id.localTimeRoutineLayout)
        schoolRoutineLayout = findViewById(R.id.schoolRoutineLayout)
        mainContainer = findViewById(R.id.mainContainer)

        auth = FirebaseAuth.getInstance()

        // Check for unlocked routines
        checkRoutineUnlock()

        // Set click listeners
        morningRoutineLayout.setOnClickListener {
            Log.d(TAG, "Morning Routine clicked")
            animateClick(morningRoutineLayout)
            Handler(Looper.getMainLooper()).postDelayed({
                checkAgeAndNavigate("Morning Routine", morningRoutineLayout)
            }, 100)
        }

        localTimeRoutineLayout.setOnClickListener {
            val isUnlocked = sharedPrefs.getBoolean("routine_1", false)
            if (isUnlocked) {
                Log.d(TAG, "Local-Time Routine clicked (unlocked)")
                animateClick(localTimeRoutineLayout)
                Handler(Looper.getMainLooper()).postDelayed({
                    checkAgeAndNavigate("Local-Time Routine", localTimeRoutineLayout)
                }, 100)
            } else {
                Log.d(TAG, "Local-Time Routine clicked (locked)")
                animateLocked(localTimeRoutineLayout)
                showLockedToast("Local-Time Routine")
            }
        }

        schoolRoutineLayout.setOnClickListener {
            val isUnlocked = sharedPrefs.getBoolean("routine_2", false)
            if (isUnlocked) {
                Log.d(TAG, "School Routine clicked (unlocked)")
                animateClick(schoolRoutineLayout)
                Handler(Looper.getMainLooper()).postDelayed({
                    checkAgeAndNavigate("School Routine", schoolRoutineLayout)
                }, 100)
            } else {
                Log.d(TAG, "School Routine clicked (locked)")
                animateLocked(schoolRoutineLayout)
                showLockedToast("School Routine")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh routine unlock status when returning to this activity
        checkRoutineUnlock()
    }

    private fun checkRoutineUnlock() {
        runOnUiThread {
            try {
                // Check for routine 1 (Bedtime/Local-Time Routine)
                if (sharedPrefs.getBoolean("routine_1", false)) {
                    // Update UI for local time routine
                    localTimeRoutineLayout.isEnabled = true
                    localTimeRoutineLayout.alpha = 1.0f

                    // Change background if drawable exists
                    try {
                        val unlockedBg = ContextCompat.getDrawable(this, R.drawable.rounded_button_background_unlocked)
                        if (unlockedBg != null) {
                            localTimeRoutineLayout.background = unlockedBg
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not set unlocked background: ${e.message}")
                    }

                    // Hide locked tag and show available tag
                    val lockedTag1 = findViewById<TextView>(R.id.lockedTag1)
                    val availableTag1 = findViewById<TextView>(R.id.availableTag1)

                    if (lockedTag1 != null) lockedTag1.visibility = View.GONE
                    if (availableTag1 != null) availableTag1.visibility = View.VISIBLE
                }

                // Check for routine 2 (School Routine)
                if (sharedPrefs.getBoolean("routine_2", false)) {
                    // Update UI for school routine
                    schoolRoutineLayout.isEnabled = true
                    schoolRoutineLayout.alpha = 1.0f

                    // Change background if drawable exists
                    try {
                        val unlockedBg = ContextCompat.getDrawable(this, R.drawable.rounded_button_background_unlocked)
                        if (unlockedBg != null) {
                            schoolRoutineLayout.background = unlockedBg
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not set unlocked background: ${e.message}")
                    }

                    // Hide locked tag and show available tag
                    val lockedTag2 = findViewById<TextView>(R.id.lockedTag2)
                    val availableTag2 = findViewById<TextView>(R.id.availableTag2)

                    if (lockedTag2 != null) lockedTag2.visibility = View.GONE
                    if (availableTag2 != null) availableTag2.visibility = View.VISIBLE
                }

                // Also update sticker count display
                updateStickerCountDisplay()

            } catch (e: Exception) {
                Log.e(TAG, "Error in checkRoutineUnlock: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun updateStickerCountDisplay() {
        try {
            val stickerPrefs = getSharedPreferences("sticker_progress", Context.MODE_PRIVATE)
            val stickerCount = stickerPrefs.getInt("stickers_collected", 0)

            // Update info text to show progress
            val infoText = findViewById<TextView>(R.id.infoTextView)
            if (infoText != null) {
                when {
                    stickerCount >= 6 -> {
                        infoText.text = "All routines unlocked! 🎉"
                    }
                    stickerCount >= 3 -> {
                        infoText.text = "You have $stickerCount/6 stickers. Collect ${6 - stickerCount} more for School Routine!"
                    }
                    else -> {
                        infoText.text = "You have $stickerCount/3 stickers. Collect ${3 - stickerCount} more for Bedtime Routine!"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating sticker display: ${e.message}")
        }
    }

    private fun animateClick(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun animateLocked(view: View) {
        view.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .alpha(0.7f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun showLockedToast(routineName: String) {
        val stickerPrefs = getSharedPreferences("sticker_progress", Context.MODE_PRIVATE)
        val stickerCount = stickerPrefs.getInt("stickers_collected", 0)

        val message = when (routineName) {
            "Local-Time Routine" -> {
                val needed = 3 - stickerCount
                if (needed > 0) {
                    "Collect $needed more stickers to unlock $routineName!"
                } else {
                    "$routineName is locked. Complete Morning Routine first!"
                }
            }
            "School Routine" -> {
                val needed = 6 - stickerCount
                if (needed > 0) {
                    "Collect $needed more stickers to unlock $routineName!"
                } else {
                    "$routineName is locked. Complete previous routines first!"
                }
            }
            else -> "$routineName is locked. Complete previous routines first!"
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun checkAgeAndNavigate(routineName: String, clickedView: View) {
        Log.d(TAG, "Checking age for routine: $routineName")

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d(TAG, "User not logged in")
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Show loading on the clicked view
        clickedView.alpha = 0.7f
        clickedView.isEnabled = false

        Log.d(TAG, "Fetching age for user: ${currentUser.uid}")
        Log.d(TAG, "User email: ${currentUser.email}")

        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                clickedView.alpha = 1.0f
                clickedView.isEnabled = true

                Log.d(TAG, "Document retrieved: ${document.exists()}")

                if (document.exists()) {
                    val allData = document.data
                    Log.d(TAG, "All document data: $allData")

                    val ageString = document.getString("age")
                    Log.d(TAG, "Age string from document: '$ageString'")
                    Log.d(TAG, "Age string is null: ${ageString == null}")
                    Log.d(TAG, "Age string is empty: ${ageString?.isEmpty()}")

                    val firstName = document.getString("firstName")
                    val email = document.getString("email")
                    Log.d(TAG, "First name: $firstName")
                    Log.d(TAG, "Email: $email")

                    if (ageString != null && ageString.isNotEmpty()) {
                        try {
                            val age = ageString.toInt()
                            Log.d(TAG, "Age converted to int: $age")
                            navigateBasedOnAge(age, routineName)

                        } catch (e: NumberFormatException) {
                            Log.e(TAG, "Invalid age format: $ageString", e)
                            Toast.makeText(this, "Invalid age format: $ageString", Toast.LENGTH_SHORT).show()
                            navigateToDefaultActivity(routineName)
                        }
                    } else {
                        Log.d(TAG, "Age not found or empty in document")
                        Toast.makeText(this, "Age information not found in profile", Toast.LENGTH_SHORT).show()
                        navigateToDefaultActivity(routineName)
                    }
                } else {
                    Log.d(TAG, "Document doesn't exist for user")
                    Toast.makeText(this, "User profile not found. Please complete registration.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                clickedView.alpha = 1.0f
                clickedView.isEnabled = true

                Log.e(TAG, "Failed to load user data", exception)
                Toast.makeText(this, "Error loading profile: ${exception.message}", Toast.LENGTH_SHORT).show()
                navigateToDefaultActivity(routineName)
            }
    }

    private fun navigateBasedOnAge(age: Int, routineName: String) {
        Log.d(TAG, "Navigating based on age: $age for routine: $routineName")

        // Determine routine ID based on name
        val routineId = when (routineName) {
            "Morning Routine" -> 0
            "Local-Time Routine" -> 1
            "School Routine" -> 2
            else -> 0
        }

        when (age) {
            in 6..7 -> {
                Log.d(TAG, "Showing tap guidance for age 6-7")
                // For younger kids: Tap interface
                showTapGuidance(routineId)
            }
            in 8..10 -> {
                Log.d(TAG, "Showing drag and drop guidance for age 8-10")
                // For older kids: Drag and drop interface
                showDragAndDropGuidance(routineId)
            }
            else -> {
                Log.d(TAG, "Age $age is outside 6-10 range")
                Toast.makeText(this, "Age $age is outside 6-10 range. Defaulting to simpler activity.", Toast.LENGTH_SHORT).show()
                navigateToDefaultActivity(routineName)
            }
        }
    }

    private fun showTapGuidance(routineId: Int) {
        Log.d(TAG, "Showing ASGuide_BelowActivity with tap video for routine: $routineId")
        val guidanceIntent = Intent(this, ASGuide_BelowActivity::class.java)
        guidanceIntent.putExtra("SELECTED_ROUTINE_ID", routineId)
        startActivity(guidanceIntent)
    }

    private fun showDragAndDropGuidance(routineId: Int) {
        Log.d(TAG, "Showing ASGuidanceAboveActivity with drag and drop video for routine: $routineId")
        val guidanceIntent = Intent(this, ASGuidanceAboveActivity::class.java)
        guidanceIntent.putExtra("SELECTED_ROUTINE_ID", routineId)
        startActivity(guidanceIntent)
    }

    private fun navigateToDefaultActivity(routineName: String) {
        Log.d(TAG, "Navigating to default ActivitySequenceUnderActivity for routine: $routineName")
        val intent = Intent(this, ActivitySequenceUnderActivity::class.java)

        // Pass routine ID based on name
        val routineId = when (routineName) {
            "Morning Routine" -> 0
            "Local-Time Routine" -> 1
            "School Routine" -> 2
            else -> 0
        }

        intent.putExtra("SELECTED_ROUTINE_ID", routineId)
        startActivity(intent)
    }

    companion object {
        // Helper function to convert dp to px
        fun Int.dpToPx(context: Context): Int {
            val density = context.resources.displayMetrics.density
            return (this * density).toInt()
        }
    }
}