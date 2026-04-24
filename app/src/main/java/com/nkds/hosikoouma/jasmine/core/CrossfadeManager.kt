package com.nkds.hosikoouma.jasmine.core

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.nkds.hosikoouma.jasmine.CrossfadeAudioProcessor
import kotlinx.coroutines.*

class CrossfadeManager(
    private val serviceScope: CoroutineScope,
    private val playerA: ExoPlayer,
    private val processorA: CrossfadeAudioProcessor,
    private val playerB: ExoPlayer,
    private val processorB: CrossfadeAudioProcessor,
    private val onPlayerSwapped: (ExoPlayer) -> Unit
) {
    private var currentPlayer: ExoPlayer = playerA
    var isCrossfading = false
        private set
        
    private var fadeJob: Job? = null
    private var crossfadeCheckJob: Job? = null

    var isEnabled = true
    var durationMs = 3000L

    fun getCurrentPlayer() = currentPlayer

    fun cancelActiveCrossfade() {
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
        scheduleCrossfade()
    }

    fun scheduleCrossfade() {
        val player = currentPlayer
        crossfadeCheckJob?.cancel()

        if (!isEnabled || isCrossfading || !player.isPlaying || player.playbackState != Player.STATE_READY) return
        
        val isRadio = player.currentMediaItem?.mediaMetadata?.extras?.getBoolean("isRadio") ?: false
        if (isRadio) return

        val duration = player.duration
        if (duration == C.TIME_UNSET || duration <= 0) return

        val remaining = duration - player.currentPosition
        
        // Если осталось слишком мало времени, не планируем
        if (remaining < 200) return 

        val delayMs = remaining - durationMs

        crossfadeCheckJob = serviceScope.launch {
            // Если мы УЖЕ в зоне кроссфейда (например, после перемотки), 
            // мы НЕ запускаем его, чтобы избежать наложения "самого на себя" или 
            // двойного срабатывания. Кроссфейд должен быть запланирован заранее.
            if (delayMs <= 0) return@launch
            
            delay(delayMs)
            
            if (player == currentPlayer && player.isPlaying && !isCrossfading && player.playbackState == Player.STATE_READY) {
                val hasNext = player.nextMediaItemIndex != C.INDEX_UNSET || 
                             player.repeatMode != Player.REPEAT_MODE_OFF
                
                if (hasNext) startOverlappingCrossfade()
            }
        }
    }

    private fun startOverlappingCrossfade() {
        isCrossfading = true
        val oldPlayer = currentPlayer
        val nextPlayer = if (oldPlayer == playerA) playerB else playerA
        val nextProcessor = if (nextPlayer == playerA) processorA else processorB
        val oldProcessor = if (oldPlayer == playerA) processorA else processorB
        
        val currentRepeatMode = oldPlayer.repeatMode
        val currentShuffleMode = oldPlayer.shuffleModeEnabled
        
        val nextIndex = when {
            currentRepeatMode == Player.REPEAT_MODE_ONE -> oldPlayer.currentMediaItemIndex
            oldPlayer.nextMediaItemIndex != C.INDEX_UNSET -> oldPlayer.nextMediaItemIndex
            currentRepeatMode == Player.REPEAT_MODE_ALL -> 0
            else -> -1
        }

        if (nextIndex == -1) {
            isCrossfading = false
            return
        }

        val allItems = List(oldPlayer.mediaItemCount) { oldPlayer.getMediaItemAt(it) }
        
        serviceScope.launch {
            delay(150) // Стабилизация
            
            if (!isActive || oldPlayer != currentPlayer || !oldPlayer.isPlaying) {
                isCrossfading = false
                return@launch
            }

            withContext(Dispatchers.Main) {
                nextProcessor.setVolumeScale(0f)
                nextPlayer.setMediaItems(allItems, nextIndex, 0L)
                nextPlayer.shuffleModeEnabled = currentShuffleMode
                nextPlayer.repeatMode = currentRepeatMode
                nextPlayer.prepare()
                
                currentPlayer = nextPlayer
                onPlayerSwapped(nextPlayer)
                
                nextPlayer.play()

                startFadeAnimation(oldPlayer, oldProcessor, nextProcessor)
            }
        }
    }

    private fun startFadeAnimation(
        oldPlayer: ExoPlayer,
        oldProcessor: CrossfadeAudioProcessor,
        nextProcessor: CrossfadeAudioProcessor
    ) {
        fadeJob?.cancel()
        fadeJob = serviceScope.launch(Dispatchers.Default) {
            val steps = 30 
            val interval = (durationMs / steps).coerceAtLeast(16)
            for (i in 1..steps) {
                if (!isActive) break
                val progress = i.toFloat() / steps
                nextProcessor.setVolumeScale(progress)
                oldProcessor.setVolumeScale(1f - progress)
                delay(interval)
            }
            withContext(Dispatchers.Main) {
                oldPlayer.pause()
                oldPlayer.stop()
                oldProcessor.setVolumeScale(1.0f)
                isCrossfading = false
                scheduleCrossfade()
            }
        }
    }

    fun release() {
        crossfadeCheckJob?.cancel()
        fadeJob?.cancel()
        processorA.release()
        processorB.release()
        playerA.release()
        playerB.release()
    }
}
