package com.chirathi.voicebridge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panda_intro)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Initialize views
        videoView = findViewById(R.id.videoView)
        skipButton = findViewById(R.id.skipButton)
        adventureText = findViewById(R.id.adventureText)

        // Initialize VideoView WITHOUT any MediaController or controls
        val videoPath = "android.resource://" + packageName + "/" + R.raw.game_intro
        val uri = Uri.parse(videoPath)
        videoView.setVideoURI(uri)

        // No MediaController - no playback controls at all
        // No screen tap listener - only skip button works

        // Load user data and set adventure text
        loadUserData()

        // Start playing video
        videoView.start()

        // Set click listener ONLY on skip button
        skipButton.setOnClickListener {
            checkUserAgeAndNavigate()
        }

        // When video ends, automatically check age and navigate
        videoView.setOnCompletionListener {
            checkUserAgeAndNavigate()
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Fetch user data from Firestore to get first name
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstName = document.getString("firstName")
                        // Set adventure text with user's first name on two lines
                        if (!firstName.isNullOrEmpty()) {
                            adventureText.text = "$firstName's\nAdventure"
                        } else {
                            adventureText.text = "Your\nAdventure"
                        }
                    } else {
                        adventureText.text = "Your\nAdventure"
                    }
                }
                .addOnFailureListener {
                    adventureText.text = "Your\nAdventure"
                }
        } else {
            adventureText.text = "Your\nAdventure"
        }
    }

    private fun checkUserAgeAndNavigate() {
        // Stop the video when skipping or navigating
        videoView.stopPlayback()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Fetch user data from Firestore to get age
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val ageString = document.getString("age")
                        if (ageString != null) {
                            try {
                                val age = ageString.toInt()

                                // Navigate based on age group
                                when {
                                    age <= 7 -> {
                                        // Age 7 or below: Go to MoodMatchSevenDownActivity
                                        val intent = Intent(this, MoodMatchSevenDownActivity::class.java)
                                        startActivity(intent)
                                    }
                                    else -> {
                                        // Age 8 and above: Go to MoodMatchSevenUpActivity
                                        val intent = Intent(this, MoodMatchSevenUpActivity::class.java)
                                        startActivity(intent)
                                    }
                                }
                                finish()
                            } catch (e: NumberFormatException) {
                                goToDefaultActivity()
                            }
                        } else {
                            goToDefaultActivity()
                        }
                    } else {
                        goToDefaultActivity()
                    }
                }
                .addOnFailureListener { exception ->
                    goToDefaultActivity()
                }
        } else {
            goToDefaultActivity()
        }
    }

    private fun goToDefaultActivity() {
        // Default navigation if age check fails
        // Default to younger age group
        val intent = Intent(this, MoodMatchSevenDownActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Resume video playback when activity resumes
        if (!videoView.isPlaying) {
            videoView.start()
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause video when activity pauses
        videoView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release video resources
        videoView.stopPlayback()
    }
}