package com.nkds.hosikoouma.jasmine.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.data.PlaylistDatabase
import com.nkds.hosikoouma.jasmine.data.RadioStation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RadioViewModel(application: Application) : AndroidViewModel(application) {
    private val radioDao = PlaylistDatabase.getDatabase(application).radioDao()

    val stations: StateFlow<List<RadioStation>> = radioDao.getAllStations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addStation(name: String, url: String) {
        viewModelScope.launch {
            radioDao.insertStation(RadioStation(name = name, url = url))
        }
    }

    fun deleteStation(station: RadioStation) {
        viewModelScope.launch {
            radioDao.deleteStation(station)
        }
    }
}
