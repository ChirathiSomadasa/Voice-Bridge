package com.chirathi.voicebridge

import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class GameDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private val TAG = "GameDashboardDebug"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_dashboard)

        auth = FirebaseAuth.getInstance()

        val moodMatchBtn = findViewById<Button>(R.id.btn_game1)
        val myDayBtn = findViewById<Button>(R.id.btn_level2)
        val singTimeBtn = findViewById<Button>(R.id.btn_level3)
        val backBtn = findViewById<ImageView>(R.id.backGame)

        // Mood Match button click - navigate to PandaIntroActivity
        moodMatchBtn.setOnClickListener {
            val intent = Intent(this, PandaIntroActivity::class.java)
            startActivity(intent)
        }

        // My Day button click - navigate to RoutineSelectionActivity
        myDayBtn.setOnClickListener {
            val intent = Intent(this, RoutineSelectionActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.drawable.slide_in_right, R.drawable.slide_out_left)
        }

        singTimeBtn.setOnClickListener {
            val intent = Intent(this, SongSelectionActivity::class.java)
            startActivity(intent)
        }

        // Back button click
        backBtn.setOnClickListener {
            finish()
        }
    }

    private fun checkAgeAndNavigate() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Show loading
        Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show()

        Log.d(TAG, "Checking age for user: ${currentUser.uid}")
        Log.d(TAG, "User email: ${currentUser.email}")

        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                Log.d(TAG, "Document retrieved: ${document.exists()}")

                if (document.exists()) {
                    // Get all document data for debugging
                    val allData = document.data
                    Log.d(TAG, "All document data: $allData")

                    // Get age as String (matching ChildRegistrationActivity)
                    val ageString = document.getString("age")
                    Log.d(TAG, "Age string from document: '$ageString'")
                    Log.d(TAG, "Age string is null: ${ageString == null}")
                    Log.d(TAG, "Age string is empty: ${ageString?.isEmpty()}")

                    // Also check other fields to confirm document structure
                    val firstName = document.getString("firstName")
                    val email = document.getString("email")
                    Log.d(TAG, "First name: $firstName")
                    Log.d(TAG, "Email: $email")

                    if (ageString != null && ageString.isNotEmpty()) {
                        try {
                            val age = ageString.toInt()
                            Log.d(TAG, "Age converted to int: $age")

                            // Navigate based on age group
                            when (age) {
                                in 6..7 -> {
                                    Log.d(TAG, "Navigating to ActivitySequenceUnderActivity (age: $age)")
                                    // Age 6-7: Navigate to ActivitySequenceUnderActivity
                                    val intent = Intent(this, ActivitySequenceUnderActivity::class.java)
                                    startActivity(intent)
                                }
                                in 8..10 -> {
                                    Log.d(TAG, "Navigating to ActivitySequenceOverActivity (age: $age)")
                                    // Age 8-10: Navigate to ActivitySequenceOverActivity
                                    val intent = Intent(this, ActivitySequenceOverActivity::class.java)
                                    startActivity(intent)
                                }
                                else -> {
                                    Log.d(TAG, "Age $age is outside 6-10 range")
                                    Toast.makeText(this, "Age $age is outside 6-10 range. Defaulting to simpler activity.", Toast.LENGTH_SHORT).show()
                                    // Default to simpler activity for safety
                                    val intent = Intent(this, ActivitySequenceUnderActivity::class.java)
                                    startActivity(intent)
                                }
                            }
                        } catch (e: NumberFormatException) {
                            Log.e(TAG, "Invalid age format: $ageString", e)
                            // Age is not a valid number
                            Toast.makeText(this, "Invalid age format: $ageString", Toast.LENGTH_SHORT).show()
                            // Default to simpler activity
                            val intent = Intent(this, ActivitySequenceUnderActivity::class.java)
                            startActivity(intent)
                        }
                    } else {
                        Log.d(TAG, "Age not found or empty in document")
                        // Age not found in document
                        Toast.makeText(this, "Age information not found in profile", Toast.LENGTH_SHORT).show()
                        // Default to simpler activity
                        val intent = Intent(this, ActivitySequenceUnderActivity::class.java)
                        startActivity(intent)
                    }
                } else {
                    Log.d(TAG, "Document doesn't exist for user")
                    // Document doesn't exist
                    Toast.makeText(this, "User profile not found. Please complete registration.", Toast.LENGTH_SHORT).show()
                    // Go to login
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to load user data", exception)
                // Firestore fetch failed
                Toast.makeText(this, "Error loading profile: ${exception.message}", Toast.LENGTH_SHORT).show()
                // Default to simpler activity
                val intent = Intent(this, ActivitySequenceUnderActivity::class.java)
                startActivity(intent)
            }
    }
}