package com.chirathi.voicebridge

import android.graphics.Bitmap
import org.junit.Test
import org.junit.Assert.assertTrue

class CameraProcessingPerformanceTest {

    @Test
    fun testBitmapProcessingSpeed() {

        val bitmap =
            Bitmap.createBitmap(640,640,Bitmap.Config.ARGB_8888)

        val start = System.currentTimeMillis()

        val resized =
            Bitmap.createScaledBitmap(bitmap,320,320,true)

        val end = System.currentTimeMillis()

        val time = end - start

        println("Bitmap processing time: $time ms")

        assertTrue(time < 500)
    }
}