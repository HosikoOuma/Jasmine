package com.nkds.hosikoouma.jasmine

import android.app.PendingIntent
import android.content.Intent
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
import com.nkds.hosikoouma.jasmine.core.CrossfadeManager
import com.nkds.hosikoouma.jasmine.data.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.nio.charset.Charset
import androidx.media3.extractor.metadata.icy.IcyInfo
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    
    private lateinit var playerA: ExoPlayer
    private lateinit var processorA: CrossfadeAudioProcessor
    
    private lateinit var playerB: ExoPlayer
    private lateinit var processorB: CrossfadeAudioProcessor
    
    private lateinit var crossfadeManager: CrossfadeManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var audioManager: AudioManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var playOnFocusGain = false
    private lateinit var focusRequest: AudioFocusRequest

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        
        setupAudioFocus()
        
        processorA = CrossfadeAudioProcessor()
        playerA = createPlayer(processorA, "PlayerA")
        
        processorB = CrossfadeAudioProcessor()
        playerB = createPlayer(processorB, "PlayerB")

        crossfadeManager = CrossfadeManager(
            serviceScope = serviceScope,
            playerA = playerA,
            processorA = processorA,
            playerB = playerB,
            processorB = processorB,
            onPlayerSwapped = { newPlayer ->
                mediaSession?.setPlayer(newPlayer)
            }
        )
        
        observeSettings()
        
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
    }

    private fun observeSettings() {
        serviceScope.launch {
            settingsRepository.isCrossfadeEnabled.collectLatest { crossfadeManager.isEnabled = it }
        }
        serviceScope.launch {
            settingsRepository.crossfadeDuration.collectLatest { crossfadeManager.durationMs = it }
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
                crossfadeManager.cancelActiveCrossfade()
            }
            return super.onPlayerCommandRequest(session, controller, playerCommand)
        }
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
                        crossfadeManager.cancelActiveCrossfade()
                        crossfadeManager.getCurrentPlayer().pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        if (crossfadeManager.getCurrentPlayer().isPlaying) {
                            playOnFocusGain = true
                            crossfadeManager.getCurrentPlayer().pause()
                        }
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        crossfadeManager.getCurrentPlayer().volume = 0.2f
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        crossfadeManager.getCurrentPlayer().volume = 1.0f
                        if (playOnFocusGain) {
                            crossfadeManager.getCurrentPlayer().play()
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
                if (playWhenReady && player == crossfadeManager.getCurrentPlayer()) {
                    requestManualAudioFocus()
                }
                crossfadeManager.scheduleCrossfade()
            }
            
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                crossfadeManager.scheduleCrossfade()
            }

            override fun onPlaybackStateChanged(state: Int) {
                crossfadeManager.scheduleCrossfade()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val currentPlayer = crossfadeManager.getCurrentPlayer()
                if (player == currentPlayer) {
                    val otherPlayer = if (player == playerA) playerB else playerA
                    if (otherPlayer.isPlaying) {
                        otherPlayer.pause()
                        otherPlayer.stop()
                    }
                }
                crossfadeManager.scheduleCrossfade()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("JasminePlayer", "[$name] Error: ${error.errorCodeName} (${error.errorCode})", error)
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

    private fun getAllItems(player: Player): List<MediaItem> {
        return List(player.mediaItemCount) { i -> player.getMediaItemAt(i) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        serviceScope.cancel()
        audioManager.abandonAudioFocusRequest(focusRequest)
        crossfadeManager.release()
        mediaSession?.release()
        super.onDestroy()
    }
}
