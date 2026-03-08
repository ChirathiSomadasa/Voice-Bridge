//package com.chirathi.voicebridge
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.RadioButton
//import android.widget.RadioGroup
//import com.google.android.material.bottomsheet.BottomSheetDialogFragment
//
//class DisorderSelectionBottomSheet(private val onDisorderSelected: (String) -> Unit) : BottomSheetDialogFragment() {
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.layout_disorder_selection_bottom_sheet, container, false)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupDisorders)
//        val btnApply = view.findViewById<Button>(R.id.btnApplyDisorder)
//
//        btnApply.setOnClickListener {
//            val selectedRadioButtonId = radioGroup.checkedRadioButtonId
//            if (selectedRadioButtonId != -1) {
//                val selectedRadioButton = view.findViewById<RadioButton>(selectedRadioButtonId)
//                val selectedDisorder = selectedRadioButton.text.toString()
//                onDisorderSelected(selectedDisorder) // Pass back selected disorder
//                dismiss()
//            }
//        }
//    }
//}

package com.chirathi.voicebridge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DisorderSelectionBottomSheet(
    private val onDisorderSelected: (disorder: String, severity: String) -> Unit
) : BottomSheetDialogFragment() {

    private var selectedDisorder: String? = null
    private var selectedSeverity: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_disorder_selection_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val radioGroupDisorders = view.findViewById<RadioGroup>(R.id.radioGroupDisorders)
        val radioGroupSeverity = view.findViewById<RadioGroup>(R.id.radioGroupSeverity)
        val btnApply = view.findViewById<Button>(R.id.btnApplyDisorder)

        // Initially hide severity selection
        radioGroupSeverity.visibility = View.GONE

        // When a disorder is selected, show severity options
        radioGroupDisorders.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) {
                val selectedRadioButton = view.findViewById<RadioButton>(checkedId)
                selectedDisorder = selectedRadioButton.text.toString()

                // Show severity options
                radioGroupSeverity.visibility = View.VISIBLE

                // Clear previous severity selection
                radioGroupSeverity.clearCheck()
                selectedSeverity = null
            }
        }

        // Track severity selection
        radioGroupSeverity.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) {
                val selectedRadioButton = view.findViewById<RadioButton>(checkedId)
                selectedSeverity = selectedRadioButton.text.toString()
            }
        }

        btnApply.setOnClickListener {
            if (selectedDisorder == null) {
                Toast.makeText(requireContext(), "Please select a disorder type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedSeverity == null) {
                Toast.makeText(requireContext(), "Please select severity level", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Pass both disorder and severity back to parent
            onDisorderSelected(selectedDisorder!!, selectedSeverity!!)
            dismiss()
        }
    }
}