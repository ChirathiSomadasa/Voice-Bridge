package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TeacherHomeFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_teacher_home, container, false)

        val tvWelcome = view.findViewById<TextView>(R.id.welcomeText)
        val tvName = view.findViewById<TextView>(R.id.userName)

        // Load teacher name
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { doc ->
                    val first = doc.getString("firstName").orEmpty()
                    val last = doc.getString("lastName").orEmpty()
                    tvName.text = if (first.isNotBlank() || last.isNotBlank()) {
                        "Hello $first $last"
                    } else "Hello Teacher"
                }
                .addOnFailureListener {
                    Log.e("TeacherHome", "Failed to load user", it)
                    tvName.text = "Hello Teacher"
                }
        } else {
            tvName.text = "Hello Teacher"
        }

//        // Cards
//        view.findViewById<ConstraintLayout>(R.id.manage_lessons).setOnClickListener {
//            // startActivity(Intent(requireContext(), ManageLessonsActivity::class.java))
//            Toast.makeText(requireContext(), "Manage lessons", Toast.LENGTH_SHORT).show()
//        }
//        view.findViewById<ConstraintLayout>(R.id.review_submissions).setOnClickListener {
//            Toast.makeText(requireContext(), "Review submissions", Toast.LENGTH_SHORT).show()
//        }
//        view.findViewById<ConstraintLayout>(R.id.view_progress).setOnClickListener {
//            Toast.makeText(requireContext(), "View progress", Toast.LENGTH_SHORT).show()
//        }
//        view.findViewById<ConstraintLayout>(R.id.resources_library).setOnClickListener {
//            Toast.makeText(requireContext(), "Resources library", Toast.LENGTH_SHORT).show()
//        }

        return view
    }
}