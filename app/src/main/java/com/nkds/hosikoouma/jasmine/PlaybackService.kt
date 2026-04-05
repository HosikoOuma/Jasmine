package com.nkds.hosikoouma.jasmine

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

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        
        processorA = CrossfadeAudioProcessor()
        playerA = createPlayer(processorA)
        
        processorB = CrossfadeAudioProcessor()
        playerB = createPlayer(processorB)

        currentPlayer = playerA
        mediaSession = MediaSession.Builder(this, playerA).build()

        requestManualAudioFocus()

        serviceScope.launch {
            while (isActive) {
                delay(300)
                checkCrossfadeCondition()
            }
        }
    }

    private fun createPlayer(processor: CrossfadeAudioProcessor): ExoPlayer {
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(context: android.content.Context, enableFloat: Boolean, enableAudioTrack: Boolean): AudioSink {
                return DefaultAudioSink.Builder(context).setAudioProcessors(arrayOf(processor)).build()
            }
        }
        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(AudioAttributes.DEFAULT, false)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // Синхронизируем паузу только если она пришла от ТЕКУЩЕГО (активного) плеера
                // Это предотвращает остановку нового трека, когда старый встает на паузу в конце кроссфейда
                if (isCrossfading && player == currentPlayer) {
                    val otherPlayer = if (player == playerA) playerB else playerA
                    if (otherPlayer.playWhenReady != playWhenReady) {
                        otherPlayer.playWhenReady = playWhenReady
                    }
                }
            }
        })
        
        return player
    }

    private fun requestManualAudioFocus() {
        val playbackAttributes = AndroidAudioAttributes.Builder()
            .setUsage(AndroidAudioAttributes.USAGE_MEDIA)
            .setContentType(AndroidAudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(playbackAttributes)
            .build()
        audioManager.requestAudioFocus(focusRequest)
    }

    private suspend fun checkCrossfadeCondition() {
        val current = currentPlayer ?: return
        if (!current.isPlaying || isCrossfading) return

        val isEnabled = settingsRepository.isCrossfadeEnabled.first()
        val fadeDuration = settingsRepository.crossfadeDuration.first()
        
        if (!isEnabled) return

        val remaining = current.duration - current.currentPosition
        if (remaining in 200..fadeDuration && current.nextMediaItemIndex != C.INDEX_UNSET) {
            startOverlappingCrossfade(fadeDuration)
        }
    }

    private fun startOverlappingCrossfade(fadeDuration: Long) {
        isCrossfading = true
        val oldPlayer = currentPlayer!!
        val oldProcessor = if (oldPlayer == playerA) processorA else processorB
        
        val nextPlayer = if (oldPlayer == playerA) playerB else playerA
        val nextProcessor = if (nextPlayer == playerA) processorA else processorB
        
        val nextIndex = oldPlayer.nextMediaItemIndex
        val allItems = getAllItems(oldPlayer)

        nextProcessor.setVolumeScale(0f)
        nextPlayer.setMediaItems(allItems, nextIndex, 0L)
        nextPlayer.prepare()
        nextPlayer.play()

        if (oldPlayer.mediaItemCount > nextIndex) {
            oldPlayer.removeMediaItems(nextIndex, oldPlayer.mediaItemCount)
        }

        // Мы СРАЗУ меняем currentPlayer на новый. 
        // Теперь все команды "пауза" от пользователя будут идти к nextPlayer.
        currentPlayer = nextPlayer
        mediaSession?.setPlayer(nextPlayer)

        fadeJob?.cancel()
        fadeJob = serviceScope.launch {
            val steps = 40
            val interval = fadeDuration / steps
            
            for (i in 1..steps) {
                val progress = i.toFloat() / steps
                nextProcessor.setVolumeScale(progress)
                oldProcessor.setVolumeScale(1f - progress)
                delay(interval)
            }
            
            // Теперь, когда мы вызываем pause() у старого плеера, 
            // проверка (player == currentPlayer) в слушателе будет ложной, 
            // и основной (новый) плеер не остановится.
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
        playerA.release()
        playerB.release()
        mediaSession?.release()
        super.onDestroy()
    }
}
