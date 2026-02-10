package com.example.roboface

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

class EmotionEngine {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // State Flows for UI
    private val _currentEmotion = MutableStateFlow<EmotionState>(EmotionState.Curious)
    val currentEmotion: StateFlow<EmotionState> = _currentEmotion.asStateFlow()

    private val _tiltX = MutableStateFlow(0f)
    val tiltX: StateFlow<Float> = _tiltX.asStateFlow()

    private val _tiltY = MutableStateFlow(0f)
    val tiltY: StateFlow<Float> = _tiltY.asStateFlow()

    private val _rotationZ = MutableStateFlow(0f)
    val rotationZ: StateFlow<Float> = _rotationZ.asStateFlow()

    // Internal Metrics State
    private var metrics = BehaviorMetrics()
    private val _activityLevel = MutableStateFlow(0f)
    val activityLevel: StateFlow<Float> = _activityLevel.asStateFlow()

    private val _stillTimeMs = MutableStateFlow(0L)
    val stillTimeMs: StateFlow<Long> = _stillTimeMs.asStateFlow()

    private val _shakeCount = MutableStateFlow(0)
    val shakeCount: StateFlow<Int> = _shakeCount.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    // Stability Constants
    private val minEmotionDurationMs = 2000L
    private var lastEmotionChangeTime = System.currentTimeMillis()
    private var lastShakeResetTime = System.currentTimeMillis()

    // Thresholds
    private val highActivityThreshold = 0.5f // Activity level 
    private val motionThreshold = 0.15f // Threshold to consider "moving"

    init {
        // Main logic loop - evaluates emotion state twice per second
        scope.launch {
            while (isActive) {
                updateMetrics()
                evaluateEmotion()
                delay(500)
            }
        }
    }

    private fun updateMetrics() {
        val now = System.currentTimeMillis()
        val elapsedSinceMotion = now - metrics.lastMotionTime
        
        metrics = metrics.copy(stillTimeMs = elapsedSinceMotion)
        
        // Update flows for debug UI
        _stillTimeMs.value = metrics.stillTimeMs
        _activityLevel.value = metrics.activityLevel
        _shakeCount.value = metrics.shakeCount
        _soundLevel.value = metrics.soundLevel

        // Reset shake count if too old (3 seconds window)
        if (now - lastShakeResetTime > 3000 && metrics.shakeCount > 0) {
            metrics = metrics.copy(shakeCount = 0)
        }
    }

    private fun evaluateEmotion() {
        val now = System.currentTimeMillis()
        
        val current = _currentEmotion.value
        val isShaking = metrics.shakeCount >= 2
        val isMoving = metrics.stillTimeMs < 500
        val isVeryActive = metrics.activityLevel > highActivityThreshold

        // Priority-based state machine (Refined Logic)
        val targetState = when {
            // 1. Angry: Disturbance spikes (Overrides everything)
            isShaking -> EmotionState.Angry
            
            // 2. Proximity: Physically covered (Instant Sleep / "Do Not Disturb" mode)
            metrics.isNear -> EmotionState.Sleep

            // 3. Deep Sleep Maintenance: If already asleep and not disturbed by motion, stay asleep
            // This prevents sound from annoying him while he's in deep sleep
            current == EmotionState.Sleep && !isMoving && !isShaking -> EmotionState.Sleep

            // 4. Irritated: Constant Loud Noise
            metrics.soundLevel > 0.6f -> EmotionState.Irritated

            // 5. Happy: Sustained active engagement
            isVeryActive && metrics.stillTimeMs < 2000 -> EmotionState.Happy
            
            // 6. Sad: Inactive but awake
            metrics.stillTimeMs in 6000..15000 -> EmotionState.Sad
            
            // 7. Sleep: Prolonged inactivity (Time-based transition)
            metrics.stillTimeMs > 15000 -> EmotionState.Sleep
            
            // 8. Curious: Default baseline
            else -> EmotionState.Curious
        }

        // Stability Logic & Emergency Waking
        // Bypass minimum duration if shaking, covering sensor, noise, or waking up
        val isEmergency = (targetState == EmotionState.Angry) || 
                         (targetState == EmotionState.Irritated) ||
                         (targetState == EmotionState.Sleep && metrics.isNear) ||
                         (current == EmotionState.Sleep && isMoving && !metrics.isNear)
        
        if (targetState != current) {
            val cooldownElapsed = now - lastEmotionChangeTime >= minEmotionDurationMs
            
            if (isEmergency || cooldownElapsed) {
                _currentEmotion.value = targetState
                lastEmotionChangeTime = now
                
                // Reset interaction-specific counts when consumed
                if (targetState == EmotionState.Angry || targetState == EmotionState.Happy) {
                    // Stay in this state for a moment, then reset logic will naturally move back
                }

                // If we wake up or move to a baseline state, clear old shakes
                if (targetState == EmotionState.Curious || targetState == EmotionState.Idle) {
                    metrics = metrics.copy(shakeCount = 0)
                }
            }
        }
    }

    fun onShake() {
        val now = System.currentTimeMillis()
        metrics = metrics.copy(shakeCount = metrics.shakeCount + 1)
        lastShakeResetTime = now
        _shakeCount.value = metrics.shakeCount
    }

    fun onProximity(isNear: Boolean) {
        metrics = metrics.copy(isNear = isNear)
    }

    fun onMotionUpdate(x: Float, y: Float, rot: Float) {
        _tiltX.value = x
        _tiltY.value = y
        _rotationZ.value = rot

        // Calculate activity level magnitude
        val magnitude = kotlin.math.abs(x) + kotlin.math.abs(y)
        
        // Smooth activity level (Hysteresis/Filtering)
        val alpha = 0.1f
        val newActivity = alpha * magnitude + (1 - alpha) * metrics.activityLevel
        
        val now = System.currentTimeMillis()
        val lastMotion = if (magnitude > motionThreshold) now else metrics.lastMotionTime
        
        metrics = metrics.copy(
            activityLevel = newActivity,
            lastMotionTime = lastMotion
        )
    }

    fun onSoundUpdate(level: Float) {
        // Smooth the sound level to avoid flickering
        val alpha = 0.3f
        val smoothedSound = alpha * level + (1 - alpha) * metrics.soundLevel
        metrics = metrics.copy(soundLevel = smoothedSound)
    }
}
