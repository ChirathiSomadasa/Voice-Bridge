package com.chirathi.voicebridge

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val navigationView = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        navigationView.setOnItemSelectedListener  { menuItem ->
            when (menuItem.itemId) {
                R.id.fragment_home -> {
                    replaceFragment(HomeFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.fragment_notification -> {
                    replaceFragment(ProgressFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.fragment_profile -> {
                    replaceFragment(ProfileFragment())
                    return@setOnItemSelectedListener true
                }
                else -> false
            }
        }

        navigationView.selectedItemId = R.id.fragment_home
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

}