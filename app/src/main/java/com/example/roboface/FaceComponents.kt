package com.example.roboface

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun RoboFace(
    emotion: EmotionState, 
    tiltX: Float = 0f, 
    tiltY: Float = 0f, 
    rotationZ: Float = 0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "irritated_shake")
    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.graphicsLayer {
            // Overall head tilt based on gyroscope/tilt
            // Disambiguate scope property from parameter
            this.rotationZ = rotationZ * 10f 
            
            // Jitter when irritated
            if (emotion == EmotionState.Irritated) {
                translationX = shakeOffset
                translationY = shakeOffset
            }
        }
    ) {
        RoboEyes(
            emotion = emotion, 
            tiltX = tiltX, 
            tiltY = tiltY, 
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )
        RoboNose(emotion = emotion)
        RoboMouth(emotion = emotion)
    }
}

@Composable
fun RoboEyes(
    emotion: EmotionState, 
    tiltX: Float = 0f, 
    tiltY: Float = 0f, 
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RoboEye(emotion, tiltX, tiltY)
        RoboEye(emotion, tiltX, tiltY)
    }
}

@Composable
fun RoboEye(emotion: EmotionState, tiltX: Float = 0f, tiltY: Float = 0f) {
    val infiniteTransition = rememberInfiniteTransition(label = "eye_idle")
    
    // Pulse animation for the glow
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (emotion == EmotionState.Angry) 500 else 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Animation based on emotion
    val eyeColor by animateColorAsState(
        targetValue = when (emotion) {
            EmotionState.Angry -> Color.Red
            EmotionState.Irritated -> Color(0xFFFFA500) // Orange
            EmotionState.Sad -> Color.Blue
            EmotionState.Sleep -> Color.DarkGray
            EmotionState.Happy -> Color.Cyan
            else -> Color.Cyan
        },
        animationSpec = tween(500), 
        label = "color"
    )

    val eyeHeightScale by animateFloatAsState(
        targetValue = if (emotion == EmotionState.Sleep) 0.1f else if (emotion == EmotionState.Happy) 0.6f else if (emotion == EmotionState.Irritated) 0.8f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "height"
    )

    val rotation by animateFloatAsState(
        targetValue = if (emotion == EmotionState.Curious) 15f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label = "rotation"
    )

    Canvas(modifier = Modifier.size(120.dp)) {
        rotate(rotation) {
            val center = center
            val radius = size.minDimension / 2
            
            // Outer Ring (Neon Identity Layer)
            drawCircle(
                style = Stroke(width = 4.dp.toPx()),
                brush = Brush.radialGradient(
                    colors = listOf(eyeColor.copy(alpha = 0.8f), Color.Transparent),
                    center = center,
                    radius = radius * 1.5f * pulseScale
                ),
                radius = radius * 0.9f
            )
            drawCircle(
                color = eyeColor,
                radius = radius * 0.9f,
                style = Stroke(width = 3.dp.toPx())
            )

            // Circuit details ... (kept existing logic)
            val circuitCount = 8
            for (i in 0 until circuitCount) {
                val angle = (2 * PI / circuitCount) * i
                val startX = center.x + (radius * 0.6f) * cos(angle).toFloat()
                val startY = center.y + (radius * 0.6f) * sin(angle).toFloat()
                val endX = center.x + (radius * 0.85f) * cos(angle).toFloat()
                val endY = center.y + (radius * 0.85f) * sin(angle).toFloat()
                drawLine(
                    color = eyeColor.copy(alpha = 0.5f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Inner Ring (Processing Layer)
            drawCircle(
                color = Color(0xFF0077FF),
                radius = radius * 0.5f,
                style = Stroke(width = 4.dp.toPx())
            )

            // Inner Core (Energy Source) - Moved by Tilt
            val eyelidHeight = radius * 0.4f * eyeHeightScale
            
            // Offset the core based on tilt
            val maxOffset = radius * 0.2f
            val offsetX = tiltX * maxOffset
            val offsetY = tiltY * maxOffset

            drawOval(
                color = Color.White,
                topLeft = Offset(center.x - radius * 0.2f + offsetX, center.y - eyelidHeight + offsetY),
                size = Size(radius * 0.4f, eyelidHeight * 2),
            )
        }
    }
}

@Composable
fun RoboNose(emotion: EmotionState, modifier: Modifier = Modifier) {
    val color by animateColorAsState(
        targetValue = if (emotion == EmotionState.Angry) Color.Red else Color.Cyan,
        label = "nose_color"
    )
    
    Canvas(modifier = modifier.size(40.dp, 30.dp)) {
        val path = Path().apply {
            moveTo(0f, size.height)
            lineTo(size.width / 2, 0f)
            lineTo(size.width, size.height)
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round)
        )
        
        // Sensor dot
        drawCircle(
            color = color,
            radius = 3.dp.toPx(),
            center = Offset(size.width / 2, size.height + 10.dp.toPx())
        )
    }
}

@Composable
fun RoboMouth(emotion: EmotionState, modifier: Modifier = Modifier) {
    // 5 bars for the mouth
    Row(
        modifier = modifier.height(60.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            MouthBar(index, emotion)
        }
    }
}

@Composable
fun MouthBar(index: Int, emotion: EmotionState) {
    val infiniteTransition = rememberInfiniteTransition(label = "mouth_anim")
    
    // Vary animation based on emotion
    val targetHeight = when (emotion) {
        EmotionState.Happy -> 0.8f // Higher bars
        EmotionState.Angry -> 1.0f // Very high, erratic
        EmotionState.Irritated -> 0.7f // Mid-high, nervous
        EmotionState.Sad -> 0.2f // Low
        EmotionState.Sleep -> 0.05f // Flat line
        else -> 0.4f // Idle
    }

    val speed = when(emotion) {
        EmotionState.Angry -> 100
        EmotionState.Irritated -> 150
        EmotionState.Happy -> 300
        else -> 800
    }

    // Randomize initial phase so bars don't move in unison perfect sync
    val offset = index * 100
    
    val heightScale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = targetHeight,
        animationSpec = infiniteRepeatable(
            animation = tween(speed, delayMillis = offset, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar_height"
    )

    val color by animateColorAsState(
        targetValue = when (emotion) {
            EmotionState.Angry -> Color.Red
            EmotionState.Irritated -> Color(0xFFFFA500)
            EmotionState.Happy -> Color.Green
            EmotionState.Sad -> Color.Blue
            else -> Color.Cyan
        },
        label = "bar_color"
    )
    
    // Apply the animated height
    // We use a Box with specific width and dynamic height
    Box(
        modifier = Modifier
            .width(12.dp)
            .fillMaxHeight(if (emotion == EmotionState.Sleep) 0.05f else heightScale.coerceAtLeast(0.05f)) // Ensure at least a line visible
            .graphicsLayer {
                // Glow effect could be done via shadow or drawing but simple rect here
            }
            .drawBehind {
                drawRoundRect(
                    color = color,
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                // Glow
                 drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0f), color.copy(alpha = 0.5f), color.copy(alpha = 0f))
                    ),
                    blendMode = BlendMode.Screen,
                    size = size.copy(height = size.height * 1.5f),
                    topLeft = Offset(0f, -size.height * 0.25f)
                )
            }
    )
}
