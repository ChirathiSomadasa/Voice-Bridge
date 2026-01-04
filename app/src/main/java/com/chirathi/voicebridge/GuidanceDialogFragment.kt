package com.chirathi.voicebridge

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.chirathi.voicebridge.R

class GuidanceDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_AGE_GROUP = "age_group"
        private const val ARG_ROUTINE_NAME = "routine_name"

        fun newInstance(ageGroup: Int, routineName: String): GuidanceDialogFragment {
            val fragment = GuidanceDialogFragment()
            val args = Bundle()
            args.putInt(ARG_AGE_GROUP, ageGroup)
            args.putString(ARG_ROUTINE_NAME, routineName)
            fragment.arguments = args
            return fragment
        }
    }

    interface GuidanceDialogListener {
        fun onGuidanceDismissed(ageGroup: Int, routineName: String)
    }

    private var listener: GuidanceDialogListener? = null

    fun setListener(listener: GuidanceDialogListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_guidance_dialog, container, false)
        val ageGroup = arguments?.getInt(ARG_AGE_GROUP) ?: 6
        val routineName = arguments?.getString(ARG_ROUTINE_NAME) ?: "Routine"

        val titleText = view.findViewById<TextView>(R.id.titleText)
        val guidanceText = view.findViewById<TextView>(R.id.guidanceText)
        val okButton = view.findViewById<Button>(R.id.btn_ok)

        // Set guidance based on age group
        when (ageGroup) {
            in 6..7 -> {
                titleText.text = "Morning Routine Guide"
                guidanceText.text = "Drag the steps into the correct order! Tap on each step to learn more. Start with the first thing you do when you wake up."
            }
            in 8..10 -> {
                titleText.text = "Morning Routine Guide"
                guidanceText.text = "Oops!! That comes a bit later. \n\nFor now, focus on mastering your morning routine!"
            }
            else -> {
                titleText.text = "Welcome!"
                guidanceText.text = "Let's get started with your daily routine!"
            }
        }

        okButton.setOnClickListener {
            listener?.onGuidanceDismissed(ageGroup, routineName)
            dismiss()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}