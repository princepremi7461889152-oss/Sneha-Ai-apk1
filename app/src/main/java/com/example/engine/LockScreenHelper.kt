package com.example.engine

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.example.MainActivity

object LockScreenHelper {

    private const val TAG = "LockScreenHelper"

    /**
     * Wakes up the screen using PowerManager WakeLock even if phone is in deep sleep.
     */
    fun wakeUpScreen(context: Context, timeoutMs: Long = 5000L) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null) {
                @Suppress("DEPRECATION")
                val wakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "Sneha:WakeScreenLock"
                )
                wakeLock.acquire(timeoutMs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock", e)
        }
    }

    /**
     * Launches the Sneha assistant activity over the lock screen.
     */
    fun launchOverLockScreen(context: Context, startVoiceMic: Boolean = true, triggerSos: Boolean = false) {
        wakeUpScreen(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("extra_start_mic", startVoiceMic)
            putExtra("extra_trigger_sos", triggerSos)
        }
        context.startActivity(intent)
    }

    /**
     * Requests dismissal of keyguard (emergency unlock).
     * If device has no secure lock (swipe or none), it unlocks completely.
     * If device has PIN/Pattern/Biometrics, it shows system unlock prompt directly.
     */
    fun requestEmergencyKeyguardDismiss(activity: Activity, onComplete: (Boolean) -> Unit) {
        val km = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (km == null) {
            onComplete(false)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            km.requestDismissKeyguard(activity, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    super.onDismissSucceeded()
                    Log.d(TAG, "Keyguard dismiss succeeded")
                    onComplete(true)
                }

                override fun onDismissCancelled() {
                    super.onDismissCancelled()
                    Log.d(TAG, "Keyguard dismiss cancelled by user")
                    onComplete(false)
                }

                override fun onDismissError() {
                    super.onDismissError()
                    Log.e(TAG, "Keyguard dismiss error")
                    onComplete(false)
                }
            })
        } else {
            @Suppress("DEPRECATION")
            km.newKeyguardLock("SnehaKeyguardLock")?.disableKeyguard()
            onComplete(true)
        }
    }

    fun isDeviceLocked(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isKeyguardLocked ?: false
    }
}
