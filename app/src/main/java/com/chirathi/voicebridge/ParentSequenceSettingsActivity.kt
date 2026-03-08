package com.chirathi.voicebridge

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * ParentSequenceSettingsActivity — v2
 *
 * • Default sub-routines (index 0-2) are always shown and editable.
 * • Parents can add unlimited new sub-routines per routine (index 3+).
 * • Added sub-routines can be deleted; default ones cannot be removed.
 * • Saving writes all sub-routines back via SequenceDataManager.
 */
class ParentSequenceSettingsActivity : AppCompatActivity() {

    // subContainers[routineId] = the LinearLayout that holds all sub-routine blocks for that routine
    private val subContainers = arrayOfNulls<LinearLayout>(3)

    // subData[routineId] = ordered list of (stepFields, isDeletable)
    // stepFields = list of 3 EditTexts for that sub-routine
    private data class SubRoutineRow(
        val stepFields : List<EditText>,
        val isDeletable: Boolean,
        val container  : LinearLayout
    )
    private val subData = Array(3) { mutableListOf<SubRoutineRow>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F9FBF7"))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(32))
        }
        root.addView(container)
        setContentView(root)

        container.addView(buildTopBar())
        container.addView(TextView(this).apply {
            text     = "Edit existing steps or add new sub-routines to any category. " +
                    "Default sub-routines cannot be removed."
            textSize = 13f
            setTextColor(Color.parseColor("#666666"))
            setPadding(4, 0, 4, dp(20))
        })

        val current = SequenceDataManager.getSequences(this)

        for (routineId in 0..2) {
            container.addView(buildRoutineCard(routineId, current))
        }

        container.addView(buildBottomButtons())
    }

    // ======================================================================
    // Top bar
    // ======================================================================

    private fun buildTopBar() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity     = Gravity.CENTER_VERTICAL
        setPadding(0, 0, 0, dp(8))

        addView(TextView(this@ParentSequenceSettingsActivity).apply {
            text     = "←"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#388E3C"))
            setPadding(0, 0, dp(12), 0)
            setOnClickListener { finish() }
        })
        addView(TextView(this@ParentSequenceSettingsActivity).apply {
            text     = "Customise Activity Sequences"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1B5E20"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
    }

    // ======================================================================
    // Routine card
    // ======================================================================

    private fun buildRoutineCard(
        routineId: Int,
        current  : Map<Int, Map<Int, List<Pair<String, String>>>>
    ): View {
        val card = LinearLayout(this).apply {
            orientation  = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(16) }
            elevation = 4f
        }

        // Routine title
        card.addView(TextView(this).apply {
            text     = SequenceDataManager.routineNames[routineId]
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#2E7D32"))
            setPadding(0, 0, 0, dp(12))
        })

        // Container for sub-routine blocks (dynamic)
        val subContainer = LinearLayout(this).apply {
            orientation  = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        subContainers[routineId] = subContainer
        card.addView(subContainer)

        // Populate existing sub-routines
        val sortedKeys = current[routineId]?.keys?.sorted() ?: (0..2).toList()
        sortedKeys.forEach { subId ->
            val steps      = current[routineId]?.get(subId) ?: SequenceDataManager.defaults[routineId]!![subId] ?: emptyList()
            val deletable  = subId >= 3   // only custom-added ones can be deleted
            addSubRoutineBlock(routineId, subContainer, steps, deletable)
        }

        // "Add Sub-Routine" button
        card.addView(buildAddButton(routineId, subContainer))

        return card
    }

    // ======================================================================
    // Add a sub-routine block into the container
    // ======================================================================

    private fun addSubRoutineBlock(
        routineId   : Int,
        subContainer: LinearLayout,
        prefill     : List<Pair<String, String>>,
        deletable   : Boolean
    ) {
        val subIndex = subData[routineId].size   // 0-based display index
        val block    = LinearLayout(this).apply {
            orientation  = LinearLayout.VERTICAL
            setBackgroundColor(if (deletable) Color.parseColor("#F1F8E9") else Color.WHITE)
            setPadding(0, dp(8), 0, dp(8))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(6) }
        }

        // Header row: label + delete button
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(6) }
        }

        val labelPrefix = if (deletable) "Custom" else "Default"
        headerRow.addView(TextView(this).apply {
            text     = "$labelPrefix Sub-Routine ${subIndex + 1}"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (deletable) Color.parseColor("#2E7D32") else Color.parseColor("#555555"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })

        if (deletable) {
            headerRow.addView(TextView(this).apply {
                text     = "✕ Remove"
                textSize = 12f
                setTextColor(Color.parseColor("#C62828"))
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setBackgroundColor(Color.parseColor("#FFEBEE"))
                setOnClickListener {
                    // Remove from UI and data list
                    subContainer.removeView(block)
                    val rowToRemove = subData[routineId].firstOrNull { it.container == block }
                    subData[routineId].remove(rowToRemove)
                    refreshSubRoutineLabels(routineId)
                }
            })
        }

        block.addView(headerRow)

        // 3 step fields
        val stepFields = mutableListOf<EditText>()
        for (stepIndex in 0..2) {
            val row = LinearLayout(this).apply {
                orientation  = LinearLayout.HORIZONTAL
                gravity      = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dp(6) }
            }

            // Step badge
            row.addView(TextView(this).apply {
                text     = "${stepIndex + 1}"
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#66BB6A"))
                gravity  = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).also { it.marginEnd = dp(8) }
            })

            val et = EditText(this).apply {
                setText(prefill.getOrNull(stepIndex)?.second ?: "")
                textSize  = 14f
                setTextColor(Color.parseColor("#333333"))
                setHintTextColor(Color.parseColor("#AAAAAA"))
                hint      = "Step ${stepIndex + 1}"
                background = buildEditBackground()
                setPadding(dp(10), dp(8), dp(10), dp(8))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                maxLines  = 1
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            }
            stepFields.add(et)
            row.addView(et)
            block.addView(row)
        }

        // Divider (not on last item — handled by spacing)
        block.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#E8F5E9"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
            ).also { it.topMargin = dp(4) }
        })

        subContainer.addView(block)
        subData[routineId].add(SubRoutineRow(stepFields, deletable, block))
    }

    // Re-labels sub-routine headers after a deletion so numbers stay sequential
    private fun refreshSubRoutineLabels(routineId: Int) {
        subData[routineId].forEachIndexed { i, row ->
            val prefix = if (row.isDeletable) "Custom" else "Default"
            // The header TextView is the first child of the first child (headerRow) of the block
            val headerRow = row.container.getChildAt(0) as? LinearLayout ?: return@forEachIndexed
            val label     = headerRow.getChildAt(0) as? TextView ?: return@forEachIndexed
            label.text    = "$prefix Sub-Routine ${i + 1}"
        }
    }

    // ======================================================================
    // "Add Sub-Routine" button
    // ======================================================================

    private fun buildAddButton(routineId: Int, subContainer: LinearLayout): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)

            addView(TextView(this@ParentSequenceSettingsActivity).apply {
                text     = "+ Add Sub-Routine"
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#388E3C"))
                setBackgroundColor(Color.parseColor("#E8F5E9"))
                setPadding(dp(16), dp(10), dp(16), dp(10))
                setOnClickListener {
                    addSubRoutineBlock(routineId, subContainer, emptyList(), deletable = true)
                }
            })
        }
    }

    // ======================================================================
    // Bottom: Save + Reset
    // ======================================================================

    private fun buildBottomButtons() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(8), 0, 0)

        addView(Button(this@ParentSequenceSettingsActivity).apply {
            text     = "Save Changes"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#388E3C"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50)
            ).also { it.bottomMargin = dp(12) }
            setOnClickListener { saveAndFinish() }
        })

        addView(Button(this@ParentSequenceSettingsActivity).apply {
            text     = "Reset All to Defaults"
            textSize = 15f
            setTextColor(Color.parseColor("#388E3C"))
            setBackgroundColor(Color.parseColor("#E8F5E9"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)
            )
            setOnClickListener { confirmReset() }
        })
    }

    // ======================================================================
    // Save
    // ======================================================================

    private fun saveAndFinish() {
        val result = mutableMapOf<Int, MutableMap<Int, List<Pair<String, String>>>>()

        for (routineId in 0..2) {
            val subMap = mutableMapOf<Int, List<Pair<String, String>>>()

            subData[routineId].forEachIndexed { subIndex, row ->
                val defaults   = SequenceDataManager.defaults[routineId]?.get(subIndex)
                val steps      = row.stepFields.mapIndexed { stepIndex, et ->
                    val text      = et.text.toString().trim()
                    val finalText = text.ifEmpty {
                        defaults?.getOrNull(stepIndex)?.second ?: "Step ${stepIndex + 1}"
                    }
                    val defaultId = defaults?.getOrNull(stepIndex)?.first ?: "step_$stepIndex"
                    val id = if (text.isEmpty()) defaultId
                    else text.lowercase().replace(" ", "_").take(30)
                    id to finalText
                }
                subMap[subIndex] = steps
            }

            result[routineId] = subMap
        }

        SequenceDataManager.saveSequences(this, result)
        Toast.makeText(this, "Sequences saved!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun confirmReset() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Reset to Defaults?")
            .setMessage("All custom sub-routines will be removed and defaults restored.")
            .setPositiveButton("Reset") { _, _ ->
                SequenceDataManager.resetToDefaults(this)
                Toast.makeText(this, "Reset to defaults", Toast.LENGTH_SHORT).show()
                recreate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ======================================================================
    // Helpers
    // ======================================================================

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildEditBackground() = android.graphics.drawable.GradientDrawable().apply {
        shape        = android.graphics.drawable.GradientDrawable.RECTANGLE
        cornerRadius = dp(8).toFloat()
        setColor(Color.parseColor("#F1F8E9"))
        setStroke(dp(1), Color.parseColor("#A5D6A7"))
    }
}