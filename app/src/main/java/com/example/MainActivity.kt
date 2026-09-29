package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PhonePaused
import androidx.compose.material.icons.filled.Sms
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.engine.PhoneControlManager
import com.example.engine.VoiceSpeechManager
import com.example.service.SnehaVoiceService
import com.example.ui.MainScreen
import com.example.ui.components.AppPermissionItem
import com.example.ui.components.PermissionsDialog
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var voiceSpeechManager: VoiceSpeechManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Enable display over lockscreen and screen turn-on for lockscreen assistant operation
        setupLockscreenFlags()

        voiceSpeechManager = VoiceSpeechManager(this)

        // Safe background voice service start (only if permission already granted)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            try {
                SnehaVoiceService.startService(this)
            } catch (ignored: Exception) {}
        }

        // Handle quick action intents
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                // All runtime permissions requested by Sneha AI Voice & Call Assistant
                val allPermissionsList = remember {
                    val list = mutableListOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.ANSWER_PHONE_CALLS,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.CAMERA
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        list.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    list.toTypedArray()
                }

                var permissionCheckVersion by remember { mutableIntStateOf(0) }
                var showPermissionsDialog by remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    permissionCheckVersion++
                    val micGranted = results[Manifest.permission.RECORD_AUDIO] == true ||
                            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (micGranted) {
                        try {
                            SnehaVoiceService.startService(this@MainActivity)
                            voiceSpeechManager.startListening()
                        } catch (ignored: Exception) {}
                    }

                    // Check if any critical permissions are still missing
                    val stillMissing = allPermissionsList.any {
                        ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (stillMissing) {
                        showPermissionsDialog = true
                    } else {
                        showPermissionsDialog = false
                    }
                }

                // Initial open: check and launch permissions prompt immediately
                LaunchedEffect(Unit) {
                    val notGranted = allPermissionsList.filter {
                        ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (notGranted.isNotEmpty()) {
                        // Immediately request all permissions as soon as app opens
                        showPermissionsDialog = true
                        permissionLauncher.launch(notGranted.toTypedArray())
                    } else {
                        // All granted, start background service and mic
                        try {
                            SnehaVoiceService.startService(this@MainActivity)
                            voiceSpeechManager.startListening()
                        } catch (ignored: Exception) {}
                    }
                }

                // Compute current permission items for UI breakdown
                val permissionItems = remember(permissionCheckVersion) {
                    val items = mutableListOf<AppPermissionItem>()

                    items.add(
                        AppPermissionItem(
                            title = "माइक्रोफोन (Microphone)",
                            description = "स्नेहा को आपकी आवाज़ लगातार सुनने और तुरंत कार्य करने के लिए",
                            icon = Icons.Default.Mic,
                            isGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        )
                    )

                    items.add(
                        AppPermissionItem(
                            title = "संपर्क सूची (Contacts)",
                            description = "व्हाट्सएप मैसेज व वॉयस कॉल के लिए कॉन्टैक्ट्स खोजने के लिए",
                            icon = Icons.Default.Contacts,
                            isGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                        )
                    )

                    items.add(
                        AppPermissionItem(
                            title = "डायरेक्ट फोन कॉल (Make Calls)",
                            description = "वॉयस कमांड से किसी को भी तुरंत फोन कॉल लगाने के लिए",
                            icon = Icons.Default.Call,
                            isGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                        )
                    )

                    items.add(
                        AppPermissionItem(
                            title = "ऑटो कॉल पिकअप (Answer Calls)",
                            description = "इनकमिंग कॉल उठाने और 'मास्टर अभी क्लास में हैं' बोलने के लिए",
                            icon = Icons.Default.PhoneInTalk,
                            isGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED
                        )
                    )

                    items.add(
                        AppPermissionItem(
                            title = "कॉलर पहचान (Phone State)",
                            description = "इनकमिंग कॉल आने पर कॉलर का नंबर व स्थिति जानने के लिए",
                            icon = Icons.Default.PhonePaused,
                            isGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                        )
                    )

                    items.add(
                        AppPermissionItem(
                            title = "एसएमएस व संदेश सुरक्षा (SMS Reader)",
                            description = "संदेश बोलकर सुनाने और ओटीपी को सुरक्षित रूप से छिपाने के लिए",
                            icon = Icons.Default.Sms,
                            isGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                                    ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                        )
                    )

                    items.add(
                        AppPermissionItem(
                            title = "एसओएस व ऑटो-रिप्लाई एसएमएस",
                            description = "किसी को भी एसएमएस भेजने व इमरजेंसी अलर्ट के लिए",
                            icon = Icons.AutoMirrored.Filled.Send,
                            isGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                        )
                    )

                    items.add(
                        AppPermissionItem(
                            title = "कैमरा व स्क्रीन शेयर (Camera & Screen)",
                            description = "वॉयस कमांड से टॉर्च चालू करने और लाइव स्क्रीन शेयर के लिए",
                            icon = Icons.Default.CameraAlt,
                            isGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        )
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        items.add(
                            AppPermissionItem(
                                title = "नोटिफिकेशन्स (Alerts)",
                                description = "बैकग्राउंड अलर्ट और वॉयस स्टेटस नोटिफिकेशन के लिए",
                                icon = Icons.Default.Notifications,
                                isGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                            )
                        )
                    }

                    items
                }

                val allGranted = permissionItems.all { it.isGranted }

                Box(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        voiceManager = voiceSpeechManager,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (showPermissionsDialog && !allGranted) {
                        PermissionsDialog(
                            permissions = permissionItems,
                            allGranted = allGranted,
                            onRequestAllPermissions = {
                                val notGranted = allPermissionsList.filter {
                                    ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                                }
                                if (notGranted.isNotEmpty()) {
                                    permissionLauncher.launch(notGranted.toTypedArray())
                                }
                            },
                            onDismiss = {
                                showPermissionsDialog = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val shouldStartMic = intent.getBooleanExtra(SnehaVoiceService.EXTRA_START_MIC, false)
        val shouldTriggerSos = intent.getBooleanExtra(SnehaVoiceService.EXTRA_TRIGGER_SOS, false)

        if (shouldTriggerSos) {
            PhoneControlManager.startEmergencyAlarm(this)
            PhoneControlManager.requestEmergencyUnlock(this) {}
        }

        if (shouldStartMic) {
            window.decorView.postDelayed({
                voiceSpeechManager.speak("जी मास्टर! मैं हाजिर हूँ, आज्ञा दीजिए!")
                window.decorView.postDelayed({
                    voiceSpeechManager.startListening()
                }, 2200)
            }, 400)
        }
    }

    private fun setupLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onResume() {
        super.onResume()
        SnehaVoiceService.pauseListeningForForeground(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            voiceSpeechManager.setContinuousListening(true)
            voiceSpeechManager.startListening()
        }
    }

    override fun onPause() {
        super.onPause()
        voiceSpeechManager.stopListening()
        SnehaVoiceService.resumeListeningFromForeground(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceSpeechManager.destroy()
        PhoneControlManager.stopEmergencyAlarm()
        PhoneControlManager.stopEmergencyStrobe(this)
    }
}
