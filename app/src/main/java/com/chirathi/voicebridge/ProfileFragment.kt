package com.chirathi.voicebridge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // UI Elements
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etAge: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnUpdate: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize Views
        etFirstName = view.findViewById(R.id.etFirstName)
        etLastName = view.findViewById(R.id.etLastName)
        etAge = view.findViewById(R.id.etAge)
        etEmail = view.findViewById(R.id.etEmail)
        btnUpdate = view.findViewById(R.id.btnUpdate)
        
        etEmail.isEnabled = false

        // Load data from Firestore
        loadUserProfile()

        // Set up the Update button click listener
        btnUpdate.setOnClickListener {
            updateUserProfile()
        }

        return view
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            // Fetch the document from the "users" collection matching the current UID
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Retrieve data from document
                        val firstName = document.getString("firstName")
                        val lastName = document.getString("lastName")
                        val age = document.getString("age")
                        val email = document.getString("email")

                        // Set data to the EditText fields
                        etFirstName.setText(firstName)
                        etLastName.setText(lastName)
                        etAge.setText(age)
                        etEmail.setText(email)
                    } else {
                        Toast.makeText(requireContext(), "User profile not found.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        // Get the updated text from the fields
        val updatedFirstName = etFirstName.text.toString().trim()
        val updatedLastName = etLastName.text.toString().trim()
        val updatedAge = etAge.text.toString().trim()

        // Basic validation
        if (updatedFirstName.isEmpty() || updatedLastName.isEmpty() || updatedAge.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill out all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a map of the fields to update
        val updates = hashMapOf<String, Any>(
            "firstName" to updatedFirstName,
            "lastName" to updatedLastName,
            "age" to updatedAge
        )

        // Update the document in Firestore
        db.collection("users").document(userId).update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}