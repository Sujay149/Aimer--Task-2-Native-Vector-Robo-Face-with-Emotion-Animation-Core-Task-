package com.example.roboface

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.log10

/**
 * Monitors ambient noise levels using the microphone.
 * Requires RECORD_AUDIO permission.
 */
class AudioInputManager(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var isRunning = false
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    var onSoundLevelUpdate: ((Float) -> Unit)? = null

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    fun start() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()
                isRunning = true
                startMonitoring()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startMonitoring() {
        job = scope.launch {
            val buffer = ShortArray(bufferSize)
            while (isRunning) {
                val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readSize > 0) {
                    var sum = 0.0
                    for (i in 0 until readSize) {
                        sum += abs(buffer[i].toInt()).toDouble()
                    }
                    val amplitude = sum / readSize
                    
                    // Convert to a normalized 0.0 - 1.0 range based on common voice levels
                    // Approx 0 to 32767 for PCM 16bit
                    val normalized = (amplitude / 3000.0).toFloat().coerceIn(0f, 1f)
                    withContext(Dispatchers.Main) {
                        onSoundLevelUpdate?.invoke(normalized)
                    }
                }
                delay(100) // Sample every 100ms
            }
        }
    }

    fun stop() {
        isRunning = false
        job?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
    }
}
