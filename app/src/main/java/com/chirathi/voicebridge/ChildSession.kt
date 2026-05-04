package com.chirathi.voicebridge

import android.content.Context
import android.util.Log

/**
 * ChildSession — Global current-child context.
 *
 * INITIALIZATION POINT: GameDashboardActivity.fetchAgeAndLaunch()
 * This is the earliest point where Firebase age is available without
 * requiring changes to LoginActivity or ProfileFragment.
 *
 * All three game activities read from here instead of using hardcoded
 * childId=0 or AGE_GROUP=6 constants.
 */
object ChildSession {
    private const val TAG        = "ChildSession"
    private const val PREFS_NAME = "current_child_session"

    var childId:  Int = 0; private set
    var age:      Int = 6; private set
    var ageGroup: Int = 6; private set   // 6 = group 6-7, 8 = group 8-10

    /**
     * Called from GameDashboardActivity after Firebase returns the child's age.
     * Persists to SharedPreferences so it survives activity recreation.
     */
    fun set(context: Context, uid: String, childAge: Int) {
        // Derive a stable non-negative integer childId from the Firebase UID
        childId  = (uid.hashCode() and 0x7FFF).coerceAtLeast(1)
        age      = childAge.coerceIn(6, 10)
        ageGroup = if (age >= 8) 8 else 6

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt("childId",  childId)
            .putInt("age",      age)
            .putInt("ageGroup", ageGroup)
            .apply()

        Log.d(TAG, "✅ ChildSession set: uid=$uid childId=$childId age=$age ageGroup=$ageGroup")
    }

    /**
     * Restores from SharedPreferences. Call in any activity's onCreate()
     * as a safety net in case the process was killed and restarted.
     */
    fun restore(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        childId   = prefs.getInt("childId",  0)
        age       = prefs.getInt("age",      6)
        ageGroup  = prefs.getInt("ageGroup", 6)
        Log.d(TAG, "Restored: childId=$childId age=$age ageGroup=$ageGroup")
    }

    val isInitialized: Boolean get() = childId > 0

    fun log() = Log.d(TAG, "State: childId=$childId age=$age ageGroup=$ageGroup initialized=$isInitialized")
}