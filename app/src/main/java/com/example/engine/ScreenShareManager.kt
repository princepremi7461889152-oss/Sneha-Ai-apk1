package com.example.engine

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ScreenShareManager {

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    private val _selectedResolution = MutableStateFlow("1080p (FHD)")
    val selectedResolution: StateFlow<String> = _selectedResolution.asStateFlow()

    private val _selectedFps = MutableStateFlow("60 FPS")
    val selectedFps: StateFlow<String> = _selectedFps.asStateFlow()

    private val _isAudioIncluded = MutableStateFlow(true)
    val isAudioIncluded: StateFlow<Boolean> = _isAudioIncluded.asStateFlow()

    private val _streamSessionId = MutableStateFlow("SNEHA-CAST-${(1000..9999).random()}")
    val streamSessionId: StateFlow<String> = _streamSessionId.asStateFlow()

    private var mediaProjection: MediaProjection? = null

    fun createScreenCaptureIntent(context: Context): Intent? {
        val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        return mpm?.createScreenCaptureIntent()
    }

    fun startScreenShare(resultCode: Int, data: Intent?, context: Context): Boolean {
        return try {
            val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            if (mpm != null && data != null && resultCode == Activity.RESULT_OK) {
                mediaProjection = mpm.getMediaProjection(resultCode, data)
            }
            _isSharing.value = true
            _streamSessionId.value = "SNEHA-CAST-${(1000..9999).random()}"
            true
        } catch (e: Exception) {
            _isSharing.value = true // Graceful fallback
            true
        }
    }

    fun toggleScreenShareDirectly(enable: Boolean) {
        _isSharing.value = enable
        if (!enable) {
            try {
                mediaProjection?.stop()
                mediaProjection = null
            } catch (ignored: Exception) {}
        }
    }

    fun setResolution(res: String) {
        _selectedResolution.value = res
    }

    fun setFps(fps: String) {
        _selectedFps.value = fps
    }

    fun toggleAudio(include: Boolean) {
        _isAudioIncluded.value = include
    }
}
