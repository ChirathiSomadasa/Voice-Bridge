package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class PhraseActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speakerBtn: ImageView? = null
    private var phraseText: TextView? = null
    private var selectedPhrase: String = "I want to play"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phrase)

        // Get the passed data from intent
        val selectedIconDrawable = intent.getIntExtra("SELECTED_ICON_DRAWABLE", R.drawable.play)
        selectedPhrase = intent.getStringExtra("SELECTED_PHRASE") ?: "I want to play"

        // Initialize views
        val quickWordImage = findViewById<ImageView>(R.id.imgQuickWord)
        phraseText = findViewById<TextView>(R.id.tvPhrase)
        val refreshBtn = findViewById<ImageView>(R.id.ivRefresh)
        speakerBtn = findViewById<ImageView>(R.id.ivSpeaker)

        // Set the icon and phrase based on what was clicked
        quickWordImage.setImageResource(selectedIconDrawable)
        phraseText?.text = selectedPhrase

        // Initialize TextToSpeech with specific engine if needed
        tts = TextToSpeech(this, this)

        speakerBtn?.isEnabled = false
        refreshBtn.setOnClickListener { finish() }
        speakerBtn?.setOnClickListener { speakOut() }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setChildLikeVoice()
        } else {
            Log.e("TTS", "Initialization Failed!")
        }
    }

    private fun setChildLikeVoice() {
        val result = tts?.setLanguage(Locale.US)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TTS", "The Language is not supported!")
            return
        }

        // Method 1: Try to find a child-like voice (Android 21+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            try {
                // Get available voices
                val voices = tts!!.voices

                // Look for child-like voices - common names include "child", "kids", "young", etc.
                val childVoice = voices.find { voice ->
                    voice.name.contains("child", ignoreCase = true) ||
                            voice.name.contains("kids", ignoreCase = true) ||
                            voice.name.contains("young", ignoreCase = true) ||
                            voice.name.contains("boy", ignoreCase = true) ||
                            voice.name.contains("girl", ignoreCase = true)
                }

                if (childVoice != null) {
                    tts!!.voice = childVoice
                    Log.d("TTS", "Using child voice: ${childVoice.name}")
                } else {
                    // If no child voice found, adjust pitch and speech rate
                    setVoiceParameters()
                }
            } catch (e: Exception) {
                setVoiceParameters()
            }
        } else {
            // For older Android versions, use parameter method
            setVoiceParameters()
        }

        speakerBtn?.isEnabled = true
    }

    private fun setVoiceParameters() {
        try {
            // Higher pitch for child-like voice (1.0 is normal, >1.0 is higher)
            tts?.setPitch(1.3f)  // Higher pitch sounds more child-like

            // Slightly faster speech rate for energetic child voice
            tts?.setSpeechRate(1.1f)  // 1.0 is normal rate

            Log.d("TTS", "Voice parameters set: Pitch=1.3, Rate=1.1")
        } catch (e: Exception) {
            Log.e("TTS", "Error setting voice parameters: ${e.message}")
        }
    }

    private fun speakOut() {
        val text = selectedPhrase
        try {
            // For Android 21+ with additional control
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                //tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
                @Suppress("DEPRECATION")
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
            }
        } catch (e: Exception) {
            Log.e("TTS", "Error speaking: ${e.message}")
        }
    }

    public override fun onDestroy() {
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        tts?.stop()
    }
}