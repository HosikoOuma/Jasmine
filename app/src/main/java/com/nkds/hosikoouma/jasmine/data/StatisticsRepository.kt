package com.nkds.hosikoouma.jasmine.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepository @Inject constructor(
    private val statisticsDao: StatisticsDao
) {
    val recentHistory: Flow<List<PlayHistoryEntity>> = statisticsDao.getRecentHistory(5)
    val topTracks: Flow<List<TrackPlayCount>> = statisticsDao.getTopTracks(5)
    val totalListeningTime: Flow<Long?> = statisticsDao.getTotalListeningTime()

    suspend fun recordPlayback(trackId: Long, durationMs: Long) {
        if (durationMs < 3000) return // Игнорируем меньше 3 секунд (как в Rhythm)
        statisticsDao.insertPlayEvent(PlayHistoryEntity(trackId = trackId, playedDuration = durationMs))
    }

    suspend fun clearHistory() {
        statisticsDao.clearHistory()
    }
}
