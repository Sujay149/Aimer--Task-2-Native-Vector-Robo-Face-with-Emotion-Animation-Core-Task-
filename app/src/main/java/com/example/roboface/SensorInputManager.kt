package com.example.roboface

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

/**
 * Manages device sensors and converts raw data into semantic events for the EmotionEngine.
 * Implements low-pass filtering and threshold detection for shake events.
 */
class SensorInputManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    // Sensors
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    // Callbacks for events
    var onShakeDetected: (() -> Unit)? = null
    var onProximityChanged: ((Boolean) -> Unit)? = null
    var onMotionUpdate: ((tiltX: Float, tiltY: Float, rotationZ: Float) -> Unit)? = null

    // Low-pass filter alpha
    private val alpha = 0.2f
    private var filteredAccel = floatArrayOf(0f, 0f, 0f)
    private var filteredGyro = 0f

    // Shake detection variables
    private var lastShakeTime: Long = 0
    private val shakeThreshold = 14.0f // Lowered from 18.0 for better sensitivity

    // Proximity state
    private var isNear = false

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        proximity?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event.values)
            Sensor.TYPE_GYROSCOPE -> handleGyroscope(event.values)
            Sensor.TYPE_PROXIMITY -> handleProximity(event.values[0])
        }
    }

    private fun handleAccelerometer(values: FloatArray) {
        // Low-pass filter for tilt (X and Y)
        filteredAccel[0] = alpha * values[0] + (1 - alpha) * filteredAccel[0]
        filteredAccel[1] = alpha * values[1] + (1 - alpha) * filteredAccel[1]
        filteredAccel[2] = alpha * values[2] + (1 - alpha) * filteredAccel[2]

        // Shake detection using raw values for responsiveness
        val acceleration = kotlin.math.sqrt(
            values[0] * values[0] + values[1] * values[1] + values[2] * values[2]
        )
        
        if (acceleration > shakeThreshold) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastShakeTime > 400) { // Reduced cooldown to detect rapid shakes better
                lastShakeTime = currentTime
                onShakeDetected?.invoke()
            }
        }

        // Tilt mapping: X axis (-10 to 10) and Y axis (-10 to 10)
        // Normalize to -1.0 to 1.0 range
        val tiltX = (-filteredAccel[0] / 9.81f).coerceIn(-1f, 1f)
        val tiltY = (filteredAccel[1] / 9.81f).coerceIn(-1f, 1f)
        
        onMotionUpdate?.invoke(tiltX, tiltY, filteredGyro)
    }

    private fun handleGyroscope(values: FloatArray) {
        // We use Z-axis rotation for head tilt
        // Low-pass filter
        filteredGyro = alpha * values[2] + (1 - alpha) * filteredGyro
        
        // Note: filteredGyro here is rad/s, we'll use it for subtle rotation
    }

    private fun handleProximity(value: Float) {
        // Most proximity sensors return 0 for 'near' and something else for 'far'
        val near = value < (proximity?.maximumRange ?: 5f)
        if (near != isNear) {
            isNear = near
            onProximityChanged?.invoke(isNear)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}
