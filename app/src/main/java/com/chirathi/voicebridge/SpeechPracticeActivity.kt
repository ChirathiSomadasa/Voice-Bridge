package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class SpeechPracticeActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_practice)

        // 1. Existing Back Button
        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }

        // 2. Existing Level Buttons
        val level1 = findViewById<Button>(R.id.btn_level1)
        level1.setOnClickListener {
            val intent = Intent(this, SpeechLevel1Activity::class.java)
            startActivity(intent)
        }

        val level2 = findViewById<Button>(R.id.btn_level2)
        level2.setOnClickListener {
            val intent = Intent(this, SpeechLevel2Activity::class.java)
            startActivity(intent)
        }

        val level3 = findViewById<Button>(R.id.btn_level3)
        level3.setOnClickListener {
            val intent = Intent(this, SpeechLevel3Activity::class.java)
            startActivity(intent)
        }

        // 3. Existing Check Progress Button (Child View)
        val progress = findViewById<Button>(R.id.check_progress)
        progress.setOnClickListener {
            val intent = Intent(this, SpeechProgressActivity::class.java)
            startActivity(intent)
        }

        // Parent Zone with Biometric Lock

        // Use the ID 'cardParentZone' from your updated XML
        val parentZoneBtn = findViewById<View>(R.id.cardParentZone)

        parentZoneBtn.setOnClickListener {
            showBiometricPrompt()
        }
    }

    private fun showBiometricPrompt() {
        // Initialize Biometric Prompt
        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {

                // Success: Fingerprint matched!
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Access Granted!", Toast.LENGTH_SHORT).show()

                    // Navigate to ReportActivity (Parent Zone)
                    val intent = Intent(this@SpeechPracticeActivity, ParentReportActivity::class.java)
                    startActivity(intent)
                }

                // Error: Canceled or too many attempts
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Authentication Error: $errString", Toast.LENGTH_SHORT).show()
                }

                // Failed: Fingerprint didn't match
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Fingerprint not recognized.", Toast.LENGTH_SHORT).show()
                }
            })

        // Configure the Dialog Box
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Parental Access Required")
            .setSubtitle("Scan your fingerprint to view the progress report")
            .setNegativeButtonText("Cancel")
            .build()

        // Check hardware support before authenticating
        val biometricManager = BiometricManager.from(this)

        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Hardware works, show the prompt
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(this, "No biometric features available on this device.", Toast.LENGTH_LONG).show()
                // Fallback to password or just open for testing
                startActivity(Intent(this, ParentReportActivity::class.java))
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(this, "Biometric sensors are currently unavailable.", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // User has hardware but didn't set up a fingerprint in Android Settings
                Toast.makeText(this, "No fingerprint saved. Please set up security in settings.", Toast.LENGTH_LONG).show()
                // Allow access for now since they can't scan
                startActivity(Intent(this, ParentReportActivity::class.java))
            }
        }
    }
}