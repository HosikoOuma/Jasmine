package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.abs

object WaveformHelper {
    suspend fun extractPeaks(context: Context, uri: Uri, bins: Int): FloatArray = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val trackCount = extractor.trackCount
            var audioTrackIndex = -1
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    break
                }
            }

            if (audioTrackIndex == -1) return@withContext FloatArray(bins)

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)
            val durationUs = format.getLong(MediaFormat.KEY_DURATION)
            val maxInputSize = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                1024 * 1024
            }
            val buffer = ByteBuffer.allocate(maxInputSize)

            val peaks = FloatArray(bins)
            val samplesPerBin = 100 // Rough approximation, we process chunks

            var binIndex = 0
            var currentMax = 0f
            
            // This is a simplified extractor-based peak analysis.
            // For a real app, you might want to decode to PCM, but that's very slow.
            // Here we look at raw buffer data as a proxy for amplitude (works better for some formats than others).
            
            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val presentationTimeUs = extractor.sampleTime
                binIndex = ((presentationTimeUs.toFloat() / durationUs.toFloat()) * bins).toInt().coerceIn(0, bins - 1)

                // Very crude: check average "fullness" of the buffer as a proxy for amplitude
                var sum = 0L
                for (i in 0 until sampleSize step 2) {
                    if (i + 1 < sampleSize) {
                        val sample = buffer.getShort(i).toInt()
                        sum += abs(sample)
                    }
                }
                val avg = sum.toFloat() / (sampleSize / 2).coerceAtLeast(1)
                if (avg > peaks[binIndex]) {
                    peaks[binIndex] = avg
                }

                extractor.advance()
            }

            // Normalize
            val maxPeak = peaks.maxOrNull() ?: 1f
            if (maxPeak > 0) {
                for (i in peaks.indices) {
                    peaks[i] = peaks[i] / maxPeak
                }
            }

            peaks
        } catch (e: Exception) {
            e.printStackTrace()
            FloatArray(bins)
        } finally {
            extractor.release()
        }
    }
}
