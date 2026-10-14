package ru.doshikologiya.nativepreview

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Exact WebAudio chime port from js/features/success-sound.js.
 * Four ascending sine notes + 2.02x sine harmonics. Each note uses a 15 ms
 * exponential attack and exponential decay, with 115 ms between entries.
 *
 * This is generated locally and played once on Oracle reveal and cooking
 * success; no system notification tone, network, vibration or permission.
 */
internal object MagicChime {
    private const val SAMPLE_RATE = 44100
    private val frequencies = doubleArrayOf(523.25, 659.25, 783.99, 1046.5)
    private val pcm: ShortArray by lazy { synthesize() }

    fun play() {
        thread(name = "doshik-magic-chime", isDaemon = true) {
            var player: AudioTrack? = null
            try {
                val data = pcm
                val format = AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
                player = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(format)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(data.size * 2)
                    .build()
                val written = player.write(data, 0, data.size, AudioTrack.WRITE_BLOCKING)
                if (written != data.size) return@thread
                player.play()
                Thread.sleep(1150)
            } catch (_: Exception) {
                // Sound is optional; recipe navigation and Oracle must never fail.
            } finally {
                runCatching { player?.stop() }
                runCatching { player?.release() }
            }
        }
    }

    private fun synthesize(): ShortArray {
        // WebAudio schedules the first note 20ms ahead, then each 115ms.
        val durationSeconds = .02 + 3 * .115 + (.55 + 3 * .045) + .03
        val buffer = ShortArray((durationSeconds * SAMPLE_RATE).roundToInt())
        for (i in buffer.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            var value = 0.0
            frequencies.forEachIndexed { note, frequency ->
                val start = .02 + note * .115
                val length = .55 + note * .045
                val localTime = t - start
                if (localTime >= 0.0 && localTime <= length) {
                    // WebAudio: setValueAtTime(.0001), exponentialRampToValueAtTime(
                    // volume, start+.015), exponentialRampToValueAtTime(.0001, end).
                    val components = arrayOf(1.0 to .10, 2.02 to .025)
                    for ((multiple, peakVolume) in components) {
                        val envelope = if (localTime <= .015) {
                            .0001 * exp(ln(peakVolume / .0001) * localTime / .015)
                        } else {
                            peakVolume * exp(
                                ln(.0001 / peakVolume) * (localTime - .015) / (length - .015)
                            )
                        }
                        value += envelope * sin(2.0 * PI * frequency * multiple * localTime)
                    }
                }
            }
            buffer[i] = (value.coerceIn(-1.0, 1.0) * 32767.0).roundToInt().toShort()
        }
        return buffer
    }
}
