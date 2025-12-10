package com.chirathi.voicebridge

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MMScoreboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mmscoreboard)

        // Get the data passed from the game activity
        val correctAnswers = intent.getIntExtra("CORRECT_ANSWERS", 0)
        val totalRounds = intent.getIntExtra("TOTAL_ROUNDS", 5)
        val score = intent.getIntExtra("SCORE", 0)

        // Find the score text view
        val scoreText = findViewById<TextView>(R.id.scoreText)

        // Set the score in the format "correct/total"
        scoreText.text = "$correctAnswers/$totalRounds"

        // Optional: You could also show the total score points if needed
        // val yourScoreText = findViewById<TextView>(R.id.yourScoreText)
        // yourScoreText.text = "Your Score: $score points"

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}