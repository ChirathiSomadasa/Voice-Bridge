package com.chirathi.voicebridge

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MoodMatchSevenDownActivityTest {

    @get:Rule
    val activityRule: ActivityScenarioRule<MoodMatchSevenDownActivity> =
        ActivityScenarioRule(
            Intent(
                InstrumentationRegistry.getInstrumentation().targetContext,
                MoodMatchSevenDownActivity::class.java
            ).putExtra("AGE_GROUP", 6)
        )

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: Initial UI elements are displayed correctly
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testInitialUIDisplay() {
        onView(withId(R.id.emotionImage)).check(matches(isDisplayed()))

        onView(withId(R.id.btnOption1)).check(matches(isDisplayed()))
        onView(withId(R.id.btnOption1)).check(matches(isClickable()))
        onView(withId(R.id.btnOption2)).check(matches(isDisplayed()))
        onView(withId(R.id.btnOption2)).check(matches(isClickable()))

        onView(withId(R.id.soundOption1)).check(matches(isDisplayed()))
        onView(withId(R.id.soundOption2)).check(matches(isDisplayed()))

        onView(withId(R.id.pandaImage)).check(matches(isDisplayed()))
        onView(withId(R.id.guessText)).check(matches(isDisplayed()))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: Score and round counters start at correct values
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testInitialScoreAndRound() {
        onView(withId(R.id.tvScore)).check(matches(withText("Score: 0")))
        onView(withId(R.id.tvRound)).check(matches(withText("Round: 1/5")))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: Next button is hidden until an answer is selected
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testNextButtonIsInitiallyHidden() {
        onView(withId(R.id.btnNext)).check(matches(not(isDisplayed())))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4: Young group (age 6) shows 2-button layout, not 4-button grid
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testYoungGroupLayoutIsCorrect() {
        onView(withId(R.id.layoutYoung)).check(matches(isDisplayed()))
        onView(withId(R.id.layoutOlder)).check(matches(not(isDisplayed())))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 5: Older group (age 8) shows 4-button grid, not 2-button layout
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testOlderGroupLayoutIsCorrect() {
        val olderIntent = Intent(
            InstrumentationRegistry.getInstrumentation().targetContext,
            MoodMatchSevenDownActivity::class.java
        ).putExtra("AGE_GROUP", 8)

        ActivityScenario.launch<MoodMatchSevenDownActivity>(olderIntent).use {
            onView(withId(R.id.layoutOlder)).check(matches(isDisplayed()))
            onView(withId(R.id.layoutYoung)).check(matches(not(isDisplayed())))
            onView(withId(R.id.btnGrid1)).check(matches(isDisplayed()))
            onView(withId(R.id.btnGrid2)).check(matches(isDisplayed()))
            onView(withId(R.id.btnGrid3)).check(matches(isDisplayed()))
            onView(withId(R.id.btnGrid4)).check(matches(isDisplayed()))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 6: Back button is visible on screen
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun testBackButtonIsVisible() {
        onView(withId(R.id.backBtn)).check(matches(isDisplayed()))
    }
}