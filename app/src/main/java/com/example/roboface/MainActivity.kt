package com.example.roboface

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.example.roboface.ui.theme.RoboFaceTheme

class MainActivity : ComponentActivity() {
    private val emotionEngine = EmotionEngine()
    private lateinit var tfLiteManager: TFLiteManager
    private lateinit var sensorInputManager: SensorInputManager
    private lateinit var audioInputManager: AudioInputManager
    
    private val RECORD_AUDIO_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tfLiteManager = TFLiteManager(this)
        
        // Initialize Sensor Manager
        sensorInputManager = SensorInputManager(this).apply {
            onShakeDetected = {
                emotionEngine.onShake()
            }
            onProximityChanged = { isNear ->
                emotionEngine.onProximity(isNear)
            }
            onMotionUpdate = { tx, ty, rot ->
                emotionEngine.onMotionUpdate(tx, ty, rot)
            }
        }

        // Initialize Audio Manager
        audioInputManager = AudioInputManager(this).apply {
            onSoundLevelUpdate = { level ->
                emotionEngine.onSoundUpdate(level)
            }
        }

        requestAudioPermission()

        setContent {
            RoboFaceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    RoboApp(emotionEngine, tfLiteManager)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorInputManager.start()
        audioInputManager.start()
    }

    override fun onPause() {
        super.onPause()
        sensorInputManager.stop()
        audioInputManager.stop()
    }

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_CODE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tfLiteManager.close()
    }
}

@Composable
fun RoboApp(engine: EmotionEngine, tfManager: TFLiteManager) {
    val currentEmotion by engine.currentEmotion.collectAsState()
    val tiltX by engine.tiltX.collectAsState()
    val tiltY by engine.tiltY.collectAsState()
    val rotationZ by engine.rotationZ.collectAsState()
    
    // Collect new metrics for debug UI
    val activityLevel by engine.activityLevel.collectAsState()
    val stillTimeMs by engine.stillTimeMs.collectAsState()
    val shakeCount by engine.shakeCount.collectAsState()
    val soundLevel by engine.soundLevel.collectAsState()
    
    val stats by tfManager.getInferenceStats().collectAsState(initial = Pair(0L, false))

    var showHelp by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // FACE part - Now accepts tilt and rotation
            RoboFace(
                emotion = currentEmotion,
                tiltX = tiltX,
                tiltY = tiltY,
                rotationZ = rotationZ
            )
        }

        // Help Button
        Button(
            onClick = { showHelp = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.2f),
                contentColor = Color.White
            )
        ) {
            Text("How to Use")
        }

        // DEBUG OVERLAY - Now shows all sensor values and metrics
        PerformanceOverlay(
            currentEmotion = currentEmotion,
            inferenceTime = stats.first,
            isGpuDelegate = stats.second,
            tiltX = tiltX,
            tiltY = tiltY,
            rotationZ = rotationZ,
            activityLevel = activityLevel,
            stillTimeMs = stillTimeMs,
            shakeCount = shakeCount,
            soundLevel = soundLevel
        )

        if (showHelp) {
            HowToUseDialog(onDismiss = { showHelp = false })
        }
    }
}

@Composable
fun HowToUseDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How to Interact", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HelpItem("👋 Curious", "Hold the phone normally or move it lightly to keep him awake.")
                HelpItem("😄 Happy", "Move the phone continuously but smoothly to make him happy.")
                HelpItem("😐 Sad", "Leave him alone for 6 seconds, and he'll start to feel bored.")
                HelpItem("😴 Sleep", "Leave him for 15 seconds or cover the top sensor with your hand.")
                HelpItem("💢 Angry", "Shake the phone quickly to annoy him!")
                HelpItem("👂 Irritated", "Loud noises or yelling will make him nervous and irritated.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it!")
            }
        },
        containerColor = Color(0xFF1A1A1A),
        titleContentColor = Color.White,
        textContentColor = Color.LightGray
    )
}

@Composable
fun HelpItem(title: String, description: String) {
    Column {
        Text(title, color = Color.Cyan, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(description, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
    }
}
