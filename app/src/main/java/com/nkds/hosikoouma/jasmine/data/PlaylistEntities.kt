package com.nkds.hosikoouma.jasmine.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coverUri: String? = null, // URI или путь к обложке плейлиста
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: Long, // MediaStore ID
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "current_queue")
data class QueueTrackEntity(
    @PrimaryKey(autoGenerate = true) val queueId: Long = 0,
    val trackId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val contentUri: String,
    val albumArtUri: String?,
    val path: String,
    val isManual: Boolean,
    val sourceName: String?,
    val orderIndex: Int
)
