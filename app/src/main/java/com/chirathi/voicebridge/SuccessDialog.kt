package com.chirathi.voicebridge

import android.app.Dialog
import android.content.Context
import android.media.MediaPlayer // Import MediaPlayer for playing audio
import android.view.LayoutInflater
import android.view.Window
import android.widget.Button
import com.lottiefiles.dotlottie.core.model.Config
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.lottiefiles.dotlottie.core.widget.DotLottieAnimation

class SuccessDialog(context: Context) : Dialog(context) {

    // Declare a MediaPlayer variable to handle the "win" audio playback
    private var mediaPlayer: MediaPlayer? = null

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_success, null)

        setContentView(view)

        // Prevent the user from canceling the dialog by tapping outside of it
        setCancelable(false)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        val lottie = view.findViewById<DotLottieAnimation>(R.id.lottieSuccess)
        val btnContinue = view.findViewById<Button>(R.id.btn_success_ok)

        // Lottie configuration for the visual confetti animation
        val config = Config.Builder()
            .autoplay(true)
            .loop(true)
            .speed(1.2f)
            .source(
                DotLottieSource.Url(
                    "https://lottie.host/7695586d-21ee-4258-b0ee-9b803da98d4f/Qmrkrt5dWo.lottie"
                )
            )
            .build()

        lottie.load(config)

        // Initialize and play the "win.mp3" sound from the res/raw folder
        try {
            mediaPlayer = MediaPlayer.create(context, R.raw.win)
            mediaPlayer?.start() // Play the sound immediately when the dialog opens
        } catch (e: Exception) {
            e.printStackTrace() // Log any errors if the audio fails to load
        }

        // Handle the continue button click
        btnContinue.setOnClickListener {
            dismiss() // This will trigger the overridden dismiss() method below
        }
    }

    override fun dismiss() {
        // Safely stop and release the MediaPlayer resources
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null // Clear the reference

        // Call the parent class's dismiss method to actually close the dialog
        super.dismiss()
    }
}