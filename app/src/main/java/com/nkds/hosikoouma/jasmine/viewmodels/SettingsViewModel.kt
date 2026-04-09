package com.nkds.hosikoouma.jasmine.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProgressBarStyle { STANDARD, SOLID, DOTTED, WAVE, NEON }
enum class AppFontFamily { DEFAULT, GOOGLE_SANS, JETBRAINS_MONO }
enum class DarkMode { FOLLOW_SYSTEM, LIGHT, DARK }

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    val isCrossfadeEnabled = repository.isCrossfadeEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val crossfadeDuration = repository.crossfadeDuration.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3000L)
    val minTrackDuration = repository.minTrackDuration.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val defaultSortType = repository.defaultSortType.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "BY_DATE")
    val isDefaultSortReversed = repository.isDefaultSortReversed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val progressBarStyle = repository.progressBarStyle.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "STANDARD")
    val appFontFamily = repository.appFontFamily.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DEFAULT")
    val darkMode = repository.darkMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "FOLLOW_SYSTEM")

    val paletteStyle = repository.paletteStyle.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "TonalSpot")
    val amoledDarkMode = repository.amoledDarkMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val useDynamicColor = repository.useDynamicColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val useAlbumArtColor = repository.useAlbumArtColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val seedColor = repository.seedColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF6750A4.toInt())

    fun setCrossfadeEnabled(enabled: Boolean) = viewModelScope.launch { repository.setCrossfadeEnabled(enabled) }
    fun setCrossfadeDuration(duration: Long) = viewModelScope.launch { repository.setCrossfadeDuration(duration) }
    fun setMinTrackDuration(seconds: Int) = viewModelScope.launch { repository.setMinTrackDuration(seconds) }
    fun setDefaultSortType(sortType: String) = viewModelScope.launch { repository.setDefaultSortType(sortType) }
    fun setDefaultSortReversed(reversed: Boolean) = viewModelScope.launch { repository.setDefaultSortReversed(reversed) }
    fun setProgressBarStyle(style: ProgressBarStyle) = viewModelScope.launch { repository.setProgressBarStyle(style.name) }
    fun setAppFontFamily(fontFamily: AppFontFamily) = viewModelScope.launch { repository.setAppFontFamily(fontFamily.name) }
    fun setDarkMode(mode: DarkMode) = viewModelScope.launch { repository.setDarkMode(mode.name) }

    fun setPaletteStyle(style: String) = viewModelScope.launch { repository.setPaletteStyle(style) }
    fun setAmoledDarkMode(enabled: Boolean) = viewModelScope.launch { repository.setAmoledDarkMode(enabled) }
    fun setUseDynamicColor(enabled: Boolean) = viewModelScope.launch { repository.setUseDynamicColor(enabled) }
    fun setUseAlbumArtColor(enabled: Boolean) = viewModelScope.launch { repository.setUseAlbumArtColor(enabled) }
    fun setSeedColor(color: Int) = viewModelScope.launch { repository.setSeedColor(color) }
}
