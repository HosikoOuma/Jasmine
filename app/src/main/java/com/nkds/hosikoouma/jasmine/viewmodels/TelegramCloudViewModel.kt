package com.nkds.hosikoouma.jasmine.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.core.models.SortType
import com.nkds.hosikoouma.jasmine.data.TelegramChannelEntity
import com.nkds.hosikoouma.jasmine.data.telegram.TelegramRepository
import com.nkds.hosikoouma.jasmine.data.telegram.TelegramDownloadManager
import com.nkds.hosikoouma.jasmine.data.toTrack
import com.nkds.hosikoouma.jasmine.datamodels.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import javax.inject.Inject

data class TelegramCloudState(
    val searchResult: TdApi.Chat? = null,
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val syncingChannels: Set<Long> = emptySet(),
    val myChats: List<TdApi.Chat> = emptyList(),
    val isFetchingChats: Boolean = false,
    val searchQuery: String = "",
    val sortType: SortType = SortType.BY_DATE,
    val isReversed: Boolean = false
)

@HiltViewModel
class TelegramCloudViewModel @Inject constructor(
    private val repository: TelegramRepository,
    private val downloadManager: TelegramDownloadManager,
    private val telegramDao: com.nkds.hosikoouma.jasmine.data.TelegramDao
) : ViewModel() {

    private val _state = MutableStateFlow(TelegramCloudState())
    val state = _state.asStateFlow()

    val channels: StateFlow<List<TelegramChannelEntity>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun searchChannel(username: String) {
        val cleanUsername = username.removePrefix("@").trim()
        if (cleanUsername.isEmpty()) return

        _state.value = _state.value.copy(isSearching = true, searchError = null, searchResult = null)
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val chat = repository.searchPublicChat(cleanUsername)
            
            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime < 1000) delay(1000 - elapsedTime)

            if (chat != null) {
                _state.value = _state.value.copy(searchResult = chat, isSearching = false)
            } else {
                _state.value = _state.value.copy(searchError = "Channel not found", isSearching = false)
            }
        }
    }

    fun loadMyChats() {
        _state.value = _state.value.copy(isFetchingChats = true)
        viewModelScope.launch {
            val chats = repository.getMyChats()
            _state.value = _state.value.copy(myChats = chats, isFetchingChats = false)
        }
    }

    fun searchMyChats(query: String) {
        if (query.isBlank()) {
            loadMyChats()
            return
        }
        _state.value = _state.value.copy(isFetchingChats = true)
        viewModelScope.launch {
            val chats = repository.searchMyChats(query)
            _state.value = _state.value.copy(myChats = chats, isFetchingChats = false)
        }
    }

    fun addChannel(chat: TdApi.Chat) {
        viewModelScope.launch {
            _state.value = _state.value.copy(syncingChannels = _state.value.syncingChannels + chat.id)
            val startTime = System.currentTimeMillis()
            
            repository.addChannel(chat)
            
            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime < 1000) delay(1000 - elapsedTime)

            _state.value = _state.value.copy(
                searchResult = null,
                syncingChannels = _state.value.syncingChannels - chat.id
            )
        }
    }

    fun removeChannel(chatId: Long) {
        viewModelScope.launch {
            repository.removeChannel(chatId)
        }
    }

    fun syncChannel(chatId: Long) {
        viewModelScope.launch {
            if (_state.value.syncingChannels.contains(chatId)) return@launch
            
            _state.value = _state.value.copy(syncingChannels = _state.value.syncingChannels + chatId)
            val startTime = System.currentTimeMillis()
            
            repository.syncChannel(chatId)
            
            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime < 1000) delay(1000 - elapsedTime)

            _state.value = _state.value.copy(syncingChannels = _state.value.syncingChannels - chatId)
        }
    }

    fun setSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun setSortType(type: SortType) {
        _state.value = _state.value.copy(sortType = type)
    }

    fun toggleReverse() {
        _state.value = _state.value.copy(isReversed = !_state.value.isReversed)
    }

    fun downloadTracks(tracks: List<Track>) {
        val songIds = tracks.map { it.uid }
        downloadManager.downloadTracks(songIds)
    }
    
    fun getTracksForChannel(chatId: Long): Flow<List<Track>> {
        return combine(
            telegramDao.getSongsByChatIdFlow(chatId),
            _state.map { it.searchQuery }.distinctUntilChanged(),
            _state.map { it.sortType }.distinctUntilChanged(),
            _state.map { it.isReversed }.distinctUntilChanged()
        ) { channelSongs, query, sort, reversed ->
            withContext(Dispatchers.Default) {
                channelSongs.asSequence()
                    .map { it.toTrack() }
                    .filter { query.isBlank() || it.title.contains(query, true) || it.artist.contains(query, true) }
                    .let { seq ->
                        when (sort) {
                            SortType.BY_TITLE -> seq.sortedBy { it.title.lowercase() }
                            SortType.BY_ARTIST -> seq.sortedBy { it.artist.lowercase() }
                            SortType.BY_DATE -> seq.sortedByDescending { it.dateModified }
                            SortType.BY_DURATION -> seq.sortedBy { it.duration }
                        }
                    }
                    .let { if (reversed) it.toList().reversed() else it.toList() }
            }
        }
    }
    
    fun getChannelTitle(chatId: Long): String {
        return channels.value.find { it.chatId == chatId }?.title ?: "Telegram Channel"
    }
}
