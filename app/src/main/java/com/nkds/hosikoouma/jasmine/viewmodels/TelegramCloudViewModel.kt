package com.nkds.hosikoouma.jasmine.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.data.TelegramChannelEntity
import com.nkds.hosikoouma.jasmine.data.telegram.TelegramRepository
import com.nkds.hosikoouma.jasmine.data.toTrack
import com.nkds.hosikoouma.jasmine.datamodels.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import javax.inject.Inject

data class TelegramCloudState(
    val searchResult: TdApi.Chat? = null,
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val syncingChannels: Set<Long> = emptySet(),
    val myChats: List<TdApi.Chat> = emptyList(),
    val isFetchingChats: Boolean = false
)

@HiltViewModel
class TelegramCloudViewModel @Inject constructor(
    private val repository: TelegramRepository,
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
    
    fun getTracksForChannel(chatId: Long): kotlinx.coroutines.flow.Flow<List<Track>> {
        return telegramDao.getAllTelegramSongs().map { allSongs ->
            allSongs.filter { it.chatId == chatId }.map { it.toTrack() }
        }
    }
    
    fun getChannelTitle(chatId: Long): String {
        return channels.value.find { it.chatId == chatId }?.title ?: "Telegram Channel"
    }
}
