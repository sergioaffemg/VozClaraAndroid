package br.com.vozclara

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.NoiseSuppressor
import android.os.Process
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

/** Prototype loopback. NoiseSuppressor quality depends on the phone's Android audio implementation. */
class AudioEngine {
    private val running = AtomicBoolean(false)
    @Volatile private var gain = 1.69f
    private var recorder: AudioRecord? = null
    private var player: AudioTrack? = null
    private var suppressor: NoiseSuppressor? = null
    private var worker: Thread? = null

    fun setGainPercent(percent: Int) { gain = 0.5f + percent.coerceIn(0, 100) / 100f * 1.7f }

    @Synchronized fun start() {
        if (running.get()) return
        val rate = 48000
        val inMask = AudioFormat.CHANNEL_IN_MONO
        val outMask = AudioFormat.CHANNEL_OUT_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minIn = AudioRecord.getMinBufferSize(rate, inMask, encoding)
        val minOut = AudioTrack.getMinBufferSize(rate, outMask, encoding)
        require(minIn > 0 && minOut > 0) { "Formato de áudio não suportado" }
        val bytes = max(max(minIn, minOut), rate / 10 * 2)
        val input = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate, inMask, encoding, bytes * 2)
        require(input.state == AudioRecord.STATE_INITIALIZED) { input.release(); "Não foi possível abrir o microfone" }
        val output = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(encoding).setChannelMask(outMask).build())
            .setBufferSizeInBytes(bytes * 2)
            .setTransferMode(AudioTrack.MODE_STREAM).build()
        recorder = input; player = output
        try {
            if (NoiseSuppressor.isAvailable()) suppressor = NoiseSuppressor.create(input.audioSessionId)?.apply { enabled = true }
            output.play(); input.startRecording(); running.set(true)
            worker = Thread({
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                val buffer = ShortArray(bytes / 2)
                while (running.get()) {
                    val n = input.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                    if (n <= 0) continue
                    val g = gain
                    for (i in 0 until n) buffer[i] = (buffer[i].toInt() * g).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    var off = 0
                    while (off < n && running.get()) {
                        val written = output.write(buffer, off, n - off, AudioTrack.WRITE_BLOCKING)
                        if (written < 0) break
                        off += written
                    }
                }
            }, "VozClara-Audio").apply { start() }
        } catch (e: Exception) { running.set(false); release(); throw e }
    }

    @Synchronized fun stop() {
        running.set(false)
        try { recorder?.stop() } catch (_: Exception) {}
        try { worker?.join(700) } catch (_: InterruptedException) {}
        release()
    }

    private fun release() {
        try { suppressor?.release() } catch (_: Exception) {}
        suppressor = null
        try { recorder?.release() } catch (_: Exception) {}
        try { player?.stop() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        recorder = null; player = null; worker = null
    }
}
