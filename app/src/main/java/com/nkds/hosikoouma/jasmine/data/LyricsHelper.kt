package com.nkds.hosikoouma.jasmine.data

import com.nkds.hosikoouma.jasmine.datamodels.LyricsLine
import java.util.regex.Pattern

object LyricsHelper {
    private val lrcPattern = Pattern.compile("\\[(\\d+):(\\d+)\\.(\\d+)\\](.*)")

    fun parseLrc(lrcContent: String?): List<LyricsLine>? {
        if (lrcContent.isNullOrBlank()) return null
        
        val lines = mutableListOf<LyricsLine>()
        lrcContent.lineSequence().forEach { line ->
            val matcher = lrcPattern.matcher(line)
            if (matcher.find()) {
                val min = matcher.group(1)?.toLong() ?: 0L
                val sec = matcher.group(2)?.toLong() ?: 0L
                val ms = matcher.group(3)?.toLong() ?: 0L
                val text = matcher.group(4)?.trim() ?: ""
                
                // LRCLIB typical format is [mm:ss.SS] where SS is hundredths
                val timestamp = min * 60 * 1000 + sec * 1000 + ms * 10
                lines.add(LyricsLine(timestamp, text))
            }
        }
        return lines.ifEmpty { null }
    }
}
