package com.chirathi.voicebridge

import android.Manifest
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhraseActivityTest {


    @Test
    fun testGreetingTextView() {
        val scenario = ActivityScenario.launch(PhraseActivity::class.java)
        scenario.onActivity { activity ->
            val tvGreeting = activity.findViewById<TextView>(R.id.tvGreeting)
            assertNotNull(tvGreeting)
            assertTrue(tvGreeting.text.isEmpty() || tvGreeting.text.isNotBlank())
        }
    }

    @Test
    fun testPhraseTextView() {
        val scenario = ActivityScenario.launch(PhraseActivity::class.java)
        scenario.onActivity { activity ->
            val tvPhrase = activity.findViewById<TextView>(R.id.tvPhrase)
            assertNotNull(tvPhrase)
            assertTrue(tvPhrase.text.isNotEmpty())
        }
    }

    @Test
    fun testImage() {
        val scenario = ActivityScenario.launch(PhraseActivity::class.java)
        scenario.onActivity { activity ->
            val imgQuickWord = activity.findViewById<ImageView>(R.id.imgQuickWord)
            assertNotNull(imgQuickWord)
            assertEquals(android.view.View.VISIBLE, imgQuickWord.visibility)
        }
    }

    @Test
    fun testClickableLayouts() {
        val scenario = ActivityScenario.launch(PhraseActivity::class.java)
        scenario.onActivity { activity ->
            val speakerLayout = activity.findViewById<LinearLayout>(R.id.Speaker)
            val refreshLayout = activity.findViewById<LinearLayout>(R.id.refresh)
            assertNotNull(speakerLayout)
            assertNotNull(refreshLayout)
            assertTrue(speakerLayout.isClickable)
            assertTrue(refreshLayout.isClickable)
        }
    }
}