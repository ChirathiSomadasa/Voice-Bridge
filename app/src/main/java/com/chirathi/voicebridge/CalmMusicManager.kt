package com.chirathi.voicebridge

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Singleton that owns the single calm-background MediaPlayer.
 *
 * Call onActivityResume() / onActivityPause() from every music-enabled activity.
 * A 300 ms pause delay prevents the music cutting out during A→B transitions.
 */
object CalmMusicManager {

    private const val TAG          = "CalmMusicManager"
    private const val PREFS_NAME   = "game_settings_prefs"
    private const val KEY_ENABLED  = "pref_calm_music_enabled"

    private var player: MediaPlayer? = null
    private var hasCalmResource      = false
    private var isInitialised        = false

    private val handler       = Handler(Looper.getMainLooper())
    private var pauseRunnable : Runnable? = null

    // ------------------------------------------------------------------
    // Called once from GameDashboardActivity.setupGameSettings()
    // ------------------------------------------------------------------

    fun init(context: Context) {
        hasCalmResource = try {
            context.resources.openRawResourceFd(R.raw.calm_background) != null
        } catch (e: Exception) {
            Log.w(TAG, "calm_background not found in res/raw: ${e.message}")
            false
        }
        isInitialised = true
        Log.d(TAG, "init complete — hasCalmResource=$hasCalmResource")
    }

    // ------------------------------------------------------------------
    // Activity lifecycle hooks
    // ------------------------------------------------------------------

    fun onActivityResume(context: Context) {
        if (!isInitialised) init(context)

        // Cancel any pending pause from the previous activity's onPause
        pauseRunnable?.let { handler.removeCallbacks(it) }
        pauseRunnable = null

        val enabled = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

        if (enabled && hasCalmResource) {
            start(context)
        }
        // If disabled, leave player paused — don't start it
    }

    fun onActivityPause() {
        // Delay so A→B transitions don't produce a audible cut
        val r = Runnable {
            player?.pause()
            pauseRunnable = null
        }
        pauseRunnable = r
        handler.postDelayed(r, 300)
    }

    // ------------------------------------------------------------------
    // Called from GameDashboardActivity after settings dialog "Done"
    // ------------------------------------------------------------------

    fun applySettings(context: Context) {
        val enabled = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

        if (enabled && hasCalmResource) start(context) else stop()
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private fun start(context: Context) {
        try {
            if (player == null) {
                player = MediaPlayer.create(context.applicationContext, R.raw.calm_background)
                    ?.apply { isLooping = true }
            }
            applySystemVolume(context)
            if (player?.isPlaying == false) player?.start()
        } catch (e: Exception) {
            Log.e(TAG, "start failed: ${e.message}")
        }
    }

    fun stop() {
        try {
            player?.pause()
            player?.seekTo(0)
        } catch (e: Exception) {
            Log.e(TAG, "stop failed: ${e.message}")
        }
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        try {
            player?.release()
        } catch (_: Exception) {}
        player = null
    }

    fun applySystemVolume(context: Context) {
        val am    = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max   = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val cur   = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val ratio = if (max > 0) cur.toFloat() / max else 0.4f
        player?.setVolume(ratio, ratio)
    }
}