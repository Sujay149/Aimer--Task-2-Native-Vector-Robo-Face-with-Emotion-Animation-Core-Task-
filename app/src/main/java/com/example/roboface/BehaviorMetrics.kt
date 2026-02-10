package com.example.roboface

/**
 * Encapsulates accumulated sensor signals and behavior data used to derive emotions.
 */
data class BehaviorMetrics(
    val activityLevel: Float = 0f,
    val stillTimeMs: Long = 0L,
    val shakeCount: Int = 0,
    val isNear: Boolean = false,
    val soundLevel: Float = 0f,
    val lastMotionTime: Long = System.currentTimeMillis()
)
