package com.nkds.hosikoouma.jasmine

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes as AndroidAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.nkds.hosikoouma.jasmine.data.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    
    private lateinit var playerA: ExoPlayer
    private lateinit var processorA: CrossfadeAudioProcessor
    
    private lateinit var playerB: ExoPlayer
    private lateinit var processorB: CrossfadeAudioProcessor
    
    private var currentPlayer: ExoPlayer? = null
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var audioManager: AudioManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isCrossfading = false
    private var fadeJob: Job? = null
    
    private var playOnFocusGain = false
    private lateinit var focusRequest: AudioFocusRequest

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        
        setupAudioFocus()
        
        processorA = CrossfadeAudioProcessor()
        playerA = createPlayer(processorA)
        
        processorB = CrossfadeAudioProcessor()
        playerB = createPlayer(processorB)

        currentPlayer = playerA
        
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("OPEN_PLAYER", true)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, playerA)
            .setSessionActivity(pendingIntent)
            .setCallback(CustomMediaSessionCallback())
            .build()

        serviceScope.launch {
            while (isActive) {
                delay(300)
                checkCrossfadeCondition()
            }
        }
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            // ВАЖНО: Разрешаем ВСЕ доступные команды плеера (включая SEEK, CHANGE_VOLUME и т.д.)
            // и добавляем базовые сессионные команды.
            val availablePlayerCommands = session.player.availableCommands.buildUpon()
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_STOP)
                .add(Player.COMMAND_SET_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) // Для слайдера
                .build()
            
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(availablePlayerCommands)
                .build()
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val player = mediaSession.player
            if (player.mediaItemCount > 0) {
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        getAllItems(player),
                        player.currentMediaItemIndex,
                        player.currentPosition
                    )
                )
            }
            return Futures.immediateFailedFuture(UnsupportedOperationException())
        }

        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int
        ): Int {
            if (playerCommand == Player.COMMAND_SEEK_TO_NEXT || 
                playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS ||
                playerCommand == Player.COMMAND_SET_MEDIA_ITEM ||
                playerCommand == Player.COMMAND_STOP ||
                playerCommand == Player.COMMAND_SEEK_TO_MEDIA_ITEM ||
                playerCommand == Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM ||
                playerCommand == Player.COMMAND_PLAY_PAUSE) {
                cancelActiveCrossfade()
            }
            return super.onPlayerCommandRequest(session, controller, playerCommand)
        }
    }

    private fun cancelActiveCrossfade() {
        if (!isCrossfading) return
        fadeJob?.cancel()
        fadeJob = null
        
        val oldPlayer = if (currentPlayer == playerA) playerB else playerA
        val oldProcessor = if (oldPlayer == playerA) processorA else processorB
        val currentProcessor = if (currentPlayer == playerA) processorA else processorB
        
        oldPlayer.pause()
        oldPlayer.stop()
        oldProcessor.setVolumeScale(1.0f)
        currentProcessor.setVolumeScale(1.0f)
        
        isCrossfading = false
    }

    private fun setupAudioFocus() {
        val playbackAttributes = AndroidAudioAttributes.Builder()
            .setUsage(AndroidAudioAttributes.USAGE_MEDIA)
            .setContentType(AndroidAudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(playbackAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        cancelActiveCrossfade()
                        currentPlayer?.pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        if (currentPlayer?.isPlaying == true) {
                            playOnFocusGain = true
                            currentPlayer?.pause()
                        }
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        currentPlayer?.volume = 0.2f
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        currentPlayer?.volume = 1.0f
                        if (playOnFocusGain) {
                            currentPlayer?.play()
                            playOnFocusGain = false
                        }
                    }
                }
            }
            .build()
    }

    private fun requestManualAudioFocus(): Boolean {
        return audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun createPlayer(processor: CrossfadeAudioProcessor): ExoPlayer {
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(context: android.content.Context, enableFloat: Boolean, enableAudioTrack: Boolean): AudioSink {
                return DefaultAudioSink.Builder(context).setAudioProcessors(arrayOf(processor)).build()
            }
        }
        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(AudioAttributes.DEFAULT, false)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady && !isCrossfading) {
                    requestManualAudioFocus()
                }
                
                if (isCrossfading && player == currentPlayer) {
                    val otherPlayer = if (player == playerA) playerB else playerA
                    if (otherPlayer.playWhenReady != playWhenReady) {
                        otherPlayer.playWhenReady = playWhenReady
                    }
                }
            }
            
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (!isCrossfading) {
                    val otherPlayer = if (player == playerA) playerB else playerA
                    if (otherPlayer.isPlaying) {
                        otherPlayer.pause()
                        otherPlayer.stop()
                    }
                }
            }
        })
        
        return player
    }

    private suspend fun checkCrossfadeCondition() {
        val current = currentPlayer ?: return
        if (!current.isPlaying || isCrossfading) return

        val isEnabled = settingsRepository.isCrossfadeEnabled.first()
        val fadeDuration = settingsRepository.crossfadeDuration.first()
        
        if (!isEnabled) return

        val remaining = current.duration - current.currentPosition
        val isRepeatOne = current.repeatMode == Player.REPEAT_MODE_ONE
        val isRepeatAll = current.repeatMode == Player.REPEAT_MODE_ALL
        val hasNext = current.nextMediaItemIndex != C.INDEX_UNSET || isRepeatOne || isRepeatAll

        if (remaining in 200..fadeDuration && hasNext) {
            startOverlappingCrossfade(fadeDuration)
        }
    }

    private fun startOverlappingCrossfade(fadeDuration: Long) {
        isCrossfading = true
        val oldPlayer = currentPlayer!!
        val oldProcessor = if (oldPlayer == playerA) processorA else processorB
        
        val nextPlayer = if (oldPlayer == playerA) playerB else playerA
        val nextProcessor = if (nextPlayer == playerA) processorA else processorB
        
        val currentRepeatMode = oldPlayer.repeatMode
        val currentShuffleMode = oldPlayer.shuffleModeEnabled
        
        val nextIndex = if (currentRepeatMode == Player.REPEAT_MODE_ONE) {
            oldPlayer.currentMediaItemIndex
        } else if (oldPlayer.nextMediaItemIndex != C.INDEX_UNSET) {
            oldPlayer.nextMediaItemIndex
        } else if (currentRepeatMode == Player.REPEAT_MODE_ALL) {
            0
        } else {
            -1
        }

        if (nextIndex == -1) {
            isCrossfading = false
            return
        }

        val allItems = getAllItems(oldPlayer)
        nextProcessor.setVolumeScale(0f)
        nextPlayer.setMediaItems(allItems, nextIndex, 0L)
        nextPlayer.shuffleModeEnabled = currentShuffleMode
        nextPlayer.repeatMode = currentRepeatMode
        nextPlayer.prepare()
        nextPlayer.play()

        oldPlayer.repeatMode = Player.REPEAT_MODE_OFF
        if (currentRepeatMode != Player.REPEAT_MODE_ONE) {
            if (oldPlayer.mediaItemCount > nextIndex) {
                oldPlayer.removeMediaItems(nextIndex, oldPlayer.mediaItemCount)
            }
        }

        currentPlayer = nextPlayer
        mediaSession?.setPlayer(nextPlayer)

        fadeJob?.cancel()
        fadeJob = serviceScope.launch {
            val steps = 40
            val interval = (fadeDuration / steps).coerceAtLeast(10)
            for (i in 1..steps) {
                if (!isActive) break
                val progress = i.toFloat() / steps
                nextProcessor.setVolumeScale(progress)
                oldProcessor.setVolumeScale(1f - progress)
                delay(interval)
            }
            oldPlayer.pause()
            oldPlayer.stop()
            oldProcessor.setVolumeScale(1.0f)
            isCrossfading = false
            fadeJob = null
        }
    }

    private fun getAllItems(player: Player): List<MediaItem> {
        return List(player.mediaItemCount) { i -> player.getMediaItemAt(i) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        serviceScope.cancel()
        audioManager.abandonAudioFocusRequest(focusRequest)
        playerA.release()
        playerB.release()
        mediaSession?.release()
        super.onDestroy()
    }
}
