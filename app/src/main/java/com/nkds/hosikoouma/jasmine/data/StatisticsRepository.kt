package com.nkds.hosikoouma.jasmine.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepository @Inject constructor(
    private val statisticsDao: StatisticsDao
) {
    // Увеличиваем лимит выборки до 50, чтобы ViewModel могла корректно отфильтровать топ-5 треков и артистов
    val recentHistory: Flow<List<PlayHistoryEntity>> = statisticsDao.getRecentHistory(20)
    val topTracks: Flow<List<TrackPlayCount>> = statisticsDao.getTopTracks(50)
    val totalListeningTime: Flow<Long?> = statisticsDao.getTotalListeningTime()

    fun getTopTracksSince(days: Int, limit: Int = 30): Flow<List<TrackPlayCount>> {
        val sinceTimestamp = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        return statisticsDao.getTopTracksSince(sinceTimestamp, limit)
    }

    suspend fun recordPlayback(trackId: Long, durationMs: Long) {
        if (durationMs < 3000) return // Игнорируем меньше 3 секунд
        statisticsDao.insertPlayEvent(PlayHistoryEntity(trackId = trackId, playedDuration = durationMs))
    }

    suspend fun clearHistory() {
        statisticsDao.clearHistory()
    }
}
