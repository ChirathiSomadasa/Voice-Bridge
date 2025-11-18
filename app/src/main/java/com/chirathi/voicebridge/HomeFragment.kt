package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize Firebase Instances
        val auth = FirebaseAuth.getInstance()
        val db = Firebase.firestore

        // Reference to TextView for the name
        val userNameTextView = view.findViewById<TextView>(R.id.userName)

        // Fetch current user data from Firebase Auth
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val userId = currentUser.uid

            // Fetch user data from Firestore
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    // Check if the document exists before accessing data
                    if (document.exists()) {
                        // Retrieve names
                        val firstName = document.getString("firstName") ?: ""
                        val lastName = document.getString("lastName") ?: ""

                        // Update the TextView
                        if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                            // Concatenate names
                            userNameTextView.text = "Hello $firstName $lastName"
                        } else {
                            userNameTextView.text = "Hello User" // Fallback
                        }
                    } else {
                        // Document doesn't exist (data not saved properly during registration)
                        userNameTextView.text = "Hello User (Data Missing)"
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle network or permission errors
                    Log.e("HomeFragment", "Error fetching user data", exception)
                    userNameTextView.text = "Hello User (Error)"
                }
        } else {
            // No user is logged in
            userNameTextView.text = "Hello Guest"
        }


        // Setup click listeners
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

        return view
    }
}