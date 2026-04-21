package com.nkds.hosikoouma.jasmine.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioRepository @Inject constructor(private val radioDao: RadioDao) {

    val allStations: Flow<List<RadioStation>> = radioDao.getAllStations()

    suspend fun addStation(name: String, url: String) {
        radioDao.insertStation(RadioStation(name = name, url = url))
    }

    suspend fun deleteStation(station: RadioStation) {
        radioDao.deleteStation(station)
    }
}
