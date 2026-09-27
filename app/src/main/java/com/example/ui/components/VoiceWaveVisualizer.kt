package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.VoiceState
import com.example.ui.theme.SnehaCyan
import com.example.ui.theme.SnehaPink
import com.example.ui.theme.SnehaPurple
import kotlin.math.sin

@Composable
fun VoiceWaveVisualizer(
    voiceState: VoiceState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")

    // Rotation for thinking/idle
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulse for orb
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primaryColor = when (voiceState) {
        VoiceState.LISTENING -> SnehaCyan
        VoiceState.SPEAKING -> SnehaPurple
        VoiceState.THINKING -> SnehaPink
        VoiceState.ERROR -> Color(0xFFEF4444)
        VoiceState.IDLE -> SnehaCyan.copy(alpha = 0.6f)
    }

    val secondaryColor = when (voiceState) {
        VoiceState.LISTENING -> SnehaPurple
        VoiceState.SPEAKING -> SnehaPink
        VoiceState.THINKING -> SnehaCyan
        VoiceState.ERROR -> SnehaPink
        VoiceState.IDLE -> SnehaPurple.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier.height(130.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing Orb in center
        val orbScale = if (voiceState == VoiceState.LISTENING) {
            (1f + amplitude * 0.5f).coerceIn(1f, 1.6f)
        } else if (voiceState == VoiceState.SPEAKING) {
            pulse
        } else {
            0.95f
        }

        Box(
            modifier = Modifier
                .size((64.dp * orbScale).coerceAtMost(100.dp))
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.85f),
                            secondaryColor.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Waveform Bars Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerY = canvasHeight / 2f

            val barCount = 28
            val barSpacing = canvasWidth / (barCount + 1)
            val barWidth = (barSpacing * 0.45f).coerceAtLeast(3f)

            for (i in 0 until barCount) {
                val x = barSpacing * (i + 1)
                val distFromCenter = kotlin.math.abs(i - (barCount / 2f)) / (barCount / 2f)
                val bellCurve = (1f - distFromCenter).coerceIn(0.1f, 1f)

                val barHeight = when (voiceState) {
                    VoiceState.LISTENING -> {
                        val dynamicAmp = amplitude * 70f * bellCurve
                        val waveSin = sin((i * 0.4f) + (rotation * 0.1f)) * 8f
                        (12f + dynamicAmp + waveSin).coerceIn(8f, canvasHeight * 0.85f)
                    }
                    VoiceState.SPEAKING -> {
                        val wave = sin((i * 0.5f) + (rotation * 0.15f)) * 30f * bellCurve
                        (20f + kotlin.math.abs(wave)).coerceIn(10f, canvasHeight * 0.8f)
                    }
                    VoiceState.THINKING -> {
                        val wave = sin((i * 0.6f) + (rotation * 0.2f)) * 14f
                        (14f + kotlin.math.abs(wave)).coerceIn(6f, 40f)
                    }
                    else -> {
                        (8f + sin(i * 0.4f) * 4f).coerceIn(6f, 18f)
                    }
                }

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor, secondaryColor),
                        startY = centerY - (barHeight / 2f),
                        endY = centerY + (barHeight / 2f)
                    ),
                    topLeft = Offset(x - (barWidth / 2f), centerY - (barHeight / 2f)),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
