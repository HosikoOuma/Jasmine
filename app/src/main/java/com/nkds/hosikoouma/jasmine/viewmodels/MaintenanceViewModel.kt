package com.nkds.hosikoouma.jasmine.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.data.CoverCacheManager
import com.nkds.hosikoouma.jasmine.data.SettingsRepository
import com.nkds.hosikoouma.jasmine.data.StatisticsRepository
import com.nkds.hosikoouma.jasmine.data.telegram.TelegramCacheManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class MaintenanceState(
    val coverCount: Int = 0,
    val coverSize: Long = 0,
    val telegramCacheSize: Long = 0,
    val isClearing: Boolean = false,
    val isRefreshingOnRepeat: Boolean = false,
    val onRepeatInterval: Int = 7
)

@HiltViewModel
class MaintenanceViewModel @Inject constructor(
    private val coverCacheManager: CoverCacheManager,
    private val telegramCacheManager: TelegramCacheManager,
    private val settingsRepository: SettingsRepository,
    private val statisticsRepository: StatisticsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MaintenanceState())
    val state = _state.asStateFlow()

    init {
        updateStats()
        viewModelScope.launch {
            settingsRepository.onRepeatIntervalDays.collect { days ->
                _state.value = _state.value.copy(onRepeatInterval = days)
            }
        }
    }

    fun updateStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val (count, size) = coverCacheManager.getCacheInfo()
            val tgSize = telegramCacheManager.getCacheSize()
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(
                    coverCount = count, 
                    coverSize = size,
                    telegramCacheSize = tgSize
                )
            }
        }
    }

    fun clearCoverCache() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isClearing = true)
            withContext(Dispatchers.IO) {
                coverCacheManager.clearCache()
            }
            updateStats()
            _state.value = _state.value.copy(isClearing = false)
        }
    }

    fun clearTelegramCache() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isClearing = true)
            withContext(Dispatchers.IO) {
                telegramCacheManager.clearCache()
            }
            updateStats()
            _state.value = _state.value.copy(isClearing = false)
        }
    }

    fun forceRefreshOnRepeat() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshingOnRepeat = true)
            val days = settingsRepository.onRepeatIntervalDays.first()
            val topTracks = statisticsRepository.getTopTracksSince(days, limit = 30).first()
            val trackIds = topTracks.map { it.trackId }
            settingsRepository.saveOnRepeatTracks(trackIds)
            _state.value = _state.value.copy(isRefreshingOnRepeat = false)
        }
    }

    fun setOnRepeatInterval(days: Int) {
        viewModelScope.launch {
            settingsRepository.setOnRepeatIntervalDays(days)
        }
    }
}
