package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routine_selection)

        // Initialize views using findViewById
        morningRoutineLayout = findViewById(R.id.morningRoutineLayout)
        localTimeRoutineLayout = findViewById(R.id.localTimeRoutineLayout)
        schoolRoutineLayout = findViewById(R.id.schoolRoutineLayout)
        mainContainer = findViewById(R.id.mainContainer)

        auth = FirebaseAuth.getInstance()

        // Set click listeners
        morningRoutineLayout.setOnClickListener {
            Log.d(TAG, "Morning Routine clicked")
            animateClick(morningRoutineLayout)
            Handler(Looper.getMainLooper()).postDelayed({
                checkAgeAndNavigate("Morning Routine", morningRoutineLayout)
            }, 100)
        }

        localTimeRoutineLayout.setOnClickListener {
            Log.d(TAG, "Local-Time Routine clicked (locked)")
            animateLocked(localTimeRoutineLayout)
            showLockedToast("Local-Time Routine")
        }

        schoolRoutineLayout.setOnClickListener {
            Log.d(TAG, "School Routine clicked (locked)")
            animateLocked(schoolRoutineLayout)
            showLockedToast("School Routine")
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
        Toast.makeText(this, "$routineName is locked. Complete Morning Routine first!", Toast.LENGTH_SHORT).show()
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
                            navigateBasedOnAge(age)

                        } catch (e: NumberFormatException) {
                            Log.e(TAG, "Invalid age format: $ageString", e)
                            Toast.makeText(this, "Invalid age format: $ageString", Toast.LENGTH_SHORT).show()
                            navigateToDefaultActivity()
                        }
                    } else {
                        Log.d(TAG, "Age not found or empty in document")
                        Toast.makeText(this, "Age information not found in profile", Toast.LENGTH_SHORT).show()
                        navigateToDefaultActivity()
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
                navigateToDefaultActivity()
            }
    }

    private fun navigateBasedOnAge(age: Int) {
        Log.d(TAG, "Navigating based on age: $age")

        when (age) {
            in 6..7 -> {
                Log.d(TAG, "Showing tap guidance for age 6-7")
                // For younger kids: Tap interface
                showTapGuidance()
            }
            in 8..10 -> {
                Log.d(TAG, "Showing drag and drop guidance for age 8-10")
                // For older kids: Drag and drop interface
                showDragAndDropGuidance()
            }
            else -> {
                Log.d(TAG, "Age $age is outside 6-10 range")
                Toast.makeText(this, "Age $age is outside 6-10 range. Defaulting to simpler activity.", Toast.LENGTH_SHORT).show()
                navigateToDefaultActivity()
            }
        }
    }

    private fun showTapGuidance() {
        Log.d(TAG, "Showing ASGuide_BelowActivity with tap video")
        val guidanceIntent = Intent(this, ASGuide_BelowActivity::class.java)
        startActivity(guidanceIntent)
        // No need for Handler delay - the OK button will navigate
    }

    private fun showDragAndDropGuidance() {
        Log.d(TAG, "Showing ASGuidanceAboveActivity with drag and drop video")
        val guidanceIntent = Intent(this, ASGuidanceAboveActivity::class.java)
        startActivity(guidanceIntent)
        // No need for Handler delay - the OK button will navigate
    }

    private fun navigateToDefaultActivity() {
        Log.d(TAG, "Navigating to default ActivitySequenceUnderActivity")
        val intent = Intent(this, ActivitySequenceUnderActivity::class.java)
        startActivity(intent)
    }
}