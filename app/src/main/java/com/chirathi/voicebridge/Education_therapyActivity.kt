package com.chirathi.voicebridge

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment


class Education_therapyActivity : AppCompatActivity() {

    // Declare views at the class level to access them easily in multiple functions
    private lateinit var menuScrollView: ScrollView
    private lateinit var fragmentContainer: View
    private lateinit var topImage: ImageView
    private lateinit var bottomBg: ImageView
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_education_therapy)

        val btnLevel1: Button = findViewById(R.id.btn_level1)
        val btnLevel2: Button = findViewById(R.id.btn_level2)
        // ... find other buttons

        menuScrollView = findViewById(R.id.menuScrollView)
        fragmentContainer = findViewById(R.id.fragment_container)
        topImage = findViewById(R.id.topImage)
        bottomBg = findViewById(R.id.bottom_bg)
        backButton = findViewById(R.id.back)

        btnLevel1.setOnClickListener {
            showFragmentView() // Hide menu and images
            launchFragment(Education_therapyLevel1Fragment()) // Launch the fragment
        }

        btnLevel2.setOnClickListener {
            // Similarly for level 2 when you create its fragment
            // showFragmentView()
            // launchFragment(Level2Fragment())
        }

        // The custom back button should go back to the menu
        backButton.setOnClickListener {
            handleBackButtonPress()
        }
    }


    private fun launchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null) // Important for back navigation
            .commit()
    }

    // A helper function to hide the menu and show the fragment container
    private fun showFragmentView() {
        menuScrollView.visibility = View.GONE
        topImage.visibility = View.GONE
        bottomBg.visibility = View.GONE
        backButton.visibility = View.GONE // Hide the custom back button too
        fragmentContainer.visibility = View.VISIBLE
    }

    // A helper function to show the menu and hide the fragment container
    private fun showMenuView() {
        menuScrollView.visibility = View.VISIBLE
        topImage.visibility = View.VISIBLE
        bottomBg.visibility = View.VISIBLE
        backButton.visibility = View.VISIBLE // Show the custom back button again
        fragmentContainer.visibility = View.GONE
    }

    // Centralized logic for handling the back press
    private fun handleBackButtonPress() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            showMenuView() // Show the menu and images again
        } else {
            super.onBackPressed() // Or finish() if you want to close the activity
        }
    }

//     Handle the system back button press
    override fun onBackPressed() {
        handleBackButtonPress()
    }
}
