package com.nkds.hosikoouma.jasmine.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.data.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ProgressBarStyle { STANDARD, SOLID, DOTTED, WAVE, NEON }
enum class AppFontFamily { DEFAULT, GOOGLE_SANS, JETBRAINS_MONO, NUNITO }
enum class DarkMode { FOLLOW_SYSTEM, LIGHT, DARK }

data class SettingsState(
    val isCrossfadeEnabled: Boolean = true,
    val crossfadeDuration: Long = 3000L,
    val minTrackDuration: Int = 0,
    val defaultSortType: String = "BY_DATE",
    val isDefaultSortReversed: Boolean = false,
    val progressBarStyle: String = "STANDARD",
    val appFontFamily: String = "DEFAULT",
    val darkMode: String = "FOLLOW_SYSTEM",
    val paletteStyle: String = "TonalSpot",
    val amoledDarkMode: Boolean = false,
    val useDynamicColor: Boolean = true,
    val useAlbumArtColor: Boolean = true,
    val seedColor: Int = 0xFF6750A4.toInt()
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    val settingsState: StateFlow<SettingsState> = combine(
        repository.isCrossfadeEnabled,
        repository.crossfadeDuration,
        repository.minTrackDuration,
        repository.defaultSortType,
        repository.isDefaultSortReversed,
        repository.progressBarStyle,
        repository.appFontFamily,
        repository.darkMode,
        repository.paletteStyle,
        repository.amoledDarkMode,
        repository.useDynamicColor,
        repository.useAlbumArtColor,
        repository.seedColor
    ) { args: Array<Any> ->
        SettingsState(
            isCrossfadeEnabled = args[0] as Boolean,
            crossfadeDuration = args[1] as Long,
            minTrackDuration = args[2] as Int,
            defaultSortType = args[3] as String,
            isDefaultSortReversed = args[4] as Boolean,
            progressBarStyle = args[5] as String,
            appFontFamily = args[6] as String,
            darkMode = args[7] as String,
            paletteStyle = args[8] as String,
            amoledDarkMode = args[9] as Boolean,
            useDynamicColor = args[10] as Boolean,
            useAlbumArtColor = args[11] as Boolean,
            seedColor = args[12] as Int
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState()
    )

    // Individual setters
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
