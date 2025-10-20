package com.chirathi.voicebridge

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
        return view
    }
}
