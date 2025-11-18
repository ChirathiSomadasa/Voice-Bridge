package com.chirathi.voicebridge

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class ChildRegistrationActivity : AppCompatActivity() {

    // Declare views and Firebase instances
    lateinit var firstName: TextInputEditText
    lateinit var lastName: TextInputEditText
    lateinit var age: TextInputEditText
    lateinit var email: TextInputEditText
    lateinit var password: TextInputEditText
    lateinit var confirmPassword: TextInputEditText
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private lateinit var progressDialog: ProgressDialog // Declare ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_registration)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Initialize views
        firstName = findViewById(R.id.firstName)
        lastName = findViewById(R.id.lastName)
        age = findViewById(R.id.age)
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        confirmPassword = findViewById(R.id.confirmPassword)

        // Initialize ProgressDialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Registering, please wait...")
        progressDialog.setCancelable(false)


        val signupBtn = findViewById<Button>(R.id.signup_btn)
        signupBtn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val loginLink = findViewById<TextView>(R.id.link_signin)
        loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }

        signupBtn.setOnClickListener {
            val firstNameText = firstName.text.toString().trim()
            val lastNameText = lastName.text.toString().trim()
            val addressText = age.text.toString().trim()
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()
            val confirmPasswordText = confirmPassword.text.toString().trim()

            // Check if all fields are filled
            if (firstNameText.isEmpty() || lastNameText.isEmpty() || addressText.isEmpty() || emailText.isEmpty() || passwordText.isEmpty() || confirmPasswordText.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if passwords match
            if (passwordText != confirmPasswordText) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show the ProgressDialog
            progressDialog.show()

            // Create user with email and password
            auth.createUserWithEmailAndPassword(emailText, passwordText)
                .addOnCompleteListener { task ->
                    // Dismiss the ProgressDialog
                    progressDialog.dismiss()

                    if (task.isSuccessful) {
                        // Get the user ID of the newly created user
                        val userId = auth.currentUser?.uid

                        // Create a user map to store in Firestore
                        val userMap = hashMapOf(
                            "firstName" to firstNameText,
                            "lastName" to lastNameText,
                            "address" to addressText,
                            "email" to emailText,
                            "isTeacher" to false  // Child is not a Teacher
                        )

                        // Store user data in Firestore
                        if (userId != null) {
                            db.collection("users").document(userId).set(userMap)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Successfully Registered!", Toast.LENGTH_SHORT).show()

                                    // Clear fields
                                    firstName.text?.clear()
                                    lastName.text?.clear()
                                    age.text?.clear()
                                    email.text?.clear()
                                    password.text?.clear()
                                    confirmPassword.text?.clear()

                                    // Navigate to LoginActivity
                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        // Dismiss the ProgressDialog
                        progressDialog.dismiss()

                        // Handle different errors more specifically
                        val errorMessage = when (task.exception?.message) {
                            "The email address is already in use by another account." -> "Email is already registered. Try another email."
                            "The given password is invalid. [ Password should be at least 6 characters ]" -> "Password must be at least 6 characters."
                            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> "Network error. Please check your connection."
                            else -> "Registration Failed: ${task.exception?.message}"
                        }

                        // Display appropriate error message
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}