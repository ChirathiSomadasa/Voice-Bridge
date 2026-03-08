package com.chirathi.voicebridge

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class MoodMatchPerformanceTest {

    @Test
    fun measureActivityStartupTime() {
        // 1. Record the exact time before launching the activity
        val startTime = System.currentTimeMillis()

        // 2. Launch MoodMatchSevenDownActivity with age group 6
        val intent = Intent(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
            MoodMatchSevenDownActivity::class.java
        ).putExtra("AGE_GROUP", 6)

        val scenario = ActivityScenario.launch<MoodMatchSevenDownActivity>(intent)

        scenario.onActivity {
            // 3. Record the time once the activity is fully created and visible
            val endTime = System.currentTimeMillis()

            // 4. Calculate load time (Time To Initial Display)
            val loadTimeMs = endTime - startTime

            // Print result clearly in the Run Console
            println("=========================================================")
            println("PERFORMANCE TEST RESULT:")
            println("Activity Name : MoodMatchSevenDownActivity")
            println("Age Group     : 6 (Young — 2-button layout)")
            println("Load Time     : $loadTimeMs milliseconds")
            println("=========================================================")

            // 5. Activity MUST load in under 2 000 ms to pass
            assertTrue("Activity loaded too slowly! Took $loadTimeMs ms", loadTimeMs < 2000)
        }

        scenario.close()
    }

    @Test
    fun measureActivityStartupTime_olderGroup() {
        val startTime = System.currentTimeMillis()

        val intent = Intent(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
            MoodMatchSevenDownActivity::class.java
        ).putExtra("AGE_GROUP", 8)

        val scenario = ActivityScenario.launch<MoodMatchSevenDownActivity>(intent)

        scenario.onActivity {
            val endTime   = System.currentTimeMillis()
            val loadTimeMs = endTime - startTime

            println("=========================================================")
            println("PERFORMANCE TEST RESULT:")
            println("Activity Name : MoodMatchSevenDownActivity")
            println("Age Group     : 8 (Older — 4-button grid layout)")
            println("Load Time     : $loadTimeMs milliseconds")
            println("=========================================================")

            assertTrue("Activity loaded too slowly! Took $loadTimeMs ms", loadTimeMs < 2000)
        }

        scenario.close()
    }
}