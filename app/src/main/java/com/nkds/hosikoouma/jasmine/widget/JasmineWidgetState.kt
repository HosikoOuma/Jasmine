package com.nkds.hosikoouma.jasmine.widget

import kotlinx.serialization.Serializable

@Serializable
data class JasmineWidgetState(
    val title: String = "Unknown",
    val artist: String = "Unknown",
    val isPlaying: Boolean = false,
    val albumArtUri: String? = null,
    val showFeedback: Boolean = false
)
