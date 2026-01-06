package com.chirathi.voicebridge

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import androidx.fragment.app.DialogFragment

class FeedbackDialogFragment : DialogFragment() {

    // Interface for communication
    interface FeedbackCompletionListener {
        fun onFeedbackCompleted(correctAnswers: Int, totalRounds: Int, score: Int)
    }

    // Views
    private lateinit var videoView: VideoView
    private lateinit var titleText: TextView
    private lateinit var progressBar: ProgressBar

    // Handler for timer
    private lateinit var handler: Handler
    private var progressRunnable: Runnable? = null
    private val FEEDBACK_DURATION = 8000L // 8 seconds

    // Variables to store game data
    private var isGoodFeedback = false
    private var correctAnswers = 0
    private var totalRounds = 0
    private var score = 0

    // Listener for completion callback
    private var completionListener: FeedbackCompletionListener? = null

    companion object {
        private const val TAG = "FeedbackDialog"

        // Factory method to create new instance
        fun newInstance(isGood: Boolean, correctAnswers: Int, totalRounds: Int, score: Int): FeedbackDialogFragment {
            val fragment = FeedbackDialogFragment()
            val args = Bundle().apply {
                putBoolean("is_good", isGood)
                putInt("correct_answers", correctAnswers)
                putInt("total_rounds", totalRounds)
                putInt("score", score)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout
        val view = inflater.inflate(R.layout.dialog_feedback, container, false)

        // Make dialog background transparent and non-cancelable
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "=== DIALOG CREATED ===")

        // Initialize views
        videoView = view.findViewById(R.id.feedbackVideo)
        titleText = view.findViewById(R.id.title)
        progressBar = view.findViewById(R.id.progressBar)

        // Get data passed to the dialog
        isGoodFeedback = arguments?.getBoolean("is_good", false) ?: false
        correctAnswers = arguments?.getInt("correct_answers", 0) ?: 0
        totalRounds = arguments?.getInt("total_rounds", 5) ?: 5
        score = arguments?.getInt("score", 0) ?: 0

        Log.d(TAG, "Feedback type: ${if (isGoodFeedback) "GOOD" else "BAD"}")
        Log.d(TAG, "Correct answers: $correctAnswers, Total rounds: $totalRounds, Score: $score")

        // Set title based on feedback type
        if (isGoodFeedback) {
            titleText.text = "Wow! You got them!"
        } else {
            titleText.text = "You're learning – let's play more!"
        }

        // Setup click listener - optional (comment out if you don't want early dismissal)
        view.setOnClickListener {
            Log.d(TAG, "Dialog clicked - completing early")
            completeFeedback()
        }

        // Initialize handler
        handler = Handler(Looper.getMainLooper())

        // Setup video
        setupVideo()
    }

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        if (context is FeedbackCompletionListener) {
            completionListener = context
            Log.d(TAG, "Completion listener attached successfully")
        } else {
            Log.e(TAG, "Parent activity must implement FeedbackCompletionListener")
        }
    }

    private fun setupVideo() {
        try {
            // Determine which video to play
            val videoRes = if (isGoodFeedback) {
                R.raw.feedback_good
            } else {
                R.raw.feedback_bad
            }

            Log.d(TAG, "Loading video resource ID: $videoRes")

            // Setup video URI
            val videoUri = Uri.parse("android.resource://" + requireActivity().packageName + "/" + videoRes)
            videoView.setVideoURI(videoUri)

            // Set video error listener
            videoView.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "Video error - what: $what, extra: $extra")
                // If video fails, still proceed with timer
                handler.postDelayed({
                    startProgressAndTimer()
                }, 500)
                true
            }

            // Play video and loop it
            videoView.setOnPreparedListener { mediaPlayer ->
                Log.d(TAG, "Video prepared successfully - starting playback")
                mediaPlayer.isLooping = true
                videoView.start()

                // Start progress animation and timer after video is ready
                startProgressAndTimer()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up video: ${e.message}")
            e.printStackTrace()
            // Start progress even if video fails
            startProgressAndTimer()
        }
    }

    private fun startProgressAndTimer() {
        Log.d(TAG, "Starting progress animation and $FEEDBACK_DURATION ms timer")

        // Start progress bar animation
        startProgressAnimation()

        // Start timer for auto-close
        handler.postDelayed({
            Log.d(TAG, "Timer completed - completing feedback")
            completeFeedback()
        }, FEEDBACK_DURATION)
    }

    private fun startProgressAnimation() {
        progressBar.progress = 0
        progressRunnable = object : Runnable {
            private var progress = 0
            override fun run() {
                progress += 1
                progressBar.progress = progress
                if (progress < 100) {
                    // Update every 80ms for 8 seconds total (8000/100 = 80)
                    handler.postDelayed(this, 80)
                } else {
                    Log.d(TAG, "Progress bar reached 100%")
                }
            }
        }
        // Start the progress animation
        progressRunnable?.let {
            handler.post(it)
        }
    }

    private fun completeFeedback() {
        Log.d(TAG, "=== COMPLETE FEEDBACK ===")

        // Stop all handlers first
        progressRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacksAndMessages(null)

        // Stop video playback
        videoView.stopPlayback()

        // Dismiss the dialog first
        Log.d(TAG, "Dismissing dialog...")
        dismiss()

        // Notify listener after dismiss (dialog will still be attached briefly)
        Log.d(TAG, "Notifying completion listener...")
        completionListener?.onFeedbackCompleted(correctAnswers, totalRounds, score)

        Log.d(TAG, "Feedback completion process finished")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
        // Clean up when dialog is paused
        progressRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacksAndMessages(null)
        videoView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        // Clean up when dialog is destroyed
        progressRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacksAndMessages(null)
        videoView.stopPlayback()
        completionListener = null
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "onDetach called")
        completionListener = null
    }
}