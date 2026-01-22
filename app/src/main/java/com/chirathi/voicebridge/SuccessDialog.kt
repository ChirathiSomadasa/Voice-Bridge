package com.chirathi.voicebridge

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.Window
import android.widget.Button
import com.lottiefiles.dotlottie.core.model.Config
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.lottiefiles.dotlottie.core.widget.DotLottieAnimation

class SuccessDialog(context: Context) : Dialog(context) {

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_success, null)

        setContentView(view)
        setCancelable(false)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        val lottie = view.findViewById<DotLottieAnimation>(R.id.lottieSuccess)
        val btnContinue = view.findViewById<Button>(R.id.btn_success_ok)

        // Lottie configuration
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

        btnContinue.setOnClickListener {
            dismiss()
        }
    }
}
