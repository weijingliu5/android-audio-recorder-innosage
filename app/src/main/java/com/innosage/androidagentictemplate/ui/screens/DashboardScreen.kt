package com.innosage.androidagentictemplate.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innosage.androidagentictemplate.data.UtteranceEntity
import com.innosage.androidagentictemplate.ui.UtteranceFeed
import com.innosage.androidagentictemplate.ui.theme.ElectricGreen

@Composable
fun DashboardScreen(
    isRecording: Boolean,
    isVoiced: Boolean,
    utterances: List<UtteranceEntity>,
    onToggleRecording: () -> Unit,
    onManualTag: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Section: Title
        Text(
            text = "Context Sensor Active",
            style = MaterialTheme.typography.headlineMedium,
            color = ElectricGreen,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Middle Section: Utterance Feed
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (utterances.isEmpty()) {
                    EnergyRing(isVoiced = isVoiced && isRecording)
                    Text(
                        text = if (isRecording) "SENSING..." else "IDLE",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                } else {
                    UtteranceFeed(utterances = utterances)
                    // Mini Energy Ring overlay when list is visible
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(40.dp)) {
                        EnergyRing(isVoiced = isVoiced && isRecording)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Section: Action Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Manual Tag Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onManualTag()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = "Manual Tag",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Pause/Resume Button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleRecording()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color.DarkGray else ElectricGreen,
                        contentColor = if (isRecording) ElectricGreen else Color.Black
                    ),
                    shape = CircleShape,
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRecording) "Pause" else "Resume"
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = if (isRecording) "PAUSE" else "RESUME")
                }
            }
        }
    }
}

@Composable
fun EnergyRing(isVoiced: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "EnergyRingTransition")
    
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreatheScale"
    )

    val voiceScale by animateFloatAsState(
        targetValue = if (isVoiced) 1.5f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "VoiceScale"
    )

    val ringColor by animateColorAsState(
        targetValue = if (isVoiced) ElectricGreen else ElectricGreen.copy(alpha = 0.4f),
        label = "Color"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = (size.minDimension / 2) * 0.7f * breatheScale * voiceScale
        drawCircle(
            color = ringColor,
            style = Stroke(width = if (isVoiced) 8.dp.toPx() else 4.dp.toPx()),
            radius = radius
        )
        if (isVoiced) {
            drawCircle(
                color = ringColor.copy(alpha = 0.3f),
                radius = radius * 1.2f
            )
        }
    }
}

