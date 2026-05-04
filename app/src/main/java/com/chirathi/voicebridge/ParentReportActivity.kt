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
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

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

    // Phoneme Progress Chart
    private lateinit var phonemeChart: BarChart

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
        loadAnalysisData() // This also loads the Phoneme Chart data

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

        phonemeChart = findViewById(R.id.phonemeChart)

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
        // Level 1: 26 Letters Total
        val progress1 = (batch1 * 5).coerceAtMost(26)
        pbLevel1.max = 26
        pbLevel1.progress = progress1
        if (progress1 >= 26) {
            tvLevel1Progress.text = "Level 1: Letters (26/26) - Mastered!"
        } else {
            tvLevel1Progress.text = "Level 1: Letters ($progress1/26)"
        }

        // Level 2: 40 Words Total (8 batches * 5 words)
        val progress2 = (batch2 * 5).coerceAtMost(40)
        pbLevel2.max = 40
        pbLevel2.progress = progress2
        if (batch2 >= 8) {
            tvLevel2Progress.text = "Level 2: Words (40/40) - Mastered!"
        } else {
            tvLevel2Progress.text = "Level 2: Words ($progress2/40)"
        }

        // Level 3: 30 Sentences Total (10 stories * 3 sentences)
        val progress3 = (batch3 * 3).coerceAtMost(30)
        pbLevel3.max = 30
        pbLevel3.progress = progress3
        if (batch3 >= 10) {
            tvLevel3Progress.text = "Level 3: Sentences (30/30) - Mastered!"
        } else {
            tvLevel3Progress.text = "Level 3: Sentences ($progress3/30)"
        }

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
        val tvScore = view.findViewById<TextView>(R.id.tvScore)
        val tvLabel = view.findViewById<TextView>(R.id.tvLabel)
        val mainCard = view as androidx.cardview.widget.CardView

        tvScore.text = "$score%"
        tvLabel.text = label

        // Dynamic Color Logic based on performance
        val (bgColor, textColor) = when {
            score >= 75 -> Pair("#E8F5E9", "#2E7D32") // Light Green
            score >= 50 -> Pair("#FFF3E0", "#EF6C00") // Light Orange
            else -> Pair("#FFEBEE", "#C62828")        // Light Red
        }

        mainCard.setCardBackgroundColor(Color.parseColor(bgColor))
        tvScore.setTextColor(Color.parseColor(textColor))
    }

    // --- 3. ANALYSIS WEAKNESSES & PHONEME CHART ---
    private fun loadAnalysisData() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("pronunciation_feedback")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                insightList.clear()
                val allHistory = ArrayList<FeedbackItem>()

                // HashMap to store phoneme data (Letter -> Pair<TotalScore, Count>)
                val phonemeStats = HashMap<String, Pair<Int, Int>>()

                for (doc in documents) {
                    try {
                        val rawContent = doc.getString("content") ?: ""
                        val score = doc.getLong("score")?.toInt() ?: 0
                        val rawType = doc.getString("item_type")
                        val type = rawType ?: if (rawContent.contains(" ")) "sentence" else "word"
                        val timestamp = doc.getDate("timestamp") ?: Date(0)

                        // Format the text correctly based on the type
                        val content = if (type == "sentence") {
                            // If it's a sentence: Capitalize only the first letter, rest lowercase
                            rawContent.lowercase().replaceFirstChar { it.uppercase() }
                        } else {
                            // If it's a word or letter: All lowercase
                            rawContent.lowercase()
                        }

                        allHistory.add(FeedbackItem(content, score, type, timestamp))

                        // Extract data specifically for the Phoneme Chart (Level 1 items)
                        if (type == "letter") {
                            val letterKey = content.uppercase() // Send uppercase letter to the chart for better readability
                            val currentStat = phonemeStats[letterKey] ?: Pair(0, 0)
                            phonemeStats[letterKey] = Pair(currentStat.first + score, currentStat.second + 1)
                        }

                    } catch (e: Exception) { e.printStackTrace() }
                }

                allHistory.sortByDescending { it.timestamp }

                val processedWords = HashSet<String>()

                // Populate the Weakness List for the RecyclerView
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

                // Handle UI visibility for Empty State
                if (insightList.isEmpty()) {
                    rvInsights.visibility = View.GONE
                    tvEmptyState.visibility = View.VISIBLE
                } else {
                    rvInsights.visibility = View.VISIBLE
                    tvEmptyState.visibility = View.GONE
                }

                insightAdapter.notifyDataSetChanged()

                // Render the Bar Chart with the gathered data
                setupPhonemeChart(phonemeStats)

            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // --- 4. MPANDROIDCHART RENDERING ---
    private fun setupPhonemeChart(phonemeStats: HashMap<String, Pair<Int, Int>>) {
        if (phonemeStats.isEmpty()) {
            phonemeChart.visibility = View.GONE
            return
        }

        phonemeChart.visibility = View.VISIBLE
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        var index = 0f

        // Sort by average score (lowest first to highlight weaknesses) and take top 6
        val sortedPhonemes = phonemeStats.entries.sortedBy { (it.value.first / it.value.second) }.take(6)

        for ((letter, stats) in sortedPhonemes) {
            val averageScore = (stats.first / stats.second).toFloat()
            entries.add(BarEntry(index, averageScore))
            labels.add(letter)
            index += 1f
        }

        val dataSet = BarDataSet(entries, "Pronunciation Score %")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK

        val barData = BarData(dataSet)
        phonemeChart.data = barData

        // --- Chart UI Settings ---
        phonemeChart.description.isEnabled = false
        phonemeChart.setDrawGridBackground(false)
        phonemeChart.legend.isEnabled = false

        phonemeChart.setExtraOffsets(0f, 0f, 0f, 15f)

        val xAxis = phonemeChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.textSize = 14f
        xAxis.textColor = Color.DKGRAY

        xAxis.yOffset = 10f

        val yAxisLeft = phonemeChart.axisLeft
        yAxisLeft.axisMinimum = 0f
        yAxisLeft.axisMaximum = 100f
        yAxisLeft.setDrawGridLines(true)
        phonemeChart.axisRight.isEnabled = false

        // Refresh and animate chart
        phonemeChart.animateY(1000)
        phonemeChart.invalidate()
    }

    data class FeedbackItem(val content: String, val score: Int, val type: String, val timestamp: Date)

    // --- 5. PDF GENERATION ---
    private fun generatePDF(): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        // Define typography for different sections
        val titleFont = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val headerFont = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val normalFont = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        // Draw Header Background
        paint.color = Color.parseColor("#4CAF50") // Thematic green banner
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, 595f, 100f, paint)

        // Draw Header Text
        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.typeface = titleFont
        canvas.drawText("Voice Bridge - Speech Progress Report", 40f, 60f, paint)

        // Append the current date to the report
        val reportDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        paint.textSize = 14f
        paint.typeface = normalFont
        canvas.drawText("Date: $reportDate", 40f, 85f, paint)

        // Draw Overall Performance Metrics
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

        // Draw Weakness Analysis Table
        currentY += 50f
        paint.color = Color.parseColor("#D32F2F") // Alert red for weaknesses section
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
            // Draw Table Header Background
            paint.color = Color.parseColor("#F5F5F5")
            paint.style = Paint.Style.FILL
            canvas.drawRect(40f, currentY, 555f, currentY + 30f, paint)

            // Draw Table Header Text
            paint.color = Color.BLACK
            paint.typeface = headerFont
            paint.textSize = 12f

            canvas.drawText("Target Item", 50f, currentY + 20f, paint)
            canvas.drawText("Score", 290f, currentY + 20f, paint)
            canvas.drawText("Clinical Diagnosis / Suggestion", 350f, currentY + 20f, paint)

            currentY += 30f
            paint.typeface = normalFont

            for (item in insightList.take(15)) {
                paint.color = Color.BLACK

                // Column 1: Target Item (With Text Wrapping)
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

                // Column 2: Accuracy Score
                if (item.score < 50) {
                    paint.color = Color.parseColor("#D32F2F")
                } else {
                    paint.color = Color.parseColor("#F57C00")
                }
                paint.typeface = headerFont
                canvas.drawText("${item.score}%", 290f, currentY + 20f, paint)

                // Column 3: Clinical Diagnosis (With Text Wrapping)
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

                val rowHeight = 35f + maxOf(lineOffsetTarget, lineOffsetDiag)

                // Draw row separator line
                paint.color = Color.parseColor("#E0E0E0")
                paint.strokeWidth = 1f
                canvas.drawLine(40f, currentY + rowHeight, 555f, currentY + rowHeight, paint)

                currentY += rowHeight
            }
        }

        // Draw Footer
        paint.color = Color.GRAY
        paint.textSize = 10f
        canvas.drawText("Generated by Voice Bridge AI Speech Therapy Assistant", 40f, 800f, paint)

        pdfDocument.finishPage(page)

        // --- 6. SMART SAVING LOGIC ---
        val fileName = "VoiceBridge_Report_${System.currentTimeMillis()}.pdf"

        val privateDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (privateDir != null && !privateDir.exists()) privateDir.mkdirs()
        val localFile = File(privateDir, fileName)

        try {
            FileOutputStream(localFile).use { pdfDocument.writeTo(it) }
            pdfDocument.close()

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/VoiceBridgeReports")
                }

                val uri = contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        localFile.inputStream().copyTo(outputStream)
                    }
                    Toast.makeText(this, "Saved to Downloads/VoiceBridgeReports folder", Toast.LENGTH_LONG).show()
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val appDir = File(downloadsDir, "VoiceBridgeReports")
                if (!appDir.exists()) appDir.mkdirs()

                val publicFile = File(appDir, fileName)
                localFile.inputStream().copyTo(FileOutputStream(publicFile))
                Toast.makeText(this, "Saved to Downloads/VoiceBridgeReports folder", Toast.LENGTH_LONG).show()
            }

            return localFile

        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show()
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
