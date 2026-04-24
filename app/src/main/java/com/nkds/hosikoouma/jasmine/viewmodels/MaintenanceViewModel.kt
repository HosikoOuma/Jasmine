package com.nkds.hosikoouma.jasmine.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.data.CoverCacheManager
import com.nkds.hosikoouma.jasmine.data.SettingsRepository
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
    val isClearing: Boolean = false,
    val onRepeatInterval: Int = 7
)

@HiltViewModel
class MaintenanceViewModel @Inject constructor(
    private val coverCacheManager: CoverCacheManager,
    private val settingsRepository: SettingsRepository
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
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(coverCount = count, coverSize = size)
            }
        }
    }

    fun clearCoverCache() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isClearing = true)
            // Выполняем тяжелую операцию на IO потоке
            withContext(Dispatchers.IO) {
                coverCacheManager.clearCache()
            }
            updateStats()
            _state.value = _state.value.copy(isClearing = false)
        }
    }

    fun setOnRepeatInterval(days: Int) {
        viewModelScope.launch {
            settingsRepository.setOnRepeatIntervalDays(days)
        }
    }
}
