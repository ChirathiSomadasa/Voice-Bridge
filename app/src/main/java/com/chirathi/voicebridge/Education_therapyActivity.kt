package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions


class Education_therapyActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var disorderType: String? = null // Store disorder type globally
    private var disorderSeverity: String? = null
    private lateinit var recommender: Edu_TaskRecommender
    companion object {
        private const val EXTRA_TEXT_DESCRIPTION = "TEXT_DESCRIPTION"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_education_therapy)

        // Initialize AI Model (TFLite task recommender)
        try {
            recommender = Edu_TaskRecommender(this)
            Toast.makeText(this, "✅ AI Model loaded successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
//            Toast.makeText(this, "⚠️ AI Model error: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // Card references
        val cardRecommend: CardView = findViewById(R.id.card_recommend)
//        val cardAge1: CardView = findViewById(R.id.card_age1)
//        val cardAge2: CardView = findViewById(R.id.card_age2)
//        val cardAge3: CardView = findViewById(R.id.card_age3)
//        val cardAge4: CardView = findViewById(R.id.card_age4)
//        val cardAge5: CardView = findViewById(R.id.card_age5)
//        val cardProgress: CardView = findViewById(R.id.card_progress)
        val cardChatbot: CardView = findViewById(R.id.card_chatbot)
        
        val btnRecommend: Button = findViewById(R.id.btn_recommend)
        val btnChatbot: Button = findViewById(R.id.btn_chatbot)
//        val checkProgress: Button = findViewById(R.id.check_progress)
        val backButton: ImageView = findViewById(R.id.back)

        val etTextDescription: EditText = findViewById(R.id.etTextDescription)
        val btnTextRecommend: Button = findViewById(R.id.btnTextRecommend)
        btnTextRecommend.setOnClickListener {
            val description = etTextDescription.text.toString().trim()
            if (description.isEmpty()) {
                Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            navigateToTextRecommendations(description)
        }
        // Age card clicks
//        cardAge1.setOnClickListener {
//            navigateToSubjectsActivity("6")
//        }
//        cardAge2.setOnClickListener {
//            navigateToSubjectsActivity("7")
//        }
//        cardAge3.setOnClickListener {
//            navigateToSubjectsActivity("8")
//        }
//        cardAge4.setOnClickListener {
//            navigateToSubjectsActivity("9")
//        }
//        cardAge5.setOnClickListener {
//            navigateToSubjectsActivity("10")
//        }
        
        // Recommendation card and button clicks
        cardRecommend.setOnClickListener {
            fetchUserAgeAndNavigate()
        }

        btnRecommend.setOnClickListener {
            fetchUserAgeAndNavigate()
        }
        
        // Progress card click
//        cardProgress.setOnClickListener {
//            // Navigate to progress screen
//            Toast.makeText(this, "📊 Opening Progress Dashboard...", Toast.LENGTH_SHORT).show()
//        }
        
        // Chatbot card click
        cardChatbot.setOnClickListener {
            val intent = Intent(this, ChatbotActivity::class.java)
            startActivity(intent)
        }

        btnChatbot.setOnClickListener {
            val intent = Intent(this, ChatbotActivity::class.java)
            startActivity(intent)
        }
        
//        checkProgress.setOnClickListener {
//            Toast.makeText(this, "📊 Opening Progress Dashboard...", Toast.LENGTH_SHORT).show()
//        }

        backButton.setOnClickListener {
            finish()
        }

        validateDisorderSelection()
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }


    private fun validateDisorderSelection() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists() &&
                        document.getString("disorderType") != null &&
                        document.getString("disorderSeverity") != null) {
                        // Both disorder and severity already exist
                        disorderType = document.getString("disorderType")
                        disorderSeverity = document.getString("disorderSeverity")
                        Toast.makeText(
                            this,
                            "Disorder: $disorderType ($disorderSeverity)",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Show disorder selection bottom sheet
                        showDisorderSelectionBottomSheet(userId)
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error fetching user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not signed in!", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showDisorderSelectionBottomSheet(userId: String) {
        val disorderBottomSheet = DisorderSelectionBottomSheet { disorder, severity ->
            saveDisorderToFirestore(userId, disorder, severity)
        }
        disorderBottomSheet.isCancelable = false // Prevent dismissing without selection
        disorderBottomSheet.show(supportFragmentManager, "DisorderSelectionBottomSheet")
    }


    private fun saveDisorderToFirestore(userId: String, disorder: String, severity: String) {
        val disorderData = mapOf(
            "disorderType" to disorder,
            "disorderSeverity" to severity
        )

        db.collection("users").document(userId)
            .set(disorderData, SetOptions.merge())
            .addOnSuccessListener {
                disorderType = disorder
                disorderSeverity = severity
                Toast.makeText(
                    this,
                    "Saved: $disorder ($severity)",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Failed to save: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    private fun navigateToSubjectsActivity(ageGroup: String) {
        if (disorderType == null || disorderSeverity == null) {
            Toast.makeText(this, "Disorder type not set!", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, Education_subjects_Activity::class.java)
        intent.putExtra("AGE_GROUP", ageGroup)
        intent.putExtra("DISORDER_TYPE", disorderType)
        intent.putExtra("DISORDER_SEVERITY", disorderSeverity)
        startActivity(intent)
    }


    private fun fetchUserAgeAndNavigate() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "User not signed in!", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading message
        Toast.makeText(this, "🤖 Getting AI recommendations...", Toast.LENGTH_SHORT).show()

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Get the age field from Firestore
                    val userAge = document.getString("age")

                    if (userAge != null && userAge.isNotEmpty()) {
                        // Navigate to Flask API recommendations
                        navigateToAIRecommendations(userAge)
                    } else {
                        Toast.makeText(
                            this,
                            "Age not found in profile. Please select manually.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "User profile not found. Please register first.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error fetching age: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /**
     * Navigate to AI recommendations using Flask API
     * This displays beautiful card UI with all AI-generated therapy tasks
     */
    private fun navigateToAIRecommendations(ageString: String) {
        if (disorderType == null) {
            Toast.makeText(this, "Disorder type not set!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val ageInt = ageString.toIntOrNull() ?: 6
            
            // Navigate to AITherapyTasksActivity
            val intent = Intent(this, AITherapyTasksActivity::class.java)
            intent.putExtra("AGE", ageInt)
            intent.putExtra("DISORDER", disorderType)
            startActivity(intent)

        } catch (e: Exception) {
            Toast.makeText(this, "Navigation Error: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }


//    private fun navigateToRecommendListActivity(ageGroup: String) {
//        if (disorderType == null || disorderSeverity == null) {
//            Toast.makeText(this, "Disorder type not set!", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val intent = Intent(this, Edu_LessonListActivity::class.java)
//        intent.putExtra("AGE_GROUP", ageGroup)
//        intent.putExtra("DISORDER_TYPE", disorderType)
//        intent.putExtra("DISORDER_SEVERITY", disorderSeverity)
//        startActivity(intent)
//    }




//     * Navigate to lesson list with AI recommendation. This function uses the ML model to predict the best subject
    private fun navigateToRecommendListActivity(ageGroup: String) {
    if (disorderType == null || disorderSeverity == null) {
        Toast.makeText(this, "Disorder type not set!", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        // Step 1: Encode inputs for ML model
        val ageInt = ageGroup.toIntOrNull() ?: 6
        val disorderIndex = encodeDisorder(disorderType!!)
        val severityIndex = encodeSeverity(disorderSeverity!!)

        // Step 2: Get AI prediction (4 features: age, disorder, severity, subject)
        val (predictedIndex, confidence) = recommender.predict(
            age = ageInt,
            disorderType = disorderIndex,
            severity = severityIndex,
            subject = 0  // Default to Math (0), can be made dynamic later
        )

        // Step 3: Decode prediction to subject name
        val recommendedSubject = decodeSubject(predictedIndex)

        // Step 4: Show AI recommendation to user
        Toast.makeText(
            this,
            "🤖 AI Recommends: $recommendedSubject\nConfidence: ${(confidence * 100).toInt()}%",
            Toast.LENGTH_LONG
        ).show()

        // Step 5: Navigate to recommended lessons
        val intent = Intent(this, Edu_LessonListActivity::class.java)
        intent.putExtra("AGE_GROUP", ageGroup)
        intent.putExtra("SUBJECT", recommendedSubject)
        intent.putExtra("DISORDER_TYPE", disorderType)
        intent.putExtra("DISORDER_SEVERITY", disorderSeverity)
        intent.putExtra("IS_AI_RECOMMENDED", true)
        intent.putExtra("AI_CONFIDENCE", confidence)
        startActivity(intent)

    } catch (e: Exception) {
        Toast.makeText(this, "AI Error: ${e.message}", Toast.LENGTH_SHORT).show()
        e.printStackTrace()

        // Fallback: Navigate without AI recommendation
        navigateToSubjectsActivity(ageGroup)
    }
}


//     * Encode disorder type to integer for ML model.  Must match the encoding used during model training
    private fun encodeDisorder(disorder: String): Int {
        return when {
            disorder.contains("ASD", ignoreCase = true) ||
                    disorder.contains("Autism", ignoreCase = true) -> 0

            disorder.contains("Articulation", ignoreCase = true) -> 1

            disorder.contains("Down", ignoreCase = true) ||
                    disorder.contains("DS", ignoreCase = true) -> 2

            disorder.contains("Phonological", ignoreCase = true) -> 3

            disorder.contains("Speech Delay", ignoreCase = true) -> 4

            else -> 0 // Default to ASD if unknown
        }
    }


//    Encode severity level to integer for ML model
    private fun encodeSeverity(severity: String): Int {
        return when (severity) {
            "Mild" -> 0
            "Moderate" -> 1
            "Severe" -> 2
            else -> 1 // Default to Moderate
        }
    }


//     * Decode ML model output to subject name. 
    // IMPORTANT: These must match EXACTLY with the "subject" field in lessons04.json
    private fun decodeSubject(predictedIndex: Int): String {
        // Map based on actual subjects in lessons04.json
        val subjects = listOf(
            "General",         // 0 - General activities
            "Basic Math",      // 1 - Basic math concepts
            "Speech",          // 2 - Speech therapy
            "Reading",         // 3 - Reading activities
            "General",         // 4 - Fallback to General
            "Basic Math",      // 5 - More math
            "Speech",          // 6 - More speech
            "Reading"          // 7 - More reading
        )

        // Safely get subject with fallback to General
        return subjects.getOrElse(predictedIndex) { "General" }
    }


    private fun navigateToTextRecommendations(description: String) {
        val intent = Intent(this, AITherapyTasksActivity::class.java).apply {
            putExtra(EXTRA_TEXT_DESCRIPTION, description)
            // Optional: still pass disorder for display if you want
            putExtra("DISORDER", disorderType)
        }
        startActivity(intent)
    }

}
