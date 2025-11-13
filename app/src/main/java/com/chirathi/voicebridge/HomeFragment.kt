package com.chirathi.voicebridge

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout // Import the specific type for clarity
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val speechPractice = view.findViewById<ConstraintLayout>(R.id.speech_practice)
        speechPractice.setOnClickListener {
            val intent = Intent(context, SpeechPracticeActivity::class.java)
            startActivity(intent)
        }

        val symbolCommunication = view.findViewById<ConstraintLayout>(R.id.symbol_communication)
        symbolCommunication.setOnClickListener {
            val intent = Intent(context, SymbolCommunicationActivity::class.java)
            startActivity(intent)
        }

        val gameSection = view.findViewById<ConstraintLayout>(R.id.game)
        gameSection.setOnClickListener {
            val intent = Intent(context, GameDashboardActivity::class.java)
            startActivity(intent)
        }

        val education_therapy = view.findViewById<ConstraintLayout>(R.id.education_therapy)
        education_therapy.setOnClickListener {
            val intent = Intent(context, Education_therapyActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}
