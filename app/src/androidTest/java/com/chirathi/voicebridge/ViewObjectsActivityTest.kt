package com.chirathi.voicebridge

import android.Manifest
import android.widget.Button
import android.widget.TextView
import androidx.camera.view.PreviewView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewObjectsActivityTest {

    // Grant camera permission for tests
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA
    )

    @Test
    fun testPreviewViewExists() {
        val scenario = ActivityScenario.launch(ViewObjectsActivity::class.java)
        scenario.onActivity { activity ->
            val preview = activity.findViewById<PreviewView>(R.id.previewView)
            assertNotNull(preview)
        }
    }

    @Test
    fun testResultTextIsDetecting() {
        val scenario = ActivityScenario.launch(ViewObjectsActivity::class.java)
        scenario.onActivity { activity ->
            val resultText = activity.findViewById<TextView>(R.id.resultText)
            assertNotNull(resultText)
            assertEquals("Detecting...", resultText.text.toString())
        }
    }

    @Test
    fun testButtonsExist() {
        val scenario = ActivityScenario.launch(ViewObjectsActivity::class.java)
        scenario.onActivity { activity ->
            val btnIWant = activity.findViewById<Button>(R.id.btnIWant)
            val btnISee = activity.findViewById<Button>(R.id.btnISee)
            val btnThisIs = activity.findViewById<Button>(R.id.btnThisIs)

            assertNotNull(btnIWant)
            assertNotNull(btnISee)
            assertNotNull(btnThisIs)
        }
    }

}