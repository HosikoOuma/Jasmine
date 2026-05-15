package com.nkds.hosikoouma.jasmine.viewmodels

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.nkds.hosikoouma.jasmine.data.TrackRepository
import com.nkds.hosikoouma.jasmine.data.WaveformHelper
import com.nkds.hosikoouma.jasmine.datamodels.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import androidx.media3.common.util.UnstableApi

@HiltViewModel
class TrimViewModel @Inject constructor(
    application: Application,
    private val trackRepository: TrackRepository
) : AndroidViewModel(application) {

    private val _track = MutableStateFlow<Track?>(null)
    val track = _track.asStateFlow()

    private val _waveform = MutableStateFlow<FloatArray>(FloatArray(0))
    val waveform = _waveform.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress = _exportProgress.asStateFlow()

    private val _startTime = MutableStateFlow(0L)
    val startTime = _startTime.asStateFlow()

    private val _endTime = MutableStateFlow(0L)
    val endTime = _endTime.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private var exoPlayer: ExoPlayer? = null

    init {
        exoPlayer = ExoPlayer.Builder(application).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }
                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION || reason == Player.DISCONTINUITY_REASON_SEEK) {
                         checkBounds()
                    }
                }
            })
        }
        
        viewModelScope.launch {
            while (true) {
                exoPlayer?.let {
                    _currentPosition.value = it.currentPosition
                    if (it.isPlaying && it.currentPosition >= _endTime.value) {
                        it.pause()
                        it.seekTo(_startTime.value)
                    }
                }
                kotlinx.coroutines.delay(50)
            }
        }
    }

    private fun checkBounds() {
        exoPlayer?.let {
            if (it.currentPosition < _startTime.value) {
                it.seekTo(_startTime.value)
            }
        }
    }

    fun loadTrack(trackId: Long) {
        viewModelScope.launch {
            val t = trackRepository.allTracks.value.find { it.id == trackId }
            _track.value = t
            t?.let {
                _endTime.value = it.duration
                _startTime.value = 0L
                exoPlayer?.setMediaItem(MediaItem.fromUri(it.contentUri))
                exoPlayer?.prepare()
                
                // Generate waveform
                _waveform.value = WaveformHelper.extractPeaks(getApplication(), it.contentUri, 100)
            }
        }
    }

    fun setStartTime(time: Long) {
        _startTime.value = time.coerceIn(0, _endTime.value)
        if (exoPlayer?.currentPosition ?: 0 < _startTime.value) {
            exoPlayer?.seekTo(_startTime.value)
        }
    }

    fun setEndTime(time: Long) {
        _endTime.value = time.coerceIn(_startTime.value, _track.value?.duration ?: 0)
    }

    fun togglePlayPause() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                if (it.currentPosition >= _endTime.value || it.currentPosition < _startTime.value) {
                    it.seekTo(_startTime.value)
                }
                it.play()
            }
        }
    }

    @UnstableApi
    fun export(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentTrack = _track.value ?: return
        val start = _startTime.value
        val end = _endTime.value
        
        _isExporting.value = true
        
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val transformer = Transformer.Builder(context).build()
                
                val mediaItem = MediaItem.Builder()
                    .setUri(currentTrack.contentUri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(start)
                            .setEndPositionMs(end)
                            .build()
                    )
                    .build()
                
                val outputDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Jasmine/Trimmed")
                if (!outputDir.exists()) outputDir.mkdirs()
                
                val fileName = "${currentTrack.title}_trimmed_${System.currentTimeMillis()}.mp3"
                val outputFile = File(outputDir, fileName)

                val editedMediaItem = EditedMediaItem.Builder(mediaItem).build()

                transformer.addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        viewModelScope.launch {
                            addToMediaStore(outputFile)
                            _isExporting.value = false
                            onSuccess()
                        }
                    }

                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: androidx.media3.transformer.ExportException) {
                        _isExporting.value = false
                        onError(exportException.message ?: "Unknown error")
                    }
                })

                transformer.start(editedMediaItem, outputFile.absolutePath)

            } catch (e: Exception) {
                _isExporting.value = false
                onError(e.message ?: "Unknown error")
            }
        }
    }

    private fun addToMediaStore(file: File) {
        val context = getApplication<Application>()
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Jasmine/Trimmed")
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
        }
        context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        trackRepository.loadTracks()
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
}
