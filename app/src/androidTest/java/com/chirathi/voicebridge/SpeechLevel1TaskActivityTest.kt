package com.chirathi.voicebridge

import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.CoreMatchers.not

@RunWith(AndroidJUnit4::class)
class SpeechLevel1TaskActivityTest {

    // Automatically launch the Level 1 Activity before each test
    @get:Rule
    val activityRule = ActivityScenarioRule(SpeechLevel1TaskActivity::class.java)

    // Automatically grant microphone permissions to avoid test failure
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @Test
    fun testInitialUIDisplayForLetters() {
        // Verify that the TextView displaying the target letter is visible
        onView(withId(R.id.tvLetter)).check(matches(isDisplayed()))

        // Verify that the Speak button is displayed and clickable
        onView(withId(R.id.llSpeakSound)).check(matches(isDisplayed()))
        onView(withId(R.id.llSpeakSound)).check(matches(isClickable()))

        // Verify that the Play (TTS) button is displayed and clickable
        onView(withId(R.id.llPlaySound)).check(matches(isDisplayed()))
        onView(withId(R.id.llPlaySound)).check(matches(isClickable()))
    }

    @Test
    fun testNextButtonIsInitiallyDisabled() {
        // The Next button should remain disabled to prevent skipping before the child speaks
        onView(withId(R.id.btnNext)).check(matches(not(isEnabled())))
    }
}