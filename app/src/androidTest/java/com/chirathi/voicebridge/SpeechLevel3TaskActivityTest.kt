package com.chirathi.voicebridge

import android.Manifest
import androidx.test.espresso.Espresso.onView
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
class SpeechLevel3TaskActivityTest {

    // Automatically launch the Level 3 Activity before each test
    @get:Rule
    val activityRule = ActivityScenarioRule(SpeechLevel3TaskActivity::class.java)

    // Automatically grant microphone permissions
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @Test
    fun testInitialUIDisplayForSentences() {
        // Verify that the sentence text and its corresponding context image are displayed
        onView(withId(R.id.tvSentence)).check(matches(isDisplayed()))
        onView(withId(R.id.ivSentenceImage)).check(matches(isDisplayed()))

        // Verify that the Speak button is visible
        onView(withId(R.id.llSpeakSound)).check(matches(isDisplayed()))
    }

    @Test
    fun testNextButtonDisabledBeforeSpeaking() {
        // The Next button must be disabled initially
        onView(withId(R.id.btnNext)).check(matches(not(isEnabled())))
    }
}