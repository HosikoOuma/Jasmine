package com.nkds.hosikoouma.jasmine

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes as AndroidAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
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
import com.nkds.hosikoouma.jasmine.data.PlaylistDao
import com.nkds.hosikoouma.jasmine.data.QueueTrackEntity
import com.nkds.hosikoouma.jasmine.data.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import java.nio.charset.Charset
import androidx.media3.extractor.metadata.icy.IcyInfo
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import android.graphics.Bitmap
import android.graphics.Canvas
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    
    private lateinit var playerA: Player
    private lateinit var processorA: CrossfadeAudioProcessor
    
    private lateinit var playerB: Player
    private lateinit var processorB: CrossfadeAudioProcessor
    
    private lateinit var crossfadeManager: CrossfadeManager
    
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var playlistDao: PlaylistDao
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var audioManager: AudioManager? = null
    private var playOnFocusGain = false
    private lateinit var focusRequest: AudioFocusRequest
    private var manageAudioFocus = true
    
    private var saveStateJob: Job? = null
    private var saveQueueJob: Job? = null

    private val placeholderArtwork: ByteArray by lazy {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val drawable = androidx.core.content.res.ResourcesCompat.getDrawable(resources, R.drawable.ison_vec, null)
        drawable?.setBounds(0, 0, size, size)
        drawable?.draw(canvas)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.toByteArray()
    }

    override fun onCreate() {
        super.onCreate()
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

        restoreQueueToPlayer()
    }

    private fun createPlayer(processor: CrossfadeAudioProcessor, name: String): Player {
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(context: android.content.Context, enableFloat: Boolean, enableAudioTrack: Boolean): AudioSink {
                return DefaultAudioSink.Builder(context).setAudioProcessors(arrayOf(processor)).build()
            }
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        
        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, false) // Мы управляем фокусом вручную для кроссфейда
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        val forwardingPlayer = ResumingForwardingPlayer(player)

        forwardingPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (player == crossfadeManager.getCurrentPlayer()) {
                    val otherPlayer = if (player == playerA) playerB else playerA
                    if (otherPlayer.isPlaying && !crossfadeManager.isCrossfading) {
                        otherPlayer.pause()
                    }
                    saveCurrentState(immediate = true)
                }
                crossfadeManager.scheduleCrossfade()
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (player == crossfadeManager.getCurrentPlayer()) {
                    saveQueueJob?.cancel()
                    saveQueueJob = serviceScope.launch {
                        delay(500)
                        saveQueueToDb(player)
                    }
                }
            }
            
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady && player == crossfadeManager.getCurrentPlayer()) {
                    handleFocusOnPlay()
                }
                if (!playWhenReady) saveCurrentState(immediate = true)
                crossfadeManager.scheduleCrossfade()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                crossfadeManager.scheduleCrossfade()
            }

            override fun onPlaybackStateChanged(state: Int) {
                crossfadeManager.scheduleCrossfade()
            }

            override fun onPositionDiscontinuity(old: Player.PositionInfo, new: Player.PositionInfo, reason: Int) {
                if (reason != Player.DISCONTINUITY_REASON_SEEK) saveCurrentState()
                crossfadeManager.scheduleCrossfade()
            }

            override fun onMetadata(metadata: Metadata) {
                val title = extractStreamTitleFromMetadata(metadata)
                if (!title.isNullOrBlank()) updateCurrentMediaItemMetadata(player, fixEncodingIfNeeded(title))
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                if (player == crossfadeManager.getCurrentPlayer()) {
                    val isRadio = player.currentMediaItem?.mediaMetadata?.extras?.getBoolean("isRadio", false) ?: false
                    // Сохраняем режим повтора только если это НЕ радио
                    if (!isRadio) {
                        serviceScope.launch { settingsRepository.setRepeatMode(repeatMode) }
                    }
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                if (player == crossfadeManager.getCurrentPlayer()) {
                    val isRadio = player.currentMediaItem?.mediaMetadata?.extras?.getBoolean("isRadio", false) ?: false
                    // Перемешивание для радио тоже не имеет смысла сохранять в пользовательские настройки
                    if (!isRadio) {
                        serviceScope.launch { settingsRepository.setShuffleModeEnabled(shuffleModeEnabled) }
                    }
                }
            }
        })
        
        return forwardingPlayer
    }

    private fun restoreQueueToPlayer() {
        serviceScope.launch {
            try {
                val queueEntities = playlistDao.getCurrentQueue()
                if (queueEntities.isNotEmpty()) {
                    val lastIndex = settingsRepository.lastMediaItemIndex.first()
                    val lastPos = settingsRepository.lastPlaybackPosition.first()
                    val shuffleEnabled = settingsRepository.shuffleModeEnabled.first()
                    val repeatMode = settingsRepository.repeatMode.first()
                    
                    val mediaItems = queueEntities.map { entityToMediaItem(it) }
                    withContext(Dispatchers.Main) {
                        val index = if (lastIndex in mediaItems.indices) lastIndex else 0
                        
                        // Сначала устанавливаем режимы
                        playerA.shuffleModeEnabled = shuffleEnabled
                        playerA.repeatMode = repeatMode
                        playerB.shuffleModeEnabled = shuffleEnabled
                        playerB.repeatMode = repeatMode
                        
                        playerA.setMediaItems(mediaItems, index, lastPos)
                        playerA.prepare()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun observeSettings() {
        serviceScope.launch { settingsRepository.isCrossfadeEnabled.collectLatest { crossfadeManager.isEnabled = it } }
        serviceScope.launch { settingsRepository.crossfadeDuration.collectLatest { crossfadeManager.durationMs = it } }
        serviceScope.launch { settingsRepository.manageAudioFocus.collectLatest { manageAudioFocus = it } }
    }

    private fun saveCurrentState(immediate: Boolean = false) {
        val player = crossfadeManager.getCurrentPlayer()
        val index = player.currentMediaItemIndex
        val pos = player.currentPosition
        val isRadio = player.currentMediaItem?.mediaMetadata?.extras?.getBoolean("isRadio", false) ?: false
        
        saveStateJob?.cancel()
        saveStateJob = serviceScope.launch {
            if (!immediate) delay(1000)
            settingsRepository.savePlayerState(index, pos, isRadio)
        }
    }

    private fun saveQueueToDb(player: Player) {
        val items = List(player.mediaItemCount) { player.getMediaItemAt(it) }
        serviceScope.launch(Dispatchers.IO) { 
            playlistDao.updateQueue(items.mapIndexed { i, m -> mediaItemToEntity(m, i) }) 
        }
    }

    private fun mediaItemToEntity(item: MediaItem, index: Int): QueueTrackEntity {
        val meta = item.mediaMetadata
        val extras = meta.extras ?: android.os.Bundle()
        val artworkUri = if (meta.artworkUri?.toString()?.contains("drawable/ison_vec") == true) null else meta.artworkUri?.toString()
        return QueueTrackEntity(
            trackId = try { (item.mediaId.split("_").firstOrNull() ?: "0").toLong() } catch (_: Exception) { 0L },
            title = meta.title?.toString() ?: "Unknown",
            artist = meta.artist?.toString() ?: "Unknown",
            album = meta.albumTitle?.toString() ?: "Unknown",
            duration = extras.getLong("duration", 0L),
            contentUri = item.localConfiguration?.uri?.toString() ?: "",
            albumArtUri = artworkUri,
            path = extras.getString("path", ""),
            isManual = extras.getBoolean("isManual", false),
            sourceName = extras.getString("sourceName"),
            orderIndex = index
        )
    }

    private fun entityToMediaItem(entity: QueueTrackEntity): MediaItem {
        val extras = android.os.Bundle().apply {
            putLong("duration", entity.duration)
            putString("path", entity.path)
            putBoolean("isManual", entity.isManual)
            putBoolean("isRadio", false)
            putString("sourceName", entity.sourceName)
        }
        
        val metaBuilder = MediaMetadata.Builder()
            .setTitle(entity.title)
            .setArtist(entity.artist)
            .setAlbumTitle(entity.album)
            .setExtras(extras)

        val artworkUri = entity.albumArtUri?.let { Uri.parse(it) }
        if (artworkUri != null) {
            metaBuilder.setArtworkUri(artworkUri)
        } else {
            // Force update for OS by providing non-null artwork data (1x1 transparent pixel)
            metaBuilder.setArtworkData(placeholderArtwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }
            
        return MediaItem.Builder()
            .setMediaId("${entity.trackId}_${UUID.randomUUID()}")
            .setUri(entity.contentUri)
            .setMediaMetadata(metaBuilder.build())
            .build()
    }

    private fun updateCurrentMediaItemMetadata(player: Player, title: String) {
        val item = player.currentMediaItem ?: return
        if (!(item.mediaMetadata.extras?.getBoolean("isRadio", false) ?: false)) return
        val meta = item.mediaMetadata.buildUpon()
        if (title.contains(" - ")) {
            val parts = title.split(" - ", limit = 2)
            meta.setArtist(parts[0].trim()).setTitle(parts[1].trim())
        } else {
            meta.setTitle(title).setArtist("Radio Stream")
        }
        
        // Ensure radio also has a placeholder to force notification update
        if (meta.build().artworkUri == null && meta.build().artworkData == null) {
            meta.setArtworkData(placeholderArtwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }

        if (meta.build().title != item.mediaMetadata.title || 
            meta.build().artworkUri != item.mediaMetadata.artworkUri ||
            meta.build().artworkData != item.mediaMetadata.artworkData) {
            player.replaceMediaItem(player.currentMediaItemIndex, item.buildUpon().setMediaMetadata(meta.build()).build())
        }
    }

    private fun extractStreamTitleFromMetadata(metadata: Metadata): String? {
        for (i in 0 until metadata.length()) {
            val entry = metadata[i]
            if (entry is IcyInfo) return entry.title
            val cls = entry?.javaClass ?: continue
            for (m in listOf("getStreamTitle", "getTitle", "getText")) {
                try {
                    val res = cls.getMethod(m).invoke(entry)?.toString()
                    if (!res.isNullOrBlank()) return res.trim()
                } catch (_: Throwable) {}
            }
        }
        return null
    }

    private fun fixEncodingIfNeeded(s: String): String {
        if (s.any { it in '\u0400'..'\u04FF' }) return s
        return try {
            val d = String(s.toByteArray(Charsets.ISO_8859_1), Charset.forName("CP1251"))
            if (d.any { it in '\u0400'..'\u04FF' }) d else s
        } catch (_: Throwable) { s }
    }

    private fun handleFocusOnPlay() {
        if (!manageAudioFocus) return
        
        when (requestManualAudioFocus()) {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                playerA.playWhenReady = false
                playerB.playWhenReady = false
            }
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                playerA.playWhenReady = false
                playerB.playWhenReady = false
                playOnFocusGain = true
            }
        }
    }

    private fun setupAudioFocus() {
        val attr = AndroidAudioAttributes.Builder()
            .setUsage(AndroidAudioAttributes.USAGE_MEDIA)
            .setContentType(AndroidAudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
            
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attr)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener { focusChange ->
                if (!manageAudioFocus) return@setOnAudioFocusChangeListener
                
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        crossfadeManager.cancelActiveCrossfade()
                        playerA.pause()
                        playerB.pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        if (playerA.isPlaying || playerB.isPlaying) {
                            playOnFocusGain = true
                        }
                        playerA.pause()
                        playerB.pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        playerA.volume = 0.2f
                        playerB.volume = 0.2f
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        playerA.volume = 1.0f
                        playerB.volume = 1.0f
                        if (playOnFocusGain) {
                            crossfadeManager.getCurrentPlayer().play()
                            playOnFocusGain = false
                        }
                    }
                }
            }.build()
    }

    private fun requestManualAudioFocus(): Int {
        if (!manageAudioFocus) return AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return audioManager?.requestAudioFocus(focusRequest) ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
    }

    override fun onGetSession(info: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        saveStateJob?.cancel()
        saveQueueJob?.cancel()
        serviceScope.cancel()
        audioManager?.abandonAudioFocusRequest(focusRequest)
        crossfadeManager.release()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onConnect(s: MediaSession, c: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val pCmds = s.player.availableCommands.buildUpon()
                .add(Player.COMMAND_PLAY_PAUSE).add(Player.COMMAND_PREPARE).add(Player.COMMAND_STOP)
                .add(Player.COMMAND_SET_MEDIA_ITEM).add(Player.COMMAND_CHANGE_MEDIA_ITEMS)
                .add(Player.COMMAND_GET_TIMELINE).add(Player.COMMAND_GET_METADATA)
                .add(Player.COMMAND_SEEK_TO_NEXT).add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM).add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM).add(Player.COMMAND_SEEK_TO_MEDIA_ITEM)
                .add(Player.COMMAND_SET_REPEAT_MODE).add(Player.COMMAND_SET_SHUFFLE_MODE).build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(s).setAvailablePlayerCommands(pCmds).build()
        }

        override fun onPlaybackResumption(s: MediaSession, c: MediaSession.ControllerInfo): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val p = s.player
            if (p.mediaItemCount > 0) return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(List(p.mediaItemCount) { p.getMediaItemAt(it) }, p.currentMediaItemIndex, p.currentPosition))
            val setter = SettableFuture<MediaSession.MediaItemsWithStartPosition>()
            serviceScope.launch {
                try {
                    val q = playlistDao.getCurrentQueue()
                    val idx = settingsRepository.lastMediaItemIndex.first()
                    val pos = settingsRepository.lastPlaybackPosition.first()
                    if (q.isNotEmpty()) {
                        val items = q.map { entityToMediaItem(it) }
                        withContext(Dispatchers.Main) { setter.set(MediaSession.MediaItemsWithStartPosition(items, if (idx in items.indices) idx else 0, pos)) }
                    } else withContext(Dispatchers.Main) { setter.setException(UnsupportedOperationException()) }
                } catch (e: Exception) { withContext(Dispatchers.Main) { setter.setException(e) } }
            }
            return setter
        }

        override fun onPlayerCommandRequest(s: MediaSession, c: MediaSession.ControllerInfo, cmd: Int): Int {
            if (cmd in listOf(Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_STOP, Player.COMMAND_SET_MEDIA_ITEM)) crossfadeManager.cancelActiveCrossfade()
            return super.onPlayerCommandRequest(s, c, cmd)
        }
    }

    private inner class ResumingForwardingPlayer(player: Player) : ForwardingPlayer(player) {
        override fun play() {
            handleFocusOnPlay()
            super.play()
        }

        override fun setPlayWhenReady(playWhenReady: Boolean) {
            if (playWhenReady) handleFocusOnPlay()
            super.setPlayWhenReady(playWhenReady)
        }

        override fun seekToNext() {
            if (!playWhenReady) play()
            super.seekToNext()
        }

        override fun seekToNextMediaItem() {
            if (!playWhenReady) play()
            super.seekToNextMediaItem()
        }

        override fun seekToPrevious() {
            if (!playWhenReady) play()
            super.seekToPrevious()
        }

        override fun seekToPreviousMediaItem() {
            if (!playWhenReady) play()
            super.seekToPreviousMediaItem()
        }
    }

    private class SettableFuture<T> : com.google.common.util.concurrent.AbstractFuture<T>() {
        public override fun set(v: T?): Boolean = super.set(v)
        public override fun setException(t: Throwable): Boolean = super.setException(t)
    }
}
