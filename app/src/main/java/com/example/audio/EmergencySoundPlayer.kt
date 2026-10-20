package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class EmergencySoundPlayer(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    var isSoundEnabled: Boolean = true

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Plays a distinct dual-tone emergency beep when a checklist is initiated.
     */
    fun playInitiationBeep() {
        vibrate(longArrayOf(0, 70, 40, 110), intArrayOf(0, 180, 0, 255))
        if (!isSoundEnabled) return

        scope.launch {
            try {
                // Tone 1: 880 Hz (A5) for 110ms + Tone 2: 1320 Hz (E6) for 150ms
                val sampleRate = 44100
                val tone1 = generateTone(880.0, 110, sampleRate)
                val silence = ShortArray((sampleRate * 0.03).toInt())
                val tone2 = generateTone(1320.0, 160, sampleRate)

                val combined = ShortArray(tone1.size + silence.size + tone2.size)
                System.arraycopy(tone1, 0, combined, 0, tone1.size)
                System.arraycopy(silence, 0, combined, tone1.size, silence.size)
                System.arraycopy(tone2, 0, combined, tone1.size + silence.size, tone2.size)

                playPcmData(combined, sampleRate)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Plays a celebratory positive jingle when a user successfully completes an emergency checklist.
     * Ascending major arpeggio: C5 -> E5 -> G5 -> C6 with smooth bell decay.
     */
    fun playCelebrationJingle() {
        vibrate(longArrayOf(0, 80, 50, 100, 50, 180), intArrayOf(0, 160, 0, 200, 0, 255))
        if (!isSoundEnabled) return

        scope.launch {
            try {
                val sampleRate = 44100
                // Major arpeggio frequencies: C5, E5, G5, C6
                val c5 = generateTone(523.25, 130, sampleRate, attackFrac = 0.08f, decayFrac = 0.2f)
                val e5 = generateTone(659.25, 130, sampleRate, attackFrac = 0.08f, decayFrac = 0.2f)
                val g5 = generateTone(783.99, 150, sampleRate, attackFrac = 0.08f, decayFrac = 0.2f)
                val c6 = generateTone(1046.50, 420, sampleRate, attackFrac = 0.05f, decayFrac = 0.7f)

                val totalLength = c5.size + e5.size + g5.size + c6.size
                val combined = ShortArray(totalLength)
                var offset = 0

                System.arraycopy(c5, 0, combined, offset, c5.size)
                offset += c5.size
                System.arraycopy(e5, 0, combined, offset, e5.size)
                offset += e5.size
                System.arraycopy(g5, 0, combined, offset, g5.size)
                offset += g5.size
                System.arraycopy(c6, 0, combined, offset, c6.size)

                playPcmData(combined, sampleRate)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun generateTone(
        frequency: Double,
        durationMs: Int,
        sampleRate: Int,
        attackFrac: Float = 0.1f,
        decayFrac: Float = 0.2f
    ): ShortArray {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        val attackSamples = (numSamples * attackFrac).toInt().coerceAtLeast(1)
        val decayStart = (numSamples * (1.0f - decayFrac)).toInt().coerceIn(attackSamples, numSamples)

        for (i in 0 until numSamples) {
            val angle = 2.0 * PI * i / (sampleRate / frequency)
            val baseSine = sin(angle)

            // Envelope to avoid popping / clicking
            val envelope = when {
                i < attackSamples -> i.toDouble() / attackSamples
                i > decayStart -> (numSamples - i).toDouble() / (numSamples - decayStart)
                else -> 1.0
            }

            buffer[i] = (baseSine * envelope * Short.MAX_VALUE * 0.75).toInt().toShort()
        }
        return buffer
    }

    private fun playPcmData(data: ShortArray, sampleRate: Int) {
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(data.size * 2)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(data, 0, data.size)
        audioTrack.play()

        // Release after finish
        scope.launch {
            val durationMs = (data.size * 1000L) / sampleRate + 200
            kotlinx.coroutines.delay(durationMs)
            audioTrack.stop()
            audioTrack.release()
        }
    }

    private fun vibrate(timings: LongArray, amplitudes: IntArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(timings, -1)
            }
        } catch (_: Exception) {}
    }
}
