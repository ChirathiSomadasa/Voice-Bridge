package com.chirathi.voicebridge

import com.microsoft.cognitiveservices.speech.*
import com.microsoft.cognitiveservices.speech.audio.AudioConfig

class PronunciationAssesment {

    companion object {
        const val SPEECH_KEY = ""
        const val SPEECH_REGION = ""
    }

    fun assess(
        referenceText: String,
        onResult: (PronunciationAssessmentResult) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val speechConfig =
                    SpeechConfig.fromSubscription(SPEECH_KEY, SPEECH_REGION)

                speechConfig.speechRecognitionLanguage = "en-US"

                val audioConfig =
                    AudioConfig.fromDefaultMicrophoneInput()

                val recognizer =
                    SpeechRecognizer(speechConfig, audioConfig)

                val paConfig = PronunciationAssessmentConfig(
                    referenceText,
                    PronunciationAssessmentGradingSystem.HundredMark,
                    PronunciationAssessmentGranularity.Phoneme,
                    true
                )

                paConfig.applyTo(recognizer)

                // ANDROID-SAFE CALL
                val result = recognizer.recognizeOnceAsync().get()

                if (result.reason == ResultReason.RecognizedSpeech) {
                    val paResult =
                        PronunciationAssessmentResult.fromResult(result)
                    onResult(paResult)
                } else {
                    onError("Speech not recognized. Please try again.")
                }

                recognizer.close()

            } catch (e: Exception) {
                onError(e.message ?: "Speech error")
            }
        }.start()
    }
}
