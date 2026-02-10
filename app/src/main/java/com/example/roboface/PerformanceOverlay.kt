package com.example.roboface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PerformanceOverlay(
    currentEmotion: EmotionState,
    inferenceTime: Long,
    isGpuDelegate: Boolean,
    tiltX: Float = 0f,
    tiltY: Float = 0f,
    rotationZ: Float = 0f,
    activityLevel: Float = 0f,
    stillTimeMs: Long = 0L,
    shakeCount: Int = 0,
    soundLevel: Float = 0f
) {
    var fps by remember { mutableStateOf(0) }
    var lastFrameTime by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTime != 0L) {
                    val frameDuration = frameTimeNanos - lastFrameTime
                    fps = (1_000_000_000 / frameDuration).toInt()
                }
                lastFrameTime = frameTimeNanos
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(8.dp)
    ) {
        Text("FPS: $fps", color = Color.Green, fontSize = 12.sp)
        Text("State: ${currentEmotion::class.simpleName}", color = Color.White, fontSize = 12.sp)
        
        Text("Activity: ${"%.2f".format(activityLevel)}", color = if (activityLevel > 0.5f) Color.Yellow else Color.Gray, fontSize = 10.sp)
        Text("Still: ${stillTimeMs / 1000}s", color = Color.White, fontSize = 10.sp)
        Text("Shakes: $shakeCount", color = if (shakeCount > 0) Color.Red else Color.Gray, fontSize = 10.sp)
        Text("Sound: ${"%.2f".format(soundLevel)}", color = if (soundLevel > 0.6f) Color.Red else Color.Gray, fontSize = 10.sp)
        
        Text("Tilt X: ${"%.2f".format(tiltX)}", color = Color.Cyan, fontSize = 10.sp)
        Text("Tilt Y: ${"%.2f".format(tiltY)}", color = Color.Cyan, fontSize = 10.sp)
        Text("Rot Z: ${"%.2f".format(rotationZ)}", color = Color.Cyan, fontSize = 10.sp)
        
        Text("Backend: ${if (isGpuDelegate) "GPU" else "CPU"}", color = Color.Yellow, fontSize = 10.sp)
        Text("Inference: ${inferenceTime}ms", color = Color.Yellow, fontSize = 10.sp)
    }
}
