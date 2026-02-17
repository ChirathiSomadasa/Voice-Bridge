package com.chirathi.voicebridge

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(context: Context) {
    private var soundPool: SoundPool
    private var clickSoundId: Int = 0

    init {
        // Configure AudioAttributes for low latency, instant playback
        // USAGE_GAME and CONTENT_TYPE_SONIFICATION are ideal for short UI sounds
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)// Allow up to 3 sounds to play simultaneously without cutting each other off
            .setAudioAttributes(audioAttributes)
            .build()

        // Load the 'click' audio file from the res/raw folder into memory
        clickSoundId = soundPool.load(context, R.raw.click, 1)
    }

    // Call this function inside your button click listeners to play the sound
    fun playClickSound() {
        // Parameters: soundID, leftVolume, rightVolume, priority, loop, rate
        soundPool.play(clickSoundId, 1f, 1f, 0, 0, 1f)
    }

    // Release the SoundPool resources to prevent memory leaks
    // Always call this inside the Activity's onDestroy() method
    fun release() {
        soundPool.release()
    }
}