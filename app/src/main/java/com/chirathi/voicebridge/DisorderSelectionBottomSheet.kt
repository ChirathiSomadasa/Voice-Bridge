//package com.chirathi.voicebridge
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.RadioButton
//import android.widget.RadioGroup
//import android.widget.Toast
//import androidx.core.view.isVisible
//import com.google.android.material.bottomsheet.BottomSheetDialogFragment
//
//class DisorderSelectionBottomSheet(
//    private val onDisorderSelected: (disorder: String, severity: String) -> Unit
//) : BottomSheetDialogFragment() {
//
//    private var selectedDisorder: String? = null
//    private var selectedSeverity: String? = null
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.layout_disorder_selection_bottom_sheet, container, false)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        val radioGroupDisorders = view.findViewById<RadioGroup>(R.id.radioGroupDisorders)
//        val radioGroupSeverity = view.findViewById<RadioGroup>(R.id.radioGroupSeverity)
//        val btnApply = view.findViewById<Button>(R.id.btnApplyDisorder)
//
//        // Initially hide severity selection
//        radioGroupSeverity.visibility = View.GONE
//
//        // When a disorder is selected, show severity options
//        radioGroupDisorders.setOnCheckedChangeListener { _, checkedId ->
//            if (checkedId != -1) {
//                val selectedRadioButton = view.findViewById<RadioButton>(checkedId)
//                selectedDisorder = selectedRadioButton.text.toString()
//
//                // Show severity options
//                radioGroupSeverity.visibility = View.VISIBLE
//
//                // Clear previous severity selection
//                radioGroupSeverity.clearCheck()
//                selectedSeverity = null
//            }
//        }
//
//        // Track severity selection
//        radioGroupSeverity.setOnCheckedChangeListener { _, checkedId ->
//            if (checkedId != -1) {
//                val selectedRadioButton = view.findViewById<RadioButton>(checkedId)
//                selectedSeverity = selectedRadioButton.text.toString()
//            }
//        }
//
//        btnApply.setOnClickListener {
//            if (selectedDisorder == null) {
//                Toast.makeText(requireContext(), "Please select a disorder type", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            if (selectedSeverity == null) {
//                Toast.makeText(requireContext(), "Please select severity level", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            // Pass both disorder and severity back to parent
//            onDisorderSelected(selectedDisorder!!, selectedSeverity!!)
//            dismiss()
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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DisorderSelectionBottomSheet(
    private val onApply: (disorder: String, severity: String, communication: String, attention: String) -> Unit
) : BottomSheetDialogFragment() {

    private var selectedDisorder: String? = null
    private var selectedSeverity: String? = null
    private var selectedCommunication: String? = null
    private var selectedAttention: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.layout_disorder_selection_bottom_sheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rgDisorder = view.findViewById<RadioGroup>(R.id.radioGroupDisorders)
        val rgSeverity = view.findViewById<RadioGroup>(R.id.radioGroupSeverity)
        val rgCommRow1 = view.findViewById<RadioGroup>(R.id.radioGroupCommunicationRow1)
        val rgCommRow2 = view.findViewById<RadioGroup>(R.id.radioGroupCommunicationRow2)
        val rgAttention = view.findViewById<RadioGroup>(R.id.radioGroupAttention)
        val btnApply = view.findViewById<Button>(R.id.btnApplyDisorder)

        fun RadioGroup.onSelect(setter: (String) -> Unit) {
            setOnCheckedChangeListener { _, checkedId ->
                if (checkedId != -1) {
                    val rb = view.findViewById<RadioButton>(checkedId)
                    setter(rb.text.toString())
                }
            }
        }

        rgDisorder.onSelect { selectedDisorder = it }
        rgSeverity.onSelect { selectedSeverity = it }
        rgAttention.onSelect { selectedAttention = it }
        // Single selection across both Communication rows
        val commGroups = listOf(rgCommRow1, rgCommRow2)
        lateinit var commListener: RadioGroup.OnCheckedChangeListener
        commListener = RadioGroup.OnCheckedChangeListener { group, checkedId ->
            if (checkedId != -1) {
                // Clear the other row
                commGroups
                    .filter { it != group }
                    .forEach { other ->
                        other.setOnCheckedChangeListener(null)
                        other.clearCheck()
                        other.setOnCheckedChangeListener(commListener)
                    }
                val rb = group.findViewById<RadioButton>(checkedId)
                selectedCommunication = rb.text.toString()
            }
        }
        commGroups.forEach { it.setOnCheckedChangeListener(commListener) }

        btnApply.setOnClickListener {
            if (selectedDisorder == null || selectedSeverity == null ||
                selectedCommunication == null || selectedAttention == null
            ) {
                Toast.makeText(requireContext(), "Please select all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onApply(
                selectedDisorder!!,
                selectedSeverity!!,
                selectedCommunication!!,
                selectedAttention!!
            )
            dismiss()
        }
    }
}