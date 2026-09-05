package com.nkds.hosikoouma.jasmine.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.data.CoverCacheManager
import com.nkds.hosikoouma.jasmine.data.SettingsRepository
import com.nkds.hosikoouma.jasmine.data.UpdateRepository
import com.nkds.hosikoouma.jasmine.data.telegram.TelegramCacheManager
import com.nkds.hosikoouma.jasmine.datamodels.GithubRelease
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class MaintenanceState(
    val coverCount: Int = 0,
    val coverSize: Long = 0,
    val telegramCacheSize: Long = 0,
    val isClearingCovers: Boolean = false,
    val isClearingTelegram: Boolean = false,
    val isCheckingForUpdates: Boolean = false,
    val latestRelease: GithubRelease? = null,
    val currentVersion: String = "",
    val updateError: String? = null
)

@HiltViewModel
class MaintenanceViewModel @Inject constructor(
    private val coverCacheManager: CoverCacheManager,
    private val telegramCacheManager: TelegramCacheManager,
    private val settingsRepository: SettingsRepository,
    private val updateRepository: UpdateRepository,
    private val application: android.app.Application
) : ViewModel() {

    private val _state = MutableStateFlow(MaintenanceState())
    val state = _state.asStateFlow()

    init {
        updateStats()
        getCurrentVersion()
    }

    private fun getCurrentVersion() {
        try {
            val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
            _state.value = _state.value.copy(currentVersion = packageInfo.versionName ?: "Unknown")
        } catch (e: Exception) {
            _state.value = _state.value.copy(currentVersion = "Unknown")
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCheckingForUpdates = true, updateError = null)
            val release = updateRepository.getLatestRelease()
            if (release != null) {
                val isNew = updateRepository.isNewerVersion(_state.value.currentVersion, release.tagName)
                _state.value = _state.value.copy(
                    isCheckingForUpdates = false,
                    latestRelease = if (isNew) release else null,
                    updateError = if (!isNew) "UP_TO_DATE" else null
                )
            } else {
                _state.value = _state.value.copy(
                    isCheckingForUpdates = false,
                    updateError = "Failed to check for updates"
                )
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
            _state.value = _state.value.copy(isClearingCovers = true)
            withContext(Dispatchers.IO) {
                coverCacheManager.clearCache()
            }
            updateStats()
            _state.value = _state.value.copy(isClearingCovers = false)
        }
    }

    fun clearTelegramCache() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isClearingTelegram = true)
            withContext(Dispatchers.IO) {
                telegramCacheManager.clearCache()
            }
            updateStats()
            _state.value = _state.value.copy(isClearingTelegram = false)
        }
    }

    fun triggerCrash() {
        throw RuntimeException("Debug crash triggered by user in Maintenance")
    }
}
