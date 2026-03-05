package com.chirathi.voicebridge

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class AppStartupPerformanceTest {

    @Test
    fun measureActivityStartupTime() {
        // 1. Record the exact time before launching the activity
        val startTime = System.currentTimeMillis()

        // 2. Launch the SpeechLevel1TaskActivity
        val scenario = ActivityScenario.launch(SpeechLevel1TaskActivity::class.java)

        scenario.onActivity {
            // 3. Record the time immediately after the activity is fully created and visible
            val endTime = System.currentTimeMillis()

            // 4. Calculate the Total Load Time (Time To Initial Display - TTID)
            val loadTimeMs = endTime - startTime

            // Print the result clearly in the Run Console
            println("=========================================================")
            println("PERFORMANCE TEST RESULT:")
            println("Activity Name : SpeechLevel1TaskActivity")
            println("Load Time     : $loadTimeMs milliseconds")
            println("=========================================================")

            // 5. Assertion: The activity MUST load in less than 2000ms (2 seconds)
            // to be considered "Good Performance"
            assertTrue("Activity loaded too slowly! Took $loadTimeMs ms", loadTimeMs < 2000)
        }

        // Clean up
        scenario.close()
    }
}