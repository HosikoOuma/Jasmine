package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class RadioRepository(context: Context) {
    private val radioDao = PlaylistDatabase.getDatabase(context).radioDao()

    val allStations: Flow<List<RadioStation>> = radioDao.getAllStations()

    suspend fun addStation(name: String, url: String) {
        radioDao.insertStation(RadioStation(name = name, url = url))
    }

    suspend fun deleteStation(station: RadioStation) {
        radioDao.deleteStation(station)
    }
}
