package com.nkds.hosikoouma.jasmine.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProgressBarStyle {
    STANDARD,
    SOLID,
    DOTTED,
    WAVE,
    NEON
}

enum class PlayerBackgroundStyle {
    STANDARD,
    BLURRED_COVER,
    AURA
}

enum class AppFontFamily {
    DEFAULT,
    GOOGLE_SANS,
    JETBRAINS_MONO
}

enum class DarkMode {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    val isCrossfadeEnabled = repository.isCrossfadeEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val crossfadeDuration = repository.crossfadeDuration.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 3000L
    )

    val minTrackDuration = repository.minTrackDuration.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    val defaultSortType = repository.defaultSortType.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "BY_DATE"
    )

    val isDefaultSortReversed = repository.isDefaultSortReversed.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val progressBarStyle = repository.progressBarStyle.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "STANDARD"
    )

    val playerBackgroundStyle = repository.playerBackgroundStyle.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "STANDARD"
    )

    val appFontFamily = repository.appFontFamily.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "DEFAULT"
    )

    val darkMode = repository.darkMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "FOLLOW_SYSTEM"
    )

    fun setCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setCrossfadeEnabled(enabled) }
    }

    fun setCrossfadeDuration(duration: Long) {
        viewModelScope.launch { repository.setCrossfadeDuration(duration) }
    }

    fun setMinTrackDuration(seconds: Int) {
        viewModelScope.launch { repository.setMinTrackDuration(seconds) }
    }

    fun setDefaultSortType(sortType: String) {
        viewModelScope.launch { repository.setDefaultSortType(sortType) }
    }

    fun setDefaultSortReversed(reversed: Boolean) {
        viewModelScope.launch { repository.setDefaultSortReversed(reversed) }
    }

    fun setProgressBarStyle(style: ProgressBarStyle) {
        viewModelScope.launch { repository.setProgressBarStyle(style.name) }
    }

    fun setPlayerBackgroundStyle(style: PlayerBackgroundStyle) {
        viewModelScope.launch { repository.setPlayerBackgroundStyle(style.name) }
    }

    fun setAppFontFamily(fontFamily: AppFontFamily) {
        viewModelScope.launch { repository.setAppFontFamily(fontFamily.name) }
    }

    fun setDarkMode(mode: DarkMode) {
        viewModelScope.launch { repository.setDarkMode(mode.name) }
    }
}
