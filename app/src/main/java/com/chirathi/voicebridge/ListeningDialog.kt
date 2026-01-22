package com.chirathi.voicebridge

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import com.lottiefiles.dotlottie.core.model.Config
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.lottiefiles.dotlottie.core.widget.DotLottieAnimation

class ListeningDialog(context: Context) : Dialog(context) {

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_listening, null)
        setContentView(view)

        setCancelable(false)

        window?.apply {

            setBackgroundDrawableResource(android.R.color.transparent)

            setGravity(Gravity.BOTTOM)

            attributes = attributes.apply {
                y = 40
            }
        }

        val animation = view.findViewById<DotLottieAnimation>(R.id.lottieListening)

        val config = Config.Builder()
            .autoplay(true)
            .speed(1.2f)
            .loop(true)
            .source(DotLottieSource.Asset("listen_state.json"))
            .build()

        animation.load(config)
    }
}
