package com.chirathi.voicebridge

import android.content.Intent
import android.content.SharedPreferences
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
    private lateinit var prefs: SharedPreferences
    private val db = Firebase.firestore
    private val TAG = "GameDashboardDebug"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_dashboard)

        auth = FirebaseAuth.getInstance()
        prefs = getSharedPreferences("voicebridge_prefs", MODE_PRIVATE)

        val moodMatchBtn = findViewById<Button>(R.id.btn_game1)
        val myDayBtn     = findViewById<Button>(R.id.btn_level2)
        val singTimeBtn  = findViewById<Button>(R.id.btn_level3)
        val backBtn      = findViewById<ImageView>(R.id.backGame)

        moodMatchBtn.setOnClickListener { handleMoodMatchClick() }

        myDayBtn.setOnClickListener {
            startActivity(Intent(this, RoutineSelectionActivity::class.java))
            overridePendingTransition(R.drawable.slide_in_right, R.drawable.slide_out_left)
        }

        singTimeBtn.setOnClickListener {
            startActivity(Intent(this, SongSelectionActivity::class.java))
        }

        backBtn.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    /**
     * First time ever → show PandaIntroActivity (which will then navigate to the game).
     * Every time after → skip intro and go straight to MoodMatchSevenDownActivity.
     */
    private fun handleMoodMatchClick() {
        val uid = auth.currentUser?.uid ?: return
        val introKey = "panda_intro_shown_$uid"
        val hasSeenIntro = prefs.getBoolean(introKey, false)

        if (!hasSeenIntro) {
            // Mark as seen so it never shows again for this user
            prefs.edit().putBoolean(introKey, true).apply()
            // PandaIntroActivity must navigate to MoodMatchSevenDownActivity when done
            // (pass age via intent from there, or fetch age inside PandaIntroActivity)
            fetchAgeAndLaunch(viaPandaIntro = true)
        } else {
            fetchAgeAndLaunch(viaPandaIntro = false)
        }
    }

    /**
     * Fetches the child's age from Firestore, then launches either PandaIntroActivity
     * (first time) or MoodMatchSevenDownActivity directly (returning user).
     *
     * Both age groups now go to MoodMatchSevenDownActivity.
     * Age is passed as an extra so the activity can adapt its UI.
     */
    private fun fetchAgeAndLaunch(viaPandaIntro: Boolean) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val ageString = document.getString("age")
                val age = ageString?.toIntOrNull() ?: 6   // default to 6 if missing

                Log.d(TAG, "Age fetched: $age  viaPandaIntro=$viaPandaIntro")

                if (viaPandaIntro) {
                    // Let PandaIntroActivity carry the age so it can pass it onward
                    val intent = Intent(this, PandaIntroActivity::class.java)
                    intent.putExtra("AGE_GROUP", age)
                    startActivity(intent)
                } else {
                    launchMoodMatch(age)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore error: ${e.message}")
                Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show()
                // Fallback: launch with default age
                if (viaPandaIntro) {
                    startActivity(Intent(this, PandaIntroActivity::class.java).apply {
                        putExtra("AGE_GROUP", 6)
                    })
                } else {
                    launchMoodMatch(6)
                }
            }
    }

    fun launchMoodMatch(age: Int) {
        val intent = Intent(this, MoodMatchSevenDownActivity::class.java)
        intent.putExtra("AGE_GROUP", age)
        startActivity(intent)
    }
}