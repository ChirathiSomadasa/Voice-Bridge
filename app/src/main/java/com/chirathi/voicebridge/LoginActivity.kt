package com.chirathi.voicebridge

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Initialize views
        emailInput = findViewById(R.id.login_email)
        passwordInput = findViewById(R.id.login_password)

        // Initialize ProgressDialog
        progressDialog = ProgressDialog(this).apply {
            setMessage("Logging in, please wait...")
            setCancelable(false)
        }

        // Login button click listener
        val login = findViewById<Button>(R.id.btn_signin)
        login.setOnClickListener {
            handleLogin()
        }

        // Create Account button click listener
        val createAccount = findViewById<Button>(R.id.btn_create_account)
        createAccount.setOnClickListener {
            val intent = Intent(this, CreateAccountActivity::class.java)
            startActivity(intent)
        }
    }

    private fun handleLogin() {
        val emailText = emailInput.text.toString().trim()
        val passwordText = passwordInput.text.toString().trim()

        // Input Validation
        if (emailText.isEmpty() || passwordText.isEmpty()) {
            Toast.makeText(this, "Please enter both your email and password.", Toast.LENGTH_SHORT).show()
            return
        }

        // Show ProgressDialog before starting the login process
        progressDialog.show()

        // Firebase Authentication Sign-In
        auth.signInWithEmailAndPassword(emailText, passwordText)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        // Retrieve User Role from Firestore
                        fetchUserRoleAndNavigate(userId)
                    }
                } else {
                    progressDialog.dismiss()

                    // Map technical Firebase errors to user-friendly messages
                    val errorMessage = when (task.exception?.message) {
                        "There is no user record corresponding to this identifier. The user may have been deleted." -> "We couldn't find an account with that email."
                        "The email address is badly formatted." -> "Please enter a valid email address."
                        "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> "Network error. Please check your internet connection."
                        else -> "Incorrect email or password. Please try again." // Catch-all for wrong passwords/general auth errors
                    }

                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun fetchUserRoleAndNavigate(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                progressDialog.dismiss()

                if (document.exists()) {
                    val isTeacher = document.getBoolean("isTeacher") ?: false

                    // Navigate based on user role (teacher / child)
                    val destinationActivity = if (isTeacher) {
                        TeacherDashboardActivity::class.java
                    } else {
                        HomeActivity::class.java
                    }

                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, destinationActivity)
                    startActivity(intent)
                    finish() // Close LoginActivity
                } else {
                    Toast.makeText(this, "Account details not found. Please contact support.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to load account details. Please try again.", Toast.LENGTH_LONG).show()
            }
    }
}