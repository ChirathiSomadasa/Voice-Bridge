package com.chirathi.voicebridge

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ParentReportActivity : AppCompatActivity() {

    // --- UI Components ---
    private lateinit var btnBack: ImageView

    private lateinit var pbLevel1: ProgressBar
    private lateinit var pbLevel2: ProgressBar
    private lateinit var pbLevel3: ProgressBar

    private lateinit var tvLevel1Progress: TextView
    private lateinit var tvLevel2Progress: TextView
    private lateinit var tvLevel3Progress: TextView
    private lateinit var tvCourseNote: TextView

    // Card views for Average Accuracy
    private lateinit var cardLetters: View
    private lateinit var cardWords: View
    private lateinit var cardSentences: View

    // Weakness List & Empty State
    private lateinit var rvInsights: RecyclerView
    private lateinit var tvEmptyState: TextView

    private lateinit var btnDownloadPdf: MaterialButton
    private lateinit var btnShareReport: MaterialButton

    // --- Data & Firebase ---
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val insightList = ArrayList<InsightItem>()
    private lateinit var insightAdapter: InsightAdapter

    private var scoreL1 = 0
    private var scoreL2 = 0
    private var scoreL3 = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_report)

        // --- DEBUG: SHA-1 Logging ---
        try {
            val info = packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md = java.security.MessageDigest.getInstance("SHA-1")
                md.update(signature.toByteArray())
                val digest = md.digest()
                val hexString = StringBuilder()
                for (b in digest) hexString.append(String.format("%02X:", b))
                if (hexString.isNotEmpty()) hexString.setLength(hexString.length - 1)
                android.util.Log.e("MY_REAL_SHA1", "SHA-1: $hexString")
            }
        } catch (e: Exception) { e.printStackTrace() }

        initViews()
        setupRecyclerView()

        loadCourseCompletion()
        loadAverageScores()
        loadAnalysisData()

        btnBack.setOnClickListener { finish() }

        btnDownloadPdf.setOnClickListener {
            val file = generatePDF()
            if (file != null) Toast.makeText(this, "PDF Saved!", Toast.LENGTH_LONG).show()
        }

        btnShareReport.setOnClickListener {
            val file = generatePDF()
            if (file != null) sharePdf(file)
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)

        pbLevel1 = findViewById(R.id.pbLevel1)
        pbLevel2 = findViewById(R.id.pbLevel2)
        pbLevel3 = findViewById(R.id.pbLevel3)

        tvLevel1Progress = findViewById(R.id.tvLevel1Progress)
        tvLevel2Progress = findViewById(R.id.tvLevel2Progress)
        tvLevel3Progress = findViewById(R.id.tvLevel3Progress)
        tvCourseNote = findViewById(R.id.tvCourseNote)

        cardLetters = findViewById(R.id.cardLetters)
        cardWords = findViewById(R.id.cardWords)
        cardSentences = findViewById(R.id.cardSentences)

        rvInsights = findViewById(R.id.rvInsights)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        btnDownloadPdf = findViewById(R.id.btnDownloadPdf)
        btnShareReport = findViewById(R.id.btnShareReport)
    }

    private fun setupRecyclerView() {
        rvInsights.layoutManager = LinearLayoutManager(this)
        insightAdapter = InsightAdapter(insightList)
        rvInsights.adapter = insightAdapter
    }

    // --- 1. COURSE COMPLETION ---
    private fun loadCourseCompletion() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("student_progress").document(userId).get()
            .addOnSuccessListener { document ->
                val batch1 = document.getLong("level1_batch")?.toInt() ?: 0
                val batch2 = document.getLong("level2_batch")?.toInt() ?: 0
                val batch3 = document.getLong("level3_batch")?.toInt() ?: 0
                updateProgressUI(batch1, batch2, batch3)
            }
            .addOnFailureListener {
                val prefs = getSharedPreferences("VoiceBridgePrefs", Context.MODE_PRIVATE)
                val batch1 = prefs.getInt("SAVED_BATCH_LEVEL_1_$userId", 0)
                val batch2 = prefs.getInt("SAVED_BATCH_LEVEL_2_$userId", 0)
                val batch3 = prefs.getInt("SAVED_BATCH_LEVEL_3_$userId", 0)
                updateProgressUI(batch1, batch2, batch3)
            }
    }

    private fun updateProgressUI(batch1: Int, batch2: Int, batch3: Int) {
        val progress1 = (batch1 * 5).coerceAtMost(26)
        pbLevel1.max = 26
        pbLevel1.progress = progress1
        tvLevel1Progress.text = "Level 1: Letters ($progress1/26)"

        val progress2 = (batch2 * 5).coerceAtMost(25)
        pbLevel2.max = 25
        pbLevel2.progress = progress2
        tvLevel2Progress.text = "Level 2: Words ($progress2/25)"

        val progress3 = (batch3 * 3).coerceAtMost(30)
        pbLevel3.max = 30
        pbLevel3.progress = progress3
        tvLevel3Progress.text = "Level 3: Sentences ($progress3/30)"

        if (progress1 == 0 && progress2 == 0 && progress3 == 0) {
            tvCourseNote.visibility = View.VISIBLE
        } else {
            tvCourseNote.visibility = View.GONE
        }
    }

    // --- 2. AVERAGE SCORES ---
    private fun loadAverageScores() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("student_speech_progress").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    scoreL1 = doc.getDouble("overall_letter_progress")?.toInt() ?: 0
                    scoreL2 = doc.getDouble("overall_word_progress")?.toInt() ?: 0
                    scoreL3 = doc.getDouble("overall_sentence_progress")?.toInt() ?: 0

                    updateScoreCard(cardLetters, scoreL1, "Letters")
                    updateScoreCard(cardWords, scoreL2, "Words")
                    updateScoreCard(cardSentences, scoreL3, "Sentences")
                }
            }
    }

    private fun updateScoreCard(view: View, score: Int, label: String) {
        // Find TextViews inside the card
        val tvScore = view.findViewById<TextView>(R.id.tvScore)
        val tvLabel = view.findViewById<TextView>(R.id.tvLabel)

        // The 'view' passed here IS the CardView itself due to the <include> tag.
        // We cast it directly instead of using findViewById to look inside itself.
        val mainCard = view as androidx.cardview.widget.CardView

        tvScore.text = "$score%"
        tvLabel.text = label

        // Dynamic Color Logic
        val (bgColor, textColor) = when {
            score >= 75 -> Pair("#E8F5E9", "#2E7D32") // Light Green
            score >= 50 -> Pair("#FFF3E0", "#EF6C00") // Light Orange
            else -> Pair("#FFEBEE", "#C62828")        // Light Red
        }

        mainCard.setCardBackgroundColor(Color.parseColor(bgColor))
        tvScore.setTextColor(Color.parseColor(textColor))
    }

    // --- 3. ANALYSIS WEAKNESSES ---
    private fun loadAnalysisData() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("pronunciation_feedback")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                insightList.clear()
                val allHistory = ArrayList<FeedbackItem>()

                for (doc in documents) {
                    try {
                        val content = doc.getString("content") ?: ""
                        val score = doc.getLong("score")?.toInt() ?: 0
                        val rawType = doc.getString("item_type")
                        val type = rawType ?: if (content.contains(" ")) "sentence" else "word"
                        val timestamp = doc.getDate("timestamp") ?: Date(0)

                        allHistory.add(FeedbackItem(content, score, type, timestamp))
                    } catch (e: Exception) { e.printStackTrace() }
                }

                allHistory.sortByDescending { it.timestamp }

                val processedWords = HashSet<String>()

                for (item in allHistory) {
                    if (!processedWords.contains(item.content)) {
                        processedWords.add(item.content)
                        if (item.score < 75) {
                            val diagnosis = PronunciationAnalyst.analyze(item.content, item.score, item.type)
                            insightList.add(InsightItem(item.content, item.score, diagnosis))
                        }
                    }
                    if (insightList.size >= 20) break
                }

                if (insightList.isEmpty()) {
                    rvInsights.visibility = View.GONE
                    tvEmptyState.visibility = View.VISIBLE
                } else {
                    rvInsights.visibility = View.VISIBLE
                    tvEmptyState.visibility = View.GONE
                }

                insightAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    data class FeedbackItem(val content: String, val score: Int, val type: String, val timestamp: Date)

    // --- 4. PDF GENERATION ---
    private fun generatePDF(): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        // Fonts
        val titleFont = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val headerFont = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val normalFont = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        // --- 1. DRAW HEADER BACKGROUND ---
        paint.color = Color.parseColor("#4CAF50") // Green banner
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, 595f, 100f, paint)

        // --- 2. DRAW HEADER TEXT ---
        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.typeface = titleFont
        canvas.drawText("Voice Bridge - Speech Progress Report", 40f, 60f, paint)

        val reportDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        paint.textSize = 14f
        paint.typeface = normalFont
        canvas.drawText("Date: $reportDate", 40f, 85f, paint)

        // --- 3. DRAW OVERALL PERFORMANCE ---
        var currentY = 150f
        paint.color = Color.BLACK
        paint.textSize = 18f
        paint.typeface = headerFont
        canvas.drawText("Overall Performance Summary", 40f, currentY, paint)

        currentY += 30f
        paint.textSize = 14f
        paint.typeface = normalFont

        canvas.drawText("Level 1 (Letters Accuracy):  $scoreL1%", 40f, currentY, paint)
        currentY += 25f
        canvas.drawText("Level 2 (Words Accuracy):   $scoreL2%", 40f, currentY, paint)
        currentY += 25f
        canvas.drawText("Level 3 (Sentences Accuracy): $scoreL3%", 40f, currentY, paint)

        // --- 4. DRAW WEAKNESS ANALYSIS TABLE ---
        currentY += 50f
        paint.color = Color.parseColor("#D32F2F") // Red section title
        paint.textSize = 18f
        paint.typeface = headerFont
        canvas.drawText("Areas for Improvement (Recent Sessions)", 40f, currentY, paint)

        currentY += 20f

        if (insightList.isEmpty()) {
            paint.color = Color.parseColor("#4CAF50")
            paint.textSize = 14f
            paint.typeface = normalFont
            canvas.drawText("Excellent! No significant weaknesses detected recently.", 40f, currentY + 20f, paint)
        } else {
            // Draw Table Header
            paint.color = Color.parseColor("#F5F5F5") // Light gray header bg
            paint.style = Paint.Style.FILL
            canvas.drawRect(40f, currentY, 555f, currentY + 30f, paint)

            paint.color = Color.BLACK
            paint.typeface = headerFont
            paint.textSize = 12f

            // NEW X-COORDINATES FOR BETTER SPACING
            canvas.drawText("Target Item", 50f, currentY + 20f, paint)
            canvas.drawText("Score", 290f, currentY + 20f, paint) // Score placed at X=290
            canvas.drawText("Clinical Diagnosis / Suggestion", 350f, currentY + 20f, paint) // Diagnosis at X=350

            currentY += 30f
            paint.typeface = normalFont

            // Draw Table Rows
            for (item in insightList.take(15)) {

                paint.color = Color.BLACK

                // 1. Target Item (Column 1) - Wrap text if > 35 chars
                var targetText = item.content
                val maxCharsCol1 = 35
                var lineOffsetTarget = 0f

                if (targetText.length > maxCharsCol1) {
                    val splitIndex = targetText.lastIndexOf(' ', maxCharsCol1).takeIf { it != -1 } ?: maxCharsCol1
                    val line1 = targetText.substring(0, splitIndex)
                    val line2 = targetText.substring(splitIndex).trim()

                    canvas.drawText(line1, 50f, currentY + 15f, paint)
                    canvas.drawText(if(line2.length > maxCharsCol1) line2.substring(0, maxCharsCol1 - 3)+"..." else line2, 50f, currentY + 30f, paint)
                    lineOffsetTarget = 15f
                } else {
                    canvas.drawText(targetText, 50f, currentY + 20f, paint)
                }

                // 2. Score (Column 2)
                if (item.score < 50) {
                    paint.color = Color.parseColor("#D32F2F")
                } else {
                    paint.color = Color.parseColor("#F57C00")
                }
                paint.typeface = headerFont
                canvas.drawText("${item.score}%", 290f, currentY + 20f, paint) // Score aligned here

                // 3. Clinical Diagnosis (Column 3) - Wrap text if > 35 chars
                paint.color = Color.BLACK
                paint.typeface = normalFont
                var diagText = item.diagnosis
                val maxCharsCol3 = 35
                var lineOffsetDiag = 0f

                if (diagText.length > maxCharsCol3) {
                    val splitIndex = diagText.lastIndexOf(' ', maxCharsCol3).takeIf { it != -1 } ?: maxCharsCol3
                    val line1 = diagText.substring(0, splitIndex)
                    val line2 = diagText.substring(splitIndex).trim()

                    canvas.drawText(line1, 350f, currentY + 15f, paint)
                    canvas.drawText(if(line2.length > maxCharsCol3) line2.substring(0, maxCharsCol3 - 3)+"..." else line2, 350f, currentY + 30f, paint)
                    lineOffsetDiag = 15f
                } else {
                    canvas.drawText(diagText, 350f, currentY + 20f, paint)
                }

                // Calculate required row height
                val rowHeight = 35f + maxOf(lineOffsetTarget, lineOffsetDiag)

                // Draw bottom border for the row
                paint.color = Color.parseColor("#E0E0E0")
                paint.strokeWidth = 1f
                canvas.drawLine(40f, currentY + rowHeight, 555f, currentY + rowHeight, paint)

                currentY += rowHeight // Move down for next row
            }
        }

        // Draw Footer
        paint.color = Color.GRAY
        paint.textSize = 10f
        canvas.drawText("Generated by Voice Bridge AI Speech Therapy Assistant", 40f, 800f, paint)

        pdfDocument.finishPage(page)

        val fileName = "VoiceBridge_Report_${System.currentTimeMillis()}.pdf"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }

    private fun sharePdf(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Report"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}