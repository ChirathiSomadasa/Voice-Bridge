package com.chirathi.voicebridge

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AITherapyTasksPerformanceTest {

    private var scenario: ActivityScenario<AITherapyTasksActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun startup_under2s_ageFlow() {
        val start = System.currentTimeMillis()

        val intent = Intent(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
            AITherapyTasksActivity::class.java
        ).apply {
            putExtra("AGE", 6)
            putExtra("DISORDER", "Stuttering")
        }

        scenario = ActivityScenario.launch<AITherapyTasksActivity>(intent).also {
            it.onActivity { /* activity is created and visible */ }
        }

        val duration = System.currentTimeMillis() - start
        println(
            """
            =========================================================
            PERFORMANCE TEST RESULT
            Activity   : AITherapyTasksActivity
            Entry Flow : Age/Disorder intent extras
            Load Time  : $duration ms
            =========================================================
            """.trimIndent()
        )

        assertTrue("Startup too slow! Took $duration ms", duration < 2000)
    }

    @Test
    fun startup_under2s_textDescriptionFlow() {
        val start = System.currentTimeMillis()

        val intent = Intent(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
            AITherapyTasksActivity::class.java
        ).apply {
            putExtra("TEXT_DESCRIPTION", "Child struggles with /s/ sound in sentences.")
            putExtra("DISORDER", "Stuttering")
        }

        scenario = ActivityScenario.launch<AITherapyTasksActivity>(intent).also {
            it.onActivity { /* activity is created and visible */ }
        }

        val duration = System.currentTimeMillis() - start
        println(
            """
            =========================================================
            PERFORMANCE TEST RESULT
            Activity   : AITherapyTasksActivity
            Entry Flow : Text description intent extra
            Load Time  : $duration ms
            =========================================================
            """.trimIndent()
        )

        assertTrue("Startup too slow! Took $duration ms", duration < 2000)
    }
}