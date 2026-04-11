package com.nkds.hosikoouma.jasmine.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RadioDao {
    @Query("SELECT * FROM radio_stations")
    fun getAllStations(): Flow<List<RadioStation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: RadioStation)

    @Delete
    suspend fun deleteStation(station: RadioStation)
}
