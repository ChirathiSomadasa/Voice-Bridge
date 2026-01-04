package com.chirathi.voicebridge

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.Window
import com.lottiefiles.dotlottie.core.model.Config
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.lottiefiles.dotlottie.core.widget.DotLottieAnimation

class ProcessingDialog(context: Context) : Dialog(context) {

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_processing, null)
        setContentView(view)
        setCancelable(false)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        val animation = view.findViewById<DotLottieAnimation>(R.id.lottieProcessing)

        val config = Config.Builder()
            .autoplay(true)
            .speed(1.5f)
            .loop(true)
            .source(DotLottieSource.Asset("processing_loader.json"))
            .build()

        animation.load(config)
    }
}