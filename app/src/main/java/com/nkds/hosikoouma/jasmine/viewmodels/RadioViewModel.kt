package com.nkds.hosikoouma.jasmine.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.data.RadioRepository
import com.nkds.hosikoouma.jasmine.data.RadioStation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RadioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RadioRepository(application)

    val stations: StateFlow<List<RadioStation>> = repository.allStations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    fun addStation(name: String, url: String) {
        if (name.isBlank() || url.isBlank()) return
        
        viewModelScope.launch {
            try {
                repository.addStation(name, url)
            } catch (e: Exception) {
                _errorEvent.emit("Failed to add station: ${e.message}")
            }
        }
    }

    fun deleteStation(station: RadioStation) {
        viewModelScope.launch {
            try {
                repository.deleteStation(station)
            } catch (e: Exception) {
                _errorEvent.emit("Failed to delete station")
            }
        }
    }
}
