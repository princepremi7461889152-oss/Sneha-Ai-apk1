package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.data.local.VoicePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class MicChimeType(val id: String, val displayName: String, val emoji: String) {
    FUTURISTIC("futuristic", "फ्यूचरिस्टिक चाइम (AI)", "🎵"),
    CRYSTAL_BELL("crystal", "क्रिस्टल बेल", "🔔"),
    WATER_DROP("water_drop", "वॉटर ड्रॉप", "💧"),
    GENTLE_PULSE("gentle_pulse", "सॉफ्ट पल्स", "✨"),
    MUTE("mute", "म्यूट (कोई आवाज नहीं)", "🔕");

    companion object {
        fun fromId(id: String): MicChimeType {
            return entries.find { it.id == id } ?: FUTURISTIC
        }
    }
}

/**
 * High-quality pleasant sound chime synthesizer for Mic activation.
 * Replaces harsh DTMF ToneGenerator beeps with smooth, modern AI assistant tones.
 */
object MicChimeManager {

    private const val TAG = "MicChimeManager"
    private const val SAMPLE_RATE = 44100
    private val scope = CoroutineScope(Dispatchers.Default)

    // Pre-cached PCM buffers for zero-latency instant playback
    private val cache = mutableMapOf<MicChimeType, ShortArray>()

    init {
        for (type in MicChimeType.entries) {
            if (type != MicChimeType.MUTE) {
                cache[type] = generatePcm(type)
            }
        }
    }

    private fun generatePcm(type: MicChimeType): ShortArray {
        return when (type) {
            MicChimeType.FUTURISTIC -> {
                // Ascending melodic two-tone harmonic chime (880 Hz -> 1318.5 Hz)
                // Note 1: 880 Hz (A5) for 75ms
                // Gap: 15ms
                // Note 2: 1318.5 Hz (E6) for 130ms with gentle decay
                val totalSamples = (SAMPLE_RATE * 0.22).toInt() // 220ms
                val buffer = ShortArray(totalSamples)

                val n1Samples = (SAMPLE_RATE * 0.075).toInt()
                val gapSamples = (SAMPLE_RATE * 0.015).toInt()
                val n2Samples = totalSamples - n1Samples - gapSamples

                val maxAmp = 12000.0 // pleasant, polite volume

                // Note 1: 880 Hz
                var phase = 0.0
                val freq1 = 880.0
                for (i in 0 until n1Samples) {
                    val t = i.toDouble() / n1Samples
                    val env = if (t < 0.15) t / 0.15 else 1.0 - (t - 0.15) * 0.4
                    phase += 2.0 * PI * freq1 / SAMPLE_RATE
                    val sample = (sin(phase) + 0.25 * sin(phase * 2.0)) * maxAmp * env
                    buffer[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
                }

                // Note 2: 1318.5 Hz (E6)
                phase = 0.0
                val freq2 = 1318.5
                val startIdx = n1Samples + gapSamples
                for (i in 0 until n2Samples) {
                    val t = i.toDouble() / n2Samples
                    val env = exp(-t * 4.5)
                    phase += 2.0 * PI * freq2 / SAMPLE_RATE
                    val sample = (sin(phase) + 0.2 * sin(phase * 2.0)) * (maxAmp * 1.1) * env
                    buffer[startIdx + i] = sample.toInt().coerceIn(-32767, 32767).toShort()
                }
                buffer
            }

            MicChimeType.CRYSTAL_BELL -> {
                // High clear bell ding: 1568 Hz (G6) with delicate shimmer
                val totalSamples = (SAMPLE_RATE * 0.18).toInt() // 180ms
                val buffer = ShortArray(totalSamples)
                val maxAmp = 10000.0
                var phase1 = 0.0
                var phase2 = 0.0
                val freq1 = 1567.98
                val freq2 = 3135.96

                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / totalSamples
                    val env = exp(-t * 6.0)
                    phase1 += 2.0 * PI * freq1 / SAMPLE_RATE
                    phase2 += 2.0 * PI * freq2 / SAMPLE_RATE
                    val sample = (sin(phase1) + 0.18 * sin(phase2)) * maxAmp * env
                    buffer[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
                }
                buffer
            }

            MicChimeType.WATER_DROP -> {
                // Pleasant water drop pop: pitch drops softly
                val totalSamples = (SAMPLE_RATE * 0.11).toInt() // 110ms
                val buffer = ShortArray(totalSamples)
                val maxAmp = 11000.0
                var phase = 0.0

                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / totalSamples
                    val freq = 650.0 + (350.0 * (1.0 - t))
                    val env = if (t < 0.1) t / 0.1 else exp(-(t - 0.1) * 7.0)
                    phase += 2.0 * PI * freq / SAMPLE_RATE
                    val sample = sin(phase) * maxAmp * env
                    buffer[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
                }
                buffer
            }

            MicChimeType.GENTLE_PULSE -> {
                // Warm, subtle C5 pulse (523.25 Hz)
                val totalSamples = (SAMPLE_RATE * 0.10).toInt() // 100ms
                val buffer = ShortArray(totalSamples)
                val maxAmp = 9500.0
                var phase = 0.0
                val freq = 523.25

                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / totalSamples
                    val env = sin(t * PI)
                    phase += 2.0 * PI * freq / SAMPLE_RATE
                    val sample = sin(phase) * maxAmp * env
                    buffer[i] = sample.toInt().coerceIn(-32767, 32767).toShort()
                }
                buffer
            }

            MicChimeType.MUTE -> ShortArray(0)
        }
    }

    /**
     * Plays the mic open chime according to user's saved preference.
     * Non-blocking, ultra low latency.
     */
    fun playChime(context: Context) {
        val currentType = VoicePreferences.getMicChimeType(context)
        playSpecificChime(currentType)
    }

    fun playSpecificChime(type: MicChimeType) {
        if (type == MicChimeType.MUTE) return

        val pcm = cache.getOrPut(type) { generatePcm(type) }
        if (pcm.isEmpty()) return

        scope.launch {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()

                val durationMs = (pcm.size * 1000L / SAMPLE_RATE) + 60L
                delay(durationMs)
                try {
                    track.stop()
                    track.release()
                } catch (ignored: Exception) {}
            } catch (e: Exception) {
                Log.e(TAG, "Error playing mic chime", e)
            }
        }
    }
}
