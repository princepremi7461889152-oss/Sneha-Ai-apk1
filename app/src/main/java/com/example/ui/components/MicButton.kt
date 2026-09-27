package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.VoiceState
import com.example.ui.theme.SnehaCyan
import com.example.ui.theme.SnehaPink
import com.example.ui.theme.SnehaPurple

@Composable
fun MicButton(
    voiceState: VoiceState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening = voiceState == VoiceState.LISTENING
    val isSpeaking = voiceState == VoiceState.SPEAKING

    val infiniteTransition = rememberInfiniteTransition(label = "mic_ripple")

    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_scale"
    )

    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha"
    )

    Box(
        modifier = modifier.size(88.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring when listening
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(rippleScale)
                    .background(SnehaCyan.copy(alpha = rippleAlpha), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .scale(rippleScale * 0.9f)
                    .background(SnehaPurple.copy(alpha = rippleAlpha * 0.8f), CircleShape)
            )
        }

        // Inner glowing core button
        val buttonBrush = when {
            isListening -> Brush.linearGradient(listOf(SnehaCyan, SnehaPurple))
            isSpeaking -> Brush.linearGradient(listOf(SnehaPurple, SnehaPink))
            else -> Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF6366F1)))
        }

        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(buttonBrush)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, radius = 34.dp),
                    onClick = onClick
                )
                .testTag("main_mic_button"),
            contentAlignment = Alignment.Center
        ) {
            val icon = when {
                isListening -> Icons.Default.Stop
                isSpeaking -> Icons.Default.Stop
                else -> Icons.Default.Mic
            }

            Icon(
                imageVector = icon,
                contentDescription = if (isListening) "माइक बंद करें" else "स्नेहा से बोलें",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}
