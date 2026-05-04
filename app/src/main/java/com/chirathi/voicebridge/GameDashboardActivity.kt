package com.chirathi.voicebridge

import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class GameDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var prefs: SharedPreferences
    private val db  = Firebase.firestore
    private val TAG = "GameDashboardDebug"

    private lateinit var settingsPrefs: SharedPreferences
    private lateinit var audioManager: AudioManager

    private val KEY_CALM_MUSIC  = "pref_calm_music_enabled"
    private val KEY_CALM_VOLUME = "pref_calm_music_volume"

    private var currentUserAge: Int = 6

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_dashboard)

        auth  = FirebaseAuth.getInstance()
        prefs = getSharedPreferences("voicebridge_prefs", MODE_PRIVATE)

        // ── Restore ChildSession in case process was killed ──────────────────
        // This is the safety net. Primary init happens in fetchAgeAndLaunch().
        if (!ChildSession.isInitialized) {
            ChildSession.restore(this)
            Log.d(TAG, "ChildSession restored from prefs: childId=${ChildSession.childId} age=${ChildSession.age}")
        }

        val moodMatchBtn = findViewById<Button>(R.id.btn_game1)
        val myDayBtn     = findViewById<Button>(R.id.btn_level2)
        val singTimeBtn  = findViewById<Button>(R.id.btn_level3)
        val backBtn      = findViewById<ImageView>(R.id.backGame)

        moodMatchBtn.setOnClickListener { handleMoodMatchClick() }

        myDayBtn.setOnClickListener {
            // ── Pass age to ActivitySequenceUnder/Over via RoutineSelection ──
            startActivity(Intent(this, RoutineSelectionActivity::class.java).apply {
                putExtra("USER_AGE", ChildSession.age)
            })
            overridePendingTransition(R.drawable.slide_in_right, R.drawable.slide_out_left)
        }

        singTimeBtn.setOnClickListener {
            // ── Song selection: child picks freely, age passed for tier calc ─
            startActivity(Intent(this, SongSelectionActivity::class.java).apply {
                putExtra("USER_AGE",    ChildSession.age)
                putExtra("CHILD_ID",    ChildSession.childId)
                putExtra("AGE_GROUP",   ChildSession.ageGroup)
            })
        }

        backBtn.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }

        setupGameSettings()
        prefetchUserAge()
    }

    override fun onResume() {
        super.onResume()
        CalmMusicManager.onActivityResume(this)
        // Re-log session state on every resume for debugging
        ChildSession.log()
    }

    override fun onPause() {
        super.onPause()
        CalmMusicManager.onActivityPause()
    }

    private fun prefetchUserAge() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val age = document.getString("age")?.toIntOrNull() ?: 6
                currentUserAge = age
                // ── Initialize ChildSession from Firestore age ───────────────
                ChildSession.set(this, uid, age)
                Log.d(TAG, "prefetchUserAge: age=$age → ChildSession initialized")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "prefetchUserAge failed: ${e.message}")
            }
    }

    private fun handleMoodMatchClick() {
        val uid          = auth.currentUser?.uid ?: return
        val introKey     = "panda_intro_shown_$uid"
        val hasSeenIntro = prefs.getBoolean(introKey, false)

        if (!hasSeenIntro) {
            prefs.edit().putBoolean(introKey, true).apply()
            fetchAgeAndLaunch(viaPandaIntro = true)
        } else {
            fetchAgeAndLaunch(viaPandaIntro = false)
        }
    }

    private fun fetchAgeAndLaunch(viaPandaIntro: Boolean) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish(); return
        }

        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val age = document.getString("age")?.toIntOrNull() ?: 6
                currentUserAge = age

                // ── PRIMARY ChildSession initialization point ────────────────
                // This runs every time the child launches a game from the dashboard.
                // LoginActivity is not modified — this is the equivalent hook.
                ChildSession.set(this, currentUser.uid, age)
                Log.d(TAG, "fetchAgeAndLaunch: age=$age childId=${ChildSession.childId} via=${ if (viaPandaIntro) "panda" else "direct"}")

                if (viaPandaIntro) {
                    startActivity(Intent(this, PandaIntroActivity::class.java).apply {
                        putExtra("AGE_GROUP", age)
                    })
                } else {
                    launchMoodMatch(age)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore error: ${e.message}")
                Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show()
                val fallbackAge = if (ChildSession.isInitialized) ChildSession.age else 6
                if (viaPandaIntro) {
                    startActivity(Intent(this, PandaIntroActivity::class.java).apply {
                        putExtra("AGE_GROUP", fallbackAge) })
                } else {
                    launchMoodMatch(fallbackAge)
                }
            }
    }

    fun launchMoodMatch(age: Int) {
        startActivity(Intent(this, MoodMatchSevenDownActivity::class.java).apply {
            putExtra("AGE_GROUP", age)
        })
    }

    private fun setupGameSettings() {
        settingsPrefs = getSharedPreferences("game_settings_prefs", MODE_PRIVATE)
        audioManager  = getSystemService(AUDIO_SERVICE) as AudioManager
        CalmMusicManager.init(this)
        findViewById<FrameLayout>(R.id.btnGameSettings).setOnClickListener {
            showGameSettingsDialog()
        }
    }

    private fun showGameSettingsDialog() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.activity_dialog_game_settings, null)
            val switchCalmMusic = dialogView.findViewById<SwitchCompat>(R.id.switchCalmMusic)
            val seekVolume      = dialogView.findViewById<SeekBar>(R.id.seekMasterVolume)

            val hasCalmResource = try {
                resources.openRawResourceFd(R.raw.calm_background) != null
            } catch (_: Exception) { false }

            switchCalmMusic.isChecked = settingsPrefs.getBoolean(KEY_CALM_MUSIC, false)
            if (!hasCalmResource) {
                switchCalmMusic.isChecked = false
                switchCalmMusic.isEnabled = false
                switchCalmMusic.alpha     = 0.4f
            }

            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            seekVolume.max      = maxVol
            seekVolume.progress = curVol

            seekVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, p, 0)
                    CalmMusicManager.applySystemVolume(this@GameDashboardActivity)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

            val builder = android.app.AlertDialog.Builder(this)
                .setTitle("Game Sound Settings")
                .setView(dialogView)
                .setPositiveButton("Done") { _, _ ->
                    val musicOn = switchCalmMusic.isChecked && hasCalmResource
                    settingsPrefs.edit()
                        .putBoolean(KEY_CALM_MUSIC,  musicOn)
                        .putInt(KEY_CALM_VOLUME,     seekVolume.progress)
                        .apply()
                    CalmMusicManager.applySettings(this)
                }
                .setNegativeButton("Cancel") { _, _ ->
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, curVol, 0)
                    CalmMusicManager.applySystemVolume(this)
                }

            if (currentUserAge in 8..10) {
                builder.setNeutralButton("Customize Routines") { _, _ ->
                    startActivityForResult(
                        Intent(this, ParentSequenceSettingsActivity::class.java),
                        REQUEST_CUSTOMIZE_ROUTINES
                    )
                }
            }
            builder.show()
        } catch (e: Exception) {
            Log.e(TAG, "showGameSettingsDialog failed: ${e.message}", e)
            Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQUEST_CUSTOMIZE_ROUTINES = 1001
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CUSTOMIZE_ROUTINES) showGameSettingsDialog()
    }
}