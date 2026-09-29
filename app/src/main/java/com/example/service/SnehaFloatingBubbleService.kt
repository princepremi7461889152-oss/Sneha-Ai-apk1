package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.engine.MicChimeManager
import kotlin.math.abs

class SnehaFloatingBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var params: WindowManager.LayoutParams? = null

    companion object {
        const val CHANNEL_ID = "sneha_bubble_channel"
        const val NOTIFICATION_ID = 4040
        var isRunning = false

        fun start(context: Context) {
            if (Settings.canDrawOverlays(context)) {
                val intent = Intent(context, SnehaFloatingBubbleService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SnehaFloatingBubbleService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        if (Settings.canDrawOverlays(this)) {
            showFloatingBubble()
        } else {
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                CHANNEL_ID,
                "Sneha Floating Bubble",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "स्नेहा फ्लोटिंग असिस्टेंट बबल"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(chan)
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("स्नेहा फ्लोटिंग बबल सक्रिय 🫧")
            .setContentText("स्क्रीन पर कहीं से भी एक टैप में स्नेहा से बात करें")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingBubble() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val density = resources.displayMetrics.density
        val sizePx = (64 * density).toInt()

        params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (resources.displayMetrics.widthPixels - sizePx - (16 * density).toInt())
            y = (resources.displayMetrics.heightPixels / 3)
        }

        // Programmatically build an attractive glowing gradient circle with text/icon
        val bubble = android.widget.FrameLayout(this).apply {
            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xEE0D0E1A.toInt())
                setStroke((2.5f * density).toInt(), 0xFF00E5FF.toInt())
            }
            background = bgDrawable

            val innerText = TextView(this@SnehaFloatingBubbleService).apply {
                text = "🌸\n🎙️"
                gravity = Gravity.CENTER
                textSize = 14f
                setTextColor(Color.WHITE)
            }
            addView(
                innerText,
                android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                )
            )
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        bubble.setOnTouchListener { _, event ->
            val p = params ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isDragging = true
                        p.x = initialX + dx
                        p.y = initialY + dy
                        windowManager?.updateViewLayout(bubble, p)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // User clicked the bubble: play chime and open Sneha
                        MicChimeManager.playChime(applicationContext)
                        val mainIntent = Intent(applicationContext, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra("EXTRA_START_LISTENING", true)
                        }
                        startActivity(mainIntent)
                    }
                    true
                }
                else -> false
            }
        }

        bubbleView = bubble
        try {
            windowManager?.addView(bubble, params)
        } catch (ignored: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        bubbleView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (ignored: Exception) {}
        }
        bubbleView = null
    }
}
