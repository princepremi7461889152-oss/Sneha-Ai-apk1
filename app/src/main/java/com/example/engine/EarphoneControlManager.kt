package com.example.engine

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object EarphoneControlManager {

    private const val TAG = "EarphoneControlManager"

    private val _isEarphoneConnected = MutableStateFlow(false)
    val isEarphoneConnected: StateFlow<Boolean> = _isEarphoneConnected.asStateFlow()

    private val _earphoneStatusText = MutableStateFlow("फोन माइक व स्पीकर")
    val earphoneStatusText: StateFlow<String> = _earphoneStatusText.asStateFlow()

    var onEarphoneButtonPressed: (() -> Unit)? = null
    var onEarphoneStateChanged: ((Boolean, String) -> Unit)? = null

    private var isReceiverRegistered = false
    private var isScoActive = false

    private val earphoneReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null || intent == null) return
            val action = intent.action ?: return

            when (action) {
                // Wired Headset plugged in or out
                Intent.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    val hasMic = intent.getIntExtra("microphone", 0) == 1
                    val name = intent.getStringExtra("name") ?: "वायर्ड इयरफ़ोन"

                    if (state == 1) {
                        _isEarphoneConnected.value = true
                        val desc = if (hasMic) "वायर्ड इयरफ़ोन (माइक सक्रिय) 🎧" else "वायर्ड हेडफोन 🎧"
                        _earphoneStatusText.value = desc
                        setupAudioRouting(context, true)
                        onEarphoneStateChanged?.invoke(true, desc)
                    } else if (state == 0) {
                        checkAllConnectedAudioDevices(context)
                    }
                }

                // Bluetooth Headset / Earbuds Audio SCO State
                AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> {
                    val scoState = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
                    if (scoState == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                        isScoActive = true
                        _isEarphoneConnected.value = true
                        _earphoneStatusText.value = "ब्लूटूथ ईयरबड्स/माइक कनेक्टेड 🎙️🎧"
                    } else if (scoState == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                        isScoActive = false
                    }
                }

                // Bluetooth Headset Connection Changed
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        _isEarphoneConnected.value = true
                        _earphoneStatusText.value = "ब्लूटूथ हेडसेट कनेक्टेड 🎧"
                        setupAudioRouting(context, true)
                        onEarphoneStateChanged?.invoke(true, "ब्लूटूथ हेडसेट कनेक्टेड")
                    } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                        setupAudioRouting(context, false)
                        checkAllConnectedAudioDevices(context)
                    }
                }

                // Media button on Earphone (Play/Pause / Headset Hook)
                Intent.ACTION_MEDIA_BUTTON -> {
                    val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    }

                    if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_UP) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_HEADSETHOOK,
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                Log.d(TAG, "Earphone media button pressed! Triggering voice mic.")
                                onEarphoneButtonPressed?.invoke()
                                abortBroadcast()
                            }
                        }
                    }
                }
            }
        }
    }

    fun init(context: Context) {
        if (isReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(Intent.ACTION_MEDIA_BUTTON)
            priority = 1000 // Highest priority to intercept earphone clicks
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(earphoneReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(earphoneReceiver, filter)
            }
            isReceiverRegistered = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register earphone receiver", e)
        }

        checkAllConnectedAudioDevices(context)
    }

    fun checkAllConnectedAudioDevices(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        var foundHeadset = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS or AudioManager.GET_DEVICES_OUTPUTS)
            for (dev in devices) {
                when (dev.type) {
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> {
                        foundHeadset = true
                        _isEarphoneConnected.value = true
                        _earphoneStatusText.value = "वायर्ड इयरफ़ोन सक्रिय 🎧"
                        setupAudioRouting(context, true)
                        return
                    }
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                        foundHeadset = true
                        _isEarphoneConnected.value = true
                        _earphoneStatusText.value = "ब्लूटूथ ईयरफोन/नेकबैंड 🎧"
                        setupAudioRouting(context, true)
                        return
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            if (audioManager.isWiredHeadsetOn || audioManager.isBluetoothScoOn || audioManager.isBluetoothA2dpOn) {
                foundHeadset = true
                _isEarphoneConnected.value = true
                _earphoneStatusText.value = "इयरफ़ोन कनेक्टेड 🎧"
                setupAudioRouting(context, true)
                return
            }
        }

        if (!foundHeadset) {
            _isEarphoneConnected.value = false
            _earphoneStatusText.value = "फोन स्पीकर व माइक"
            setupAudioRouting(context, false)
        }
    }

    /**
     * Routes microphone and audio through connected wired or bluetooth earphone
     */
    fun setupAudioRouting(context: Context, routeToEarphone: Boolean) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            if (routeToEarphone) {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = false

                // Try starting Bluetooth SCO if bluetooth headset is paired
                try {
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                } catch (ignored: Exception) {}
            } else {
                try {
                    audioManager.isBluetoothScoOn = false
                    audioManager.stopBluetoothSco()
                } catch (ignored: Exception) {}
                audioManager.mode = AudioManager.MODE_NORMAL
                audioManager.isSpeakerphoneOn = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio routing exception: ${e.message}")
        }
    }
}
