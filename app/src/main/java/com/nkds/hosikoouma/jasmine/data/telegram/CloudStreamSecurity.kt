package com.nkds.hosikoouma.jasmine.data.telegram

import io.ktor.http.HttpStatusCode

/**
 * Валидация запросов для локального прокси-сервера.
 */
object CloudStreamSecurity {
    const val MAX_STREAM_CONTENT_LENGTH_BYTES: Long = 2L * 1024L * 1024L * 1024L // 2GB

    private const val MAX_RANGE_HEADER_LENGTH = 64
    private const val MAX_RANGE_VALUE_BYTES = 8L * 1024L * 1024L * 1024L

    data class RangeHeaderValidation(
        val isValid: Boolean,
        val normalizedHeader: String? = null,
        val startInclusive: Long? = null,
        val endInclusive: Long? = null,
        val isSuffixRange: Boolean = false
    )

    fun validateTelegramFileId(fileId: Int): Boolean = fileId > 0

    fun validateRangeHeader(rawHeader: String?): RangeHeaderValidation {
        if (rawHeader.isNullOrBlank()) {
            return RangeHeaderValidation(isValid = true)
        }

        val header = rawHeader.trim()
        if (header.length > MAX_RANGE_HEADER_LENGTH) {
            return RangeHeaderValidation(isValid = false)
        }
        if (!header.startsWith("bytes=") || header.contains(",")) {
            return RangeHeaderValidation(isValid = false)
        }

        val payload = header.removePrefix("bytes=")
        val dashIndex = payload.indexOf('-')
        if (dashIndex <= -1 || payload.indexOf('-', dashIndex + 1) != -1) {
            return RangeHeaderValidation(isValid = false)
        }

        val startPart = payload.substring(0, dashIndex).trim()
        val endPart = payload.substring(dashIndex + 1).trim()

        if (startPart.isEmpty() && endPart.isEmpty()) {
            return RangeHeaderValidation(isValid = false)
        }
        
        val start = startPart.toLongOrNull()
        val end = endPart.toLongOrNull()

        if (start != null && (start < 0 || start > MAX_RANGE_VALUE_BYTES)) {
            return RangeHeaderValidation(isValid = false)
        }
        if (end != null && (end < 0 || end > MAX_RANGE_VALUE_BYTES)) {
            return RangeHeaderValidation(isValid = false)
        }
        if (start != null && end != null && start > end) {
            return RangeHeaderValidation(isValid = false)
        }

        return RangeHeaderValidation(
            isValid = true,
            normalizedHeader = "bytes=$startPart-$endPart",
            startInclusive = start,
            endInclusive = end,
            isSuffixRange = start == null && end != null
        )
    }
}
