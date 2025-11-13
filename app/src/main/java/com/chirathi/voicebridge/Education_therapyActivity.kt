package com.chirathi.voicebridge

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlin.compareTo


class Education_therapyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_education_therapy)

        val btnLevel1: Button = findViewById(R.id.btn_level1)
        val btnLevel2: Button = findViewById(R.id.btn_level2)
        // ... find other buttons

        val menuScrollView: ScrollView = findViewById(R.id.menuScrollView)
        val backButton: ImageView = findViewById(R.id.back)
        val fragmentContainer: View = findViewById(R.id.fragment_container)

        btnLevel1.setOnClickListener {
            // When button is clicked, hide the menu and show the fragment
            menuScrollView.visibility = View.GONE
            topImage.visibility = View.GONE
            fragmentContainer.visibility = View.VISIBLE
            launchFragment(Education_therapyLevel1Fragment())
        }

        btnLevel2.setOnClickListener {
            // menuScrollView.visibility = View.GONE
            // fragmentContainer.visibility = View.VISIBLE
            // launchFragment(Level2Fragment())
        }

        // The back button should go back to the menu, not exit the activity
        backButton.setOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
                menuScrollView.visibility = View.VISIBLE
                fragmentContainer.visibility = View.GONE
            } else {
                super.onBackPressed()
            }
        }
    }

    private fun launchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null) // This is important! It lets the user press "back" to return to the menu
            .commit()
    }

    // Handle the system back button press
    override fun onBackPressed() {
        val menuScrollView: ScrollView = findViewById(R.id.menuScrollView)
        val fragmentContainer: View = findViewById(R.id.fragment_container)
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            menuScrollView.visibility = View.VISIBLE
            fragmentContainer.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }
}
