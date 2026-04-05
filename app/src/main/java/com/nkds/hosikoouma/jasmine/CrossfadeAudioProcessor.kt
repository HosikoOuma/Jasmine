package com.nkds.hosikoouma.jasmine

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(UnstableApi::class)
class CrossfadeAudioProcessor : AudioProcessor {
    private var pendingOutputFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputFormat: AudioFormat = AudioFormat.NOT_SET
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    // Текущий коэффициент громкости (0.0 to 1.0)
    private var volumeScale = 1.0f

    fun setVolumeScale(scale: Float) {
        volumeScale = scale.coerceIn(0f, 1f)
    }

    override fun configure(inputFormat: AudioFormat): AudioFormat {
        if (inputFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputFormat)
        }
        pendingOutputFormat = inputFormat
        return pendingOutputFormat
    }

    override fun isActive(): Boolean = pendingOutputFormat != AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        var remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (buffer.capacity() < remaining) {
            buffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }

        // Применяем громкость к PCM данным
        while (inputBuffer.hasRemaining()) {
            val sample = inputBuffer.short
            val scaledSample = (sample * volumeScale).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            buffer.putShort(scaledSample)
        }

        buffer.flip()
        outputBuffer = buffer
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        outputFormat = pendingOutputFormat
    }

    override fun reset() {
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        outputFormat = AudioFormat.NOT_SET
        pendingOutputFormat = AudioFormat.NOT_SET
    }
}
