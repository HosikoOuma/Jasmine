package com.nkds.hosikoouma.jasmine.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    fun setCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setCrossfadeEnabled(enabled)
        }
    }

    fun setCrossfadeDuration(duration: Long) {
        viewModelScope.launch {
            repository.setCrossfadeDuration(duration)
        }
    }

    fun setMinTrackDuration(seconds: Int) {
        viewModelScope.launch {
            repository.setMinTrackDuration(seconds)
        }
    }
}
