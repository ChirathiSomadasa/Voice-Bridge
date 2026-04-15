package com.chirathi.voicebridge

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.TimeUnit

class Wav2Vec2Scorer(private val context: Context) {

    // Hugging Face Space URL
    private val SERVER_URL = "https://chirathisomadasa-voice-bridge-server.hf.space/predict"

    // OkHttpClient with increased timeouts to handle slow network or server "cold starts"
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2

    // 1. Audio Recording Function
    fun recordAudio(durationMs: Long): FloatArray? {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize)
        val floatBuffer = FloatBuffer.allocate((SAMPLE_RATE * durationMs / 1000).toInt())
        val shortBuffer = ShortArray(minBufferSize / 2)

        try {
            recorder.startRecording()
            var samplesRead: Long = 0
            val totalSamples = SAMPLE_RATE * durationMs / 1000

            while (samplesRead < totalSamples) {
                val read = recorder.read(shortBuffer, 0, shortBuffer.size)
                if (read > 0) {
                    for (i in 0 until read) {
                        if (floatBuffer.hasRemaining()) {
                            // Volume Boost (x5) - Amplifies the audio input
                            val boosted = (shortBuffer[i] / 32768.0f) * 5.0f
                            floatBuffer.put(boosted.coerceIn(-1.0f, 1.0f))
                        }
                    }
                    samplesRead += read
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceBridge", "Recording Error", e)
            return null
        } finally {
            try { recorder.stop(); recorder.release() } catch (e: Exception) {}
        }
        return floatBuffer.array()
    }

    // 2. Server Prediction Function (Synchronous)
    // 🌟 වෙනස: Pair එක වෙනුවට Triple එකක් return කරයි (Status, Score, PredictedWord)
    fun predict(audioData: FloatArray, targetWord: String): Triple<String, Int, String> {
        try {
            Log.d("VoiceBridge", "Preparing to send audio to server...")

            // Convert FloatArray to WAV Byte Array
            val wavBytes = convertToWav(audioData)

            // Build the Multipart Request Body
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("word", targetWord)
                .addFormDataPart("audio", "audio.wav",
                    wavBytes.toRequestBody("audio/wav".toMediaTypeOrNull(), 0, wavBytes.size))
                .build()

            val request = Request.Builder()
                .url(SERVER_URL)
                .post(requestBody)
                .build()

            // Execute the request and wait for the response (Blocking Call)
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("VoiceBridge", "Server Error: ${response.code}")
                    return Triple("SERVER_ERROR", 0, "")
                }

                val responseBody = response.body?.string() ?: "{}"
                val json = JSONObject(responseBody)

                val score = json.optInt("score", 0)
                val status = json.optString("status", "POOR_PRONUNCIATION")
                // Backend එකෙන් එන predicted වචනය ලබා ගැනීම
                val predicted = json.optString("predicted", "")

                Log.d("VoiceBridge", "Result: $predicted | Score: $score")

                return Triple(status, score, predicted)
            }

        } catch (e: Exception) {
            Log.e("VoiceBridge", "Network Error", e)
            return Triple("CONNECTION_ERROR", 0, "")
        }
    }

    // Helper Function: Convert Float Array (PCM) to WAV File format
    private fun convertToWav(floatData: FloatArray): ByteArray {
        val byteData = ByteArray(floatData.size * 2)
        val buffer = ByteBuffer.wrap(byteData).order(ByteOrder.LITTLE_ENDIAN)
        for (f in floatData) {
            // Convert float (-1.0 to 1.0) to 16-bit PCM short
            val shortVal = (f * 32767).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            buffer.putShort(shortVal)
        }
        val pcmData = buffer.array()

        // Add the WAV Header so the server recognizes it as a valid audio file
        return addWavHeader(pcmData)
    }

    // Manually constructs the 44-byte WAV (RIFF) header
    private fun addWavHeader(pcmData: ByteArray): ByteArray {
        val header = ByteArray(44)
        val totalDataLen = pcmData.size + 36
        val bitrate = 16000 * 16 * 1 / 8

        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = 1; header[23] = 0
        header[24] = (16000 and 0xff).toByte()
        header[25] = ((16000 shr 8) and 0xff).toByte()
        header[26] = ((16000 shr 16) and 0xff).toByte()
        header[27] = ((16000 shr 24) and 0xff).toByte()
        header[28] = (bitrate and 0xff).toByte()
        header[29] = ((bitrate shr 8) and 0xff).toByte()
        header[30] = ((bitrate shr 16) and 0xff).toByte()
        header[31] = ((bitrate shr 24) and 0xff).toByte()
        header[32] = 2; header[33] = 0
        header[34] = 16; header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (pcmData.size and 0xff).toByte()
        header[41] = ((pcmData.size shr 8) and 0xff).toByte()
        header[42] = ((pcmData.size shr 16) and 0xff).toByte()
        header[43] = ((pcmData.size shr 24) and 0xff).toByte()

        return header + pcmData
    }
}