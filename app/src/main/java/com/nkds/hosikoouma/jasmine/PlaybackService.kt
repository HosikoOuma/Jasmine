package com.nkds.hosikoouma.jasmine

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.media.AudioAttributes as AndroidAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nkds.hosikoouma.jasmine.data.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.nio.charset.Charset
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.compose.ui.graphics.asImageBitmap
import com.kmpalette.palette.graphics.Palette
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.io.FileNotFoundException

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

    private var isCrossfadeEnabled = true
    private var crossfadeDurationMs = 3000L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_WIDGET_PLAY_PAUSE" -> {
                currentPlayer?.let { if (it.isPlaying) it.pause() else it.play() }
            }
            "ACTION_WIDGET_NEXT" -> currentPlayer?.seekToNext()
            "ACTION_WIDGET_PREV" -> currentPlayer?.seekToPrevious()
            "ACTION_WIDGET_UPDATE_REQUEST" -> pushWidgetUpdate()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        
        setupAudioFocus()
        observeSettings()
        
        processorA = CrossfadeAudioProcessor()
        playerA = createPlayer(processorA, "PlayerA")
        
        processorB = CrossfadeAudioProcessor()
        playerB = createPlayer(processorB, "PlayerB")

        currentPlayer = playerA
        
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("OPEN_PLAYER", true)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationProvider = DefaultMediaNotificationProvider(this)
        notificationProvider.setSmallIcon(R.drawable.ison_vec)
        setMediaNotificationProvider(notificationProvider)

        mediaSession = MediaSession.Builder(this, playerA)
            .setSessionActivity(pendingIntent)
            .setCallback(CustomMediaSessionCallback())
            .build()

        serviceScope.launch {
            while (isActive) {
                delay(500)
                checkCrossfadeCondition()
            }
        }
    }

    private fun observeSettings() {
        serviceScope.launch {
            settingsRepository.isCrossfadeEnabled.collectLatest { isCrossfadeEnabled = it }
        }
        serviceScope.launch {
            settingsRepository.crossfadeDuration.collectLatest { crossfadeDurationMs = it }
        }
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val availableSessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().build()
            val availablePlayerCommands = session.player.availableCommands.buildUpon()
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_PREPARE)
                .add(Player.COMMAND_STOP)
                .add(Player.COMMAND_SET_MEDIA_ITEM)
                .add(Player.COMMAND_CHANGE_MEDIA_ITEMS)
                .add(Player.COMMAND_GET_TIMELINE)
                .add(Player.COMMAND_GET_METADATA)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_MEDIA_ITEM)
                .add(Player.COMMAND_SET_REPEAT_MODE)
                .add(Player.COMMAND_SET_SHUFFLE_MODE)
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(availableSessionCommands)
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
            return Futures.immediateFailedFuture(UnsupportedOperationException("No items to resume"))
        }

        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int
        ): Int {
            if (playerCommand == Player.COMMAND_SEEK_TO_NEXT || 
                playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS ||
                playerCommand == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
                playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM ||
                playerCommand == Player.COMMAND_SET_MEDIA_ITEM ||
                playerCommand == Player.COMMAND_CHANGE_MEDIA_ITEMS ||
                playerCommand == Player.COMMAND_STOP ||
                playerCommand == Player.COMMAND_SEEK_TO_MEDIA_ITEM ||
                playerCommand == Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) {
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

    private fun createPlayer(processor: CrossfadeAudioProcessor, name: String): ExoPlayer {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("JasminePlayer/1.1")
            .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))
            .setAllowCrossProtocolRedirects(true)

        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(context: android.content.Context, enableFloat: Boolean, enableAudioTrack: Boolean): AudioSink {
                return DefaultAudioSink.Builder(context).setAudioProcessors(arrayOf(processor)).build()
            }
        }
        
        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(AudioAttributes.DEFAULT, false)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory))
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
                pushWidgetUpdate()
            }
            
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (!isCrossfading) {
                    val otherPlayer = if (player == playerA) playerB else playerA
                    if (otherPlayer.isPlaying) {
                        otherPlayer.pause()
                        otherPlayer.stop()
                    }
                }
                pushWidgetUpdate()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("JasminePlayer", "[$name] Error: ${error.errorCodeName} (${error.errorCode})", error)
            }

            override fun onPlaybackStateChanged(state: Int) {
                pushWidgetUpdate()
            }

            override fun onMetadata(metadata: Metadata) {
                try {
                    val streamTitle = extractStreamTitleFromMetadata(metadata)
                    if (!streamTitle.isNullOrBlank()) {
                        val fixed = fixEncodingIfNeeded(streamTitle)
                        updateCurrentMediaItemMetadata(player, fixed)
                    }
                } catch (e: Exception) {
                    Log.e("JasminePlayer", "Metadata parsing error", e)
                }
            }
        })
        
        return player
    }

    private fun updateCurrentMediaItemMetadata(player: Player, streamTitle: String) {
        val currentItem = player.currentMediaItem ?: return
        val extras = currentItem.mediaMetadata.extras ?: android.os.Bundle()
        val isRadio = extras.getBoolean("isRadio", false)
        if (!isRadio) return

        val metadataBuilder = currentItem.mediaMetadata.buildUpon()
        if (streamTitle.contains(" - ")) {
            val parts = streamTitle.split(" - ", limit = 2)
            metadataBuilder.setArtist(parts[0].trim())
            metadataBuilder.setTitle(parts[1].trim())
        } else {
            metadataBuilder.setTitle(streamTitle)
            metadataBuilder.setArtist("Radio Stream")
        }
        
        val updatedMetadata = metadataBuilder.build()
        
        if (updatedMetadata.title != currentItem.mediaMetadata.title || 
            updatedMetadata.artist != currentItem.mediaMetadata.artist) {
            
            val updatedItem = currentItem.buildUpon()
                .setMediaMetadata(updatedMetadata)
                .build()
            
            player.replaceMediaItem(player.currentMediaItemIndex, updatedItem)
            pushWidgetUpdate()
        }
    }

    private fun pushWidgetUpdate() {
        val player = currentPlayer ?: return
        val item = player.currentMediaItem ?: return
        
        val title = item.mediaMetadata.title?.toString() ?: "Unknown"
        val artist = item.mediaMetadata.artist?.toString() ?: "Jasmine"
        val isPlaying = player.isPlaying
        val artworkUri = item.mediaMetadata.artworkUri
        
        serviceScope.launch(Dispatchers.Default) {
            var albumArt: Bitmap? = null
            var seedColor: Int? = null
            
            try {
                if (artworkUri != null) {
                    albumArt = withContext(Dispatchers.IO) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, artworkUri))
                            } else {
                                @Suppress("DEPRECATION")
                                MediaStore.Images.Media.getBitmap(contentResolver, artworkUri)
                            }
                        } catch (e: FileNotFoundException) {
                            null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    albumArt?.let { bitmap ->
                        val palette = Palette.from(bitmap.asImageBitmap()).generate()
                        seedColor = palette.dominantSwatch?.rgb
                    }
                }
            } catch (e: Exception) { 
                // Ignore widget update errors
            }

            withContext(Dispatchers.Main) {
                PlayerWidget.updateWidget(
                    context = this@PlaybackService,
                    title = title,
                    artist = artist,
                    isPlaying = isPlaying,
                    albumArt = albumArt,
                    backgroundColor = seedColor
                )
            }
        }
    }

    private fun extractStreamTitleFromMetadata(metadata: Metadata): String? {
        for (i in 0 until metadata.length()) {
            val entry = metadata[i]
            
            if (entry is IcyInfo) {
                return entry.title
            }
            
            val cls = entry?.javaClass ?: continue
            val methodNames = listOf("getStreamTitle", "getTitle", "getText")
            for (m in methodNames) {
                try {
                    val method = cls.getMethod(m)
                    val res = method.invoke(entry)?.toString()
                    if (!res.isNullOrBlank()) return res.trim()
                } catch (_: Throwable) {}
            }
            
            val s = entry.toString()
            val p1 = Regex("""StreamTitle\s*=\s*'([^']*)'""", RegexOption.IGNORE_CASE)
            p1.find(s)?.let { return it.groupValues[1].trim() }
        }
        return null
    }

    private fun fixEncodingIfNeeded(s: String): String {
        val containsCyrillic = s.any { it in '\u0400'..'\u04FF' }
        if (containsCyrillic) return s
        return try {
            val decoded = String(s.toByteArray(Charsets.ISO_8859_1), Charset.forName("CP1251"))
            if (decoded.any { it in '\u0400'..'\u04FF' }) decoded else s
        } catch (e: Throwable) { s }
    }

    private suspend fun checkCrossfadeCondition() {
        val current = currentPlayer ?: return
        if (!current.isPlaying || isCrossfading) return

        val isRadio = current.currentMediaItem?.mediaMetadata?.extras?.getBoolean("isRadio") ?: false
        if (isRadio) return

        if (!isCrossfadeEnabled) return

        val duration = current.duration
        if (duration == C.TIME_UNSET || duration <= 0) return

        val remaining = duration - current.currentPosition
        val isRepeatOne = current.repeatMode == Player.REPEAT_MODE_ONE
        val isRepeatAll = current.repeatMode == Player.REPEAT_MODE_ALL
        val hasNext = current.nextMediaItemIndex != C.INDEX_UNSET || isRepeatOne || isRepeatAll

        if (remaining in 200..crossfadeDurationMs && hasNext) {
            startOverlappingCrossfade(crossfadeDurationMs)
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
        fadeJob = serviceScope.launch(Dispatchers.Default) {
            val steps = 30 
            val interval = (fadeDuration / steps).coerceAtLeast(16)
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
            }
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
