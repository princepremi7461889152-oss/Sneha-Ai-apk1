package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SnehaAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "SnehaAccessibility"
        var instance: SnehaAccessibilityService? = null
            private set

        private var autoScrollJob: Job? = null

        fun isRunning(): Boolean = instance != null

        fun scrollNextReel(context: Context): Pair<Boolean, String> {
            val inst = instance
            if (inst == null) {
                return Pair(false, "मास्टर, रील स्क्रॉल करने के लिए एक्सेसिबिलिटी सर्विस सक्षम करें।")
            }
            inst.scrollDown()
            return Pair(true, "अगली रील स्क्रॉल कर दी गई है।")
        }

        fun scrollPreviousReel(context: Context): Pair<Boolean, String> {
            val inst = instance
            if (inst == null) {
                return Pair(false, "मास्टर, एक्सेसिबिलिटी सर्विस सक्षम करें।")
            }
            inst.scrollUp()
            return Pair(true, "पिछली रील बैक कर दी गई है।")
        }

        fun toggleAutoScroll(context: Context, enable: Boolean): Pair<Boolean, String> {
            val inst = instance
            if (inst == null) {
                return Pair(false, "मास्टर, ऑटो स्क्रॉल के लिए एक्सेसिबिलिटी सर्विस सक्षम करें।")
            }
            autoScrollJob?.cancel()
            return if (enable) {
                autoScrollJob = CoroutineScope(Dispatchers.Main).launch {
                    while (isActive) {
                        delay(12000L) // Wait 12 seconds per reel
                        inst.scrollDown()
                    }
                }
                Pair(true, "ऑटो रील स्क्रॉल चालू कर दिया गया है। हर 12 सेकंड में अगली रील चलेगी।")
            } else {
                autoScrollJob = null
                Pair(true, "ऑटो रील स्क्रॉल बंद कर दिया गया है।")
            }
        }

        fun isAccessibilitySettingsEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${SnehaAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Sneha Autonomous Accessibility Service Connected!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Continuous state monitoring
    }

    override fun onInterrupt() {
        Log.w(TAG, "Sneha Autonomous Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    // 1. Global Navigation Actions
    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun openRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun openNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun openQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

    fun captureScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                takeScreenshot(
                    android.view.Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            Log.i(TAG, "Screenshot taken successfully")
                        }
                        override fun onFailure(errorCode: Int) {
                            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                        }
                    }
                )
                true
            } catch (e: Exception) {
                performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            false
        }
    }

    // 2. Gesture Dispatching (Tap & Swipes)
    fun tapAt(x: Float, y: Float, onComplete: ((Boolean) -> Unit)? = null) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 80)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onComplete?.invoke(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                onComplete?.invoke(false)
            }
        }, null)
    }

    fun scrollDown() {
        val dm = resources.displayMetrics
        val centerX = dm.widthPixels / 2f
        val startY = dm.heightPixels * 0.75f
        val endY = dm.heightPixels * 0.25f
        swipe(centerX, startY, centerX, endY, 300)
    }

    fun scrollUp() {
        val dm = resources.displayMetrics
        val centerX = dm.widthPixels / 2f
        val startY = dm.heightPixels * 0.25f
        val endY = dm.heightPixels * 0.75f
        swipe(centerX, startY, centerX, endY, 300)
    }

    fun swipeLeft() {
        val dm = resources.displayMetrics
        val centerY = dm.heightPixels / 2f
        val startX = dm.widthPixels * 0.85f
        val endX = dm.widthPixels * 0.15f
        swipe(startX, centerY, endX, centerY, 300)
    }

    fun swipeRight() {
        val dm = resources.displayMetrics
        val centerY = dm.heightPixels / 2f
        val startX = dm.widthPixels * 0.15f
        val endX = dm.widthPixels * 0.85f
        swipe(startX, centerY, endX, centerY, 300)
    }

    private fun swipe(fromX: Float, fromY: Float, toX: Float, toY: Float, durationMs: Long) {
        val path = Path().apply {
            moveTo(fromX, fromY)
            lineTo(toX, toY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    // 3. UI Node Content Extraction & Screen Reading
    fun readScreenContent(): String {
        val root = rootInActiveWindow ?: return "मास्टर, अभी स्क्रीन की जानकारी उपलब्ध नहीं हो पा रही है।"
        val texts = mutableListOf<String>()
        collectTextsFromNode(root, texts)
        root.recycle()

        if (texts.isEmpty()) {
            return "मास्टर, इस समय स्क्रीन पर कोई पढ़ने योग्य टेक्स्ट दिखाई नहीं दे रहा है।"
        }
        // Deduplicate adjacent and clean
        val clean = texts.distinct().take(15)
        return clean.joinToString(separator = "। ")
    }

    private fun collectTextsFromNode(node: AccessibilityNodeInfo?, outTexts: MutableList<String>) {
        if (node == null) return
        if (node.isVisibleToUser) {
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            if (!text.isNullOrBlank() && text.length > 1) {
                outTexts.add(text)
            } else if (!desc.isNullOrBlank() && desc.length > 1) {
                outTexts.add(desc)
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            collectTextsFromNode(child, outTexts)
            child?.recycle()
        }
    }

    // 4. Click by Text or Label
    fun clickByText(target: String, preferBottom: Boolean = false, preferTop: Boolean = false): Boolean {
        val root = rootInActiveWindow ?: return false
        val matchingNodes = mutableListOf<AccessibilityNodeInfo>()
        findNodesMatchingText(root, target.lowercase(), matchingNodes)

        if (matchingNodes.isEmpty()) {
            root.recycle()
            return false
        }

        val chosenNode = if (preferBottom) {
            matchingNodes.maxByOrNull {
                val rect = Rect()
                it.getBoundsInScreen(rect)
                rect.bottom
            } ?: matchingNodes.last()
        } else if (preferTop) {
            matchingNodes.minByOrNull {
                val rect = Rect()
                it.getBoundsInScreen(rect)
                rect.top
            } ?: matchingNodes.first()
        } else {
            matchingNodes.first()
        }

        val clicked = performClickOnNodeOrParent(chosenNode)
        root.recycle()
        return clicked
    }

    // 5. Visual Context Buttons ("नीचे वाला button", "ऊपर वाला", "पहला", "आखिरी")
    fun clickButtonByPosition(position: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val clickableButtons = mutableListOf<AccessibilityNodeInfo>()
        findClickableButtons(root, clickableButtons)

        if (clickableButtons.isEmpty()) {
            root.recycle()
            return false
        }

        val chosen = when {
            position.contains("नीचे") || position.contains("bottom") || position.contains("लास्ट") || position.contains("आखिरी") -> {
                clickableButtons.maxByOrNull {
                    val rect = Rect()
                    it.getBoundsInScreen(rect)
                    rect.bottom
                } ?: clickableButtons.last()
            }
            position.contains("ऊपर") || position.contains("top") || position.contains("पहला") || position.contains("first") -> {
                clickableButtons.minByOrNull {
                    val rect = Rect()
                    it.getBoundsInScreen(rect)
                    rect.top
                } ?: clickableButtons.first()
            }
            position.contains("बीच") || position.contains("center") || position.contains("middle") -> {
                val dm = resources.displayMetrics
                val midY = dm.heightPixels / 2
                clickableButtons.minByOrNull {
                    val rect = Rect()
                    it.getBoundsInScreen(rect)
                    Math.abs(rect.centerY() - midY)
                } ?: clickableButtons[clickableButtons.size / 2]
            }
            else -> clickableButtons.first()
        }

        val result = performClickOnNodeOrParent(chosen)
        root.recycle()
        return result
    }

    private fun findClickableButtons(node: AccessibilityNodeInfo?, outList: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (node.isVisibleToUser && (node.isClickable || node.className?.contains("Button", ignoreCase = true) == true)) {
            outList.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            findClickableButtons(child, outList)
        }
    }

    private fun findNodesMatchingText(node: AccessibilityNodeInfo?, target: String, outList: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (node.isVisibleToUser) {
            val text = node.text?.toString()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            if (text.contains(target) || desc.contains(target)) {
                outList.add(node)
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            findNodesMatchingText(child, target, outList)
        }
    }

    private fun performClickOnNodeOrParent(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        // If not clickable, click center coordinates
        if (node != null) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            tapAt(rect.centerX().toFloat(), rect.centerY().toFloat())
            return true
        }
        return false
    }

    // 6. Text Typing & Deleting in active input fields
    fun typeText(textToType: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = findFocusedOrFirstEditable(root)
        if (focused != null) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
            }
            val res = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            root.recycle()
            return res
        }
        root.recycle()
        return false
    }

    fun clearText(): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = findFocusedOrFirstEditable(root)
        if (focused != null) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
            }
            val res = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            root.recycle()
            return res
        }
        root.recycle()
        return false
    }

    private fun findFocusedOrFirstEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isFocused && (node.isEditable || node.className?.contains("EditText", ignoreCase = true) == true)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val res = findFocusedOrFirstEditable(node.getChild(i))
            if (res != null) return res
        }
        if (node.isEditable || node.className?.contains("EditText", ignoreCase = true) == true) {
            return node
        }
        return null
    }
}
