package com.nkds.hosikoouma.jasmine.core

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.nkds.hosikoouma.jasmine.PlaybackService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlaybackController(private val application: Application) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    val controller: MediaController?
        get() = if (controllerFuture?.isDone == true) {
            try { controllerFuture?.get() } catch (e: Exception) { null }
        } else null

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0L)
    val progress: StateFlow<Long> = _progress.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _systemVolume = MutableStateFlow(0f)
    val systemVolume: StateFlow<Float> = _systemVolume.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _playbackPitch = MutableStateFlow(1.0f)
    val playbackPitch: StateFlow<Float> = _playbackPitch.asStateFlow()

    var listener: Listener? = null

    interface Listener {
        fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?)
        fun onTimelineChanged()
        fun onMediaMetadataChanged(metadata: androidx.media3.common.MediaMetadata)
    }

    private var progressJob: Job? = null
    private var scope: CoroutineScope? = null

    private var controllerConnected = false

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") updateVolumeState()
        }
    }

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
        initializeController()
        setupVolumeReceiver()
    }

    private fun initializeController() {
        if (controllerConnected) return
        controllerFuture?.let { MediaController.releaseFuture(it) }
        val sessionToken = SessionToken(application, ComponentName(application, PlaybackService::class.java))
        val future = MediaController.Builder(application, sessionToken).buildAsync()
        controllerFuture = future
        Futures.addCallback(future, object : FutureCallback<MediaController> {
            override fun onSuccess(result: MediaController?) {
                controllerConnected = true
                setupController()
            }
            override fun onFailure(t: Throwable) {
                Log.e("PlaybackController", "Failed to connect MediaController, retrying in 2s", t)
                scope?.launch {
                    delay(2000)
                    initializeController()
                }
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupVolumeReceiver() {
        updateVolumeState()
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                application.registerReceiver(volumeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                application.registerReceiver(volumeReceiver, filter)
            }
        } catch (e: Exception) {
            Log.e("PlaybackController", "Failed to register volume receiver", e)
        }
    }

    private fun updateVolumeState() {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        _systemVolume.value = current.toFloat() / max.toFloat()
    }

    private fun setupController() {
        val c = controller ?: return
        _repeatMode.value = c.repeatMode
        _shuffleModeEnabled.value = c.shuffleModeEnabled
        _playbackSpeed.value = c.playbackParameters.speed
        _playbackPitch.value = c.playbackParameters.pitch
        _isPlaying.value = c.isPlaying
        _duration.value = if (c.duration > 0) c.duration else 0L
        _progress.value = if (c.currentPosition > 0) c.currentPosition else 0L

        if (c.isPlaying) startProgressUpdate()

        c.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                listener?.onMediaItemTransition(mediaItem)
            }
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                listener?.onTimelineChanged()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startProgressUpdate() else stopProgressUpdate()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = c.duration
                    _progress.value = c.currentPosition
                }
            }
            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                listener?.onMediaMetadataChanged(mediaMetadata)
            }
            override fun onPlaybackParametersChanged(params: PlaybackParameters) {
                _playbackSpeed.value = params.speed
                _playbackPitch.value = params.pitch
            }
            override fun onRepeatModeChanged(mode: Int) { _repeatMode.value = mode }
            override fun onShuffleModeEnabledChanged(enabled: Boolean) { _shuffleModeEnabled.value = enabled }
        })
    }

    fun togglePlayPause() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun seekTo(position: Long) { controller?.seekTo(position); _progress.value = position }
    fun skipToNext() { controller?.let { it.seekToNext(); it.play() } }
    fun setShuffleModeEnabled(enabled: Boolean) { _shuffleModeEnabled.value = enabled }
    fun toggleRepeatMode() { controller?.let { it.repeatMode = when (it.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF } } }
    fun setPlaybackSpeed(speed: Float) { controller?.let { it.playbackParameters = PlaybackParameters(speed, it.playbackParameters.pitch) } }
    fun setPlaybackPitch(pitch: Float) { controller?.let { it.playbackParameters = PlaybackParameters(it.playbackParameters.speed, pitch) } }
    fun setSystemVolume(vol: Float) { scope?.launch(Dispatchers.IO) { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (vol * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toInt(), 0); _systemVolume.value = vol } }

    private fun startProgressUpdate() { progressJob?.cancel(); progressJob = scope?.launch { while (true) { controller?.let { _progress.value = it.currentPosition }; delay(1000) } } }
    private fun stopProgressUpdate() { progressJob?.cancel() }

    fun release() {
        stopProgressUpdate()
        try { controller?.release() } catch (_: Exception) {}
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture!!)
        try { application.unregisterReceiver(volumeReceiver) } catch (_: Exception) {}
    }
}
