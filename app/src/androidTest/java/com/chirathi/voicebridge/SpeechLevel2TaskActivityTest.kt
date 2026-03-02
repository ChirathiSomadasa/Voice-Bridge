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
class SpeechLevel2TaskActivityTest {

    // 1. Launch the Activity automatically before testing
    @get:Rule
    val activityRule = ActivityScenarioRule(SpeechLevel2TaskActivity::class.java)

    // 2. Grant Microphone permission automatically for the test
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @Test
    fun testInitialUIDisplay() {
        // Check if the Text View for the word is displayed
        onView(withId(R.id.tvWord)).check(matches(isDisplayed()))

        // Check if the Image View is displayed
        onView(withId(R.id.ivWordImage)).check(matches(isDisplayed()))

        // Check if the Speak Button is displayed and clickable
        onView(withId(R.id.llSpeakSound)).check(matches(isDisplayed()))
        onView(withId(R.id.llSpeakSound)).check(matches(isClickable()))
    }

    @Test
    fun testPlaySoundButtonClick() {
        // Simulate a user clicking the "Play Sound" button
        onView(withId(R.id.llPlaySound)).perform(click())

        // Since TTS runs in the background, we verify that the app doesn't crash
        // and the target word is still visible.
        onView(withId(R.id.tvWord)).check(matches(isDisplayed()))
    }

    @Test
    fun testNextButtonIsInitiallyDisabled() {
        // The Next button should be disabled until the user speaks
        onView(withId(R.id.btnNext)).check(matches(not(isEnabled())))
    }
}