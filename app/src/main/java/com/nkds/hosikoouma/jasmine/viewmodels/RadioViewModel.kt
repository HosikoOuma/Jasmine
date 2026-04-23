package com.nkds.hosikoouma.jasmine.viewmodels

import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.data.RadioRepository
import com.nkds.hosikoouma.jasmine.data.RadioStation
import com.nkds.hosikoouma.jasmine.ui.components.ToastData
import com.nkds.hosikoouma.jasmine.ui.components.ToastType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RadioViewModel @Inject constructor(
    application: Application,
    private val repository: RadioRepository
) : AndroidViewModel(application) {
    
    private val _systemVolume = MutableStateFlow(0f)
    val systemVolume = _systemVolume.asStateFlow()
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val stations: StateFlow<List<RadioStation>> = repository.allStations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    private val _appToast = MutableStateFlow<ToastData?>(null)
    val appToast = _appToast.asStateFlow()

    fun showToast(type: ToastType, message: String? = null) {
        _appToast.value = ToastData(track = null, type = type, message = message)
    }

    fun clearToast() {
        _appToast.value = null
    }

    fun addStation(name: String, url: String) {
        if (name.isBlank() || url.isBlank()) return
        
        viewModelScope.launch {
            try {
                repository.addStation(name, url)
                showToast(ToastType.RADIO_ADDED, "Added: $name")
            } catch (e: Exception) {
                _errorEvent.emit("Failed to add station: ${e.message}")
            }
        }
    }

    fun deleteStation(station: RadioStation) {
        viewModelScope.launch {
            try {
                repository.deleteStation(station)
                showToast(ToastType.RADIO_REMOVED, "Deleted: ${station.name}")
            } catch (e: Exception) {
                _errorEvent.emit("Failed to delete station")
            }
        }
    }
    
    fun setSystemVolume(vol: Float) { 
        viewModelScope.launch(Dispatchers.IO) { 
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (vol * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toInt(), 0)
            _systemVolume.value = vol 
        } 
    }
}
