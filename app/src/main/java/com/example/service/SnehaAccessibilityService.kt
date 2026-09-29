package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SnehaAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var autoScrollJob: Job? = null

    companion object {
        private const val TAG = "SnehaAccessibility"
        var instance: SnehaAccessibilityService? = null
            private set

        fun isServiceRunning(): Boolean = instance != null

        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Cannot open accessibility settings", e)
            }
        }

        /**
         * Voice action to scroll to the NEXT reel in any app (Instagram, YouTube Shorts, Facebook, etc.)
         */
        fun scrollNextReel(context: Context): Pair<Boolean, String> {
            val service = instance
            if (service == null) {
                openAccessibilitySettings(context)
                return Pair(
                    false,
                    "मास्टर, किसी भी ऐप में रील स्क्रॉल करने के लिए 'स्नेहा एक्सेसिबिलिटी सर्विस' ऑन करनी होगी। सेटिंग्स खोली गई हैं, कृपया 'Sneha Reel Controller' को चालू कर दीजिए!"
                )
            }
            val success = service.performSwipe(isNext = true)
            return if (success) {
                Pair(true, "अगली रील स्क्रॉल कर दी गई है।")
            } else {
                Pair(false, "रील स्क्रॉल करने में समस्या हुई।")
            }
        }

        /**
         * Voice action to scroll to the PREVIOUS reel in any app.
         */
        fun scrollPreviousReel(context: Context): Pair<Boolean, String> {
            val service = instance
            if (service == null) {
                openAccessibilitySettings(context)
                return Pair(
                    false,
                    "मास्टर, किसी भी ऐप में रील स्क्रॉल करने के लिए 'स्नेहा एक्सेसिबिलिटी सर्विस' ऑन करनी होगी। सेटिंग्स खोली गई हैं, कृपया 'Sneha Reel Controller' को चालू कर दीजिए!"
                )
            }
            val success = service.performSwipe(isNext = false)
            return if (success) {
                Pair(true, "पिछली रील पर स्क्रॉल कर दिया गया है।")
            } else {
                Pair(false, "रील स्क्रॉल करने में समस्या हुई।")
            }
        }

        /**
         * Starts automatic hands-free scrolling every X seconds
         */
        fun toggleAutoScroll(context: Context, enable: Boolean, intervalSeconds: Int = 12): Pair<Boolean, String> {
            val service = instance
            if (service == null) {
                openAccessibilitySettings(context)
                return Pair(
                    false,
                    "मास्टर, पहले सेटिंग्स में 'Sneha Reel Controller' एक्सेसिबिलिटी को ऑन करें।"
                )
            }
            return if (enable) {
                service.startAutoScroll(intervalSeconds)
                Pair(true, "ऑटो रील स्क्रॉल चालू कर दिया गया है! हर $intervalSeconds सेकंड में अगली रील अपने आप स्क्रॉल होगी। बंद करने के लिए 'ऑटो स्क्रॉल बंद करो' कहें।")
            } else {
                service.stopAutoScroll()
                Pair(true, "ऑटो रील स्क्रॉल बंद कर दिया गया है।")
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "SnehaAccessibilityService connected and ready for reel scrolling")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Optional tracking of active window package if needed
    }

    override fun onInterrupt() {
        Log.w(TAG, "SnehaAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoScroll()
        instance = null
    }

    /**
     * Executes a smooth vertical swipe gesture on screen.
     * isNext = true: swipes UP from 76% to 20% to move to next reel
     * isNext = false: swipes DOWN from 24% to 80% to move to previous reel
     */
    fun performSwipe(isNext: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }

        val metrics = DisplayMetrics()
        val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        @Suppress("DEPRECATION")
        wm?.defaultDisplay?.getRealMetrics(metrics)

        val width = if (metrics.widthPixels > 0) metrics.widthPixels else 1080
        val height = if (metrics.heightPixels > 0) metrics.heightPixels else 2400

        val midX = width / 2f
        val startY: Float
        val endY: Float

        if (isNext) {
            startY = height * 0.76f
            endY = height * 0.20f
        } else {
            startY = height * 0.24f
            endY = height * 0.80f
        }

        val swipePath = Path().apply {
            moveTo(midX, startY)
            lineTo(midX, endY)
        }

        val gestureDuration = 220L
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(swipePath, 0, gestureDuration))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    fun startAutoScroll(intervalSeconds: Int = 12) {
        stopAutoScroll()
        autoScrollJob = serviceScope.launch {
            while (isActive) {
                delay(intervalSeconds * 1000L)
                if (isActive) {
                    performSwipe(isNext = true)
                }
            }
        }
    }

    fun stopAutoScroll() {
        autoScrollJob?.cancel()
        autoScrollJob = null
    }
}
