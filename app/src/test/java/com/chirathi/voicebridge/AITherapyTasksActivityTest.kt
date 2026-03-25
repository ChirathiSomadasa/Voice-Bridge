package com.chirathi.voicebridge

import android.content.Intent
import android.os.Looper
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.chirathi.voicebridge.api.models.TherapyTask
import com.chirathi.voicebridge.repository.AIRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AITherapyTasksActivityTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val intent = Intent().apply {
        putExtra("AGE", 6)
        putExtra("DISORDER", "Speech Sound Disorder")
    }

    @Before
    fun setUp() {
        mockkConstructor(AIRepository::class)
        mockkConstructor(Edu_GeminiHelper::class)
    }

    @After
    fun tearDown() {
        io.mockk.unmockkAll()
    }

    // Helper to build a relaxed TherapyTask mock with the fields we assert on
    private fun sampleTask(title: String = "Bubble Breathing"): TherapyTask {
        val task = mockk<TherapyTask>(relaxed = true)
        every { task.title } returns title
        every { task.activity } returns "Blow bubbles to practice airflow"
        every { task.ageGroup } returns "6-7"
        every { task.disorder } returns "Speech"
        every { task.goal } returns "Improve /s/ sound"
        every { task.materials } returns "Bubble wand"
        return task
    }

    @Test
    fun loadAIRecommendations_success_showsListAndSubtitle() {
        // Arrange
        val tasks = listOf(sampleTask("Task A"), sampleTask("Task B"))
        coEvery { anyConstructed<AIRepository>().getRecommendationsByAge(6, "Speech Sound Disorder") } returns tasks

        val controller = Robolectric.buildActivity(AITherapyTasksActivity::class.java, intent)

        // Act
        val activity = controller.setup().get()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Assert
        val recycler = activity.findViewById<RecyclerView>(R.id.rvLessons)
        val subtitle = activity.findViewById<View>(R.id.tvAiSubtitle)

        assert(recycler.visibility == View.VISIBLE)
        assert(subtitle.visibility == View.VISIBLE)
        assert((recycler.adapter?.itemCount ?: 0) == 2)
    }

    @Test
    fun loadAIRecommendations_empty_showsEmptyMessage() {
        // Arrange
        coEvery { anyConstructed<AIRepository>().getRecommendationsByAge(6, "Speech Sound Disorder") } returns emptyList()

        val controller = Robolectric.buildActivity(AITherapyTasksActivity::class.java, intent)

        // Act
        val activity = controller.setup().get()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Assert
        val subtitleText = activity.findViewById<android.widget.TextView>(R.id.tvAiSubtitle)
        assert(subtitleText.visibility == View.VISIBLE)
        assert(subtitleText.text.contains("No recommendations found", ignoreCase = true))
    }

    @Test
    fun loadAIRecommendations_error_showsFailureMessage() {
        // Arrange
        coEvery { anyConstructed<AIRepository>().getRecommendationsByAge(any(), any()) } throws RuntimeException("Network down")

        val controller = Robolectric.buildActivity(AITherapyTasksActivity::class.java, intent)

        // Act
        val activity = controller.setup().get()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Assert
        val subtitleText = activity.findViewById<android.widget.TextView>(R.id.tvAiSubtitle)
        assert(subtitleText.visibility == View.VISIBLE)
        assert(subtitleText.text.contains("Failed to load", ignoreCase = true))
    }

    @Test
    fun showTaskDetails_prefersGeminiOutputWhenAvailable() {
        // Arrange
        val task = sampleTask("Rainbow Reading")
        coEvery { anyConstructed<Edu_GeminiHelper>().generateTherapyDetail(task) } returns "AI DETAIL TEXT"

        val activity = Robolectric.buildActivity(AITherapyTasksActivity::class.java, intent).setup().get()

        // Act
        activity.runOnUiThread { activity.showTaskDetails(task) }
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Assert
        val latestDialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
        val message = latestDialog?.findViewById<android.widget.TextView>(android.R.id.message)?.text?.toString()

        assert(message?.contains("AI DETAIL TEXT") == true)
    }
}