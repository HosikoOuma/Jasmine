package com.nkds.hosikoouma.jasmine.datamodels

import android.net.Uri

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val contentUri: Uri,
    val albumArtUri: Uri?,
    val uid: String = id.toString(),
    val isManual: Boolean = false
)
