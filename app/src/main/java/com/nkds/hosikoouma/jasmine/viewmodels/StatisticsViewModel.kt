package com.nkds.hosikoouma.jasmine.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.core.utils.FormatUtils
import com.nkds.hosikoouma.jasmine.data.StatisticsRepository
import com.nkds.hosikoouma.jasmine.data.TrackRepository
import com.nkds.hosikoouma.jasmine.datamodels.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
    private val trackRepository: TrackRepository
) : ViewModel() {

    val topTracks: StateFlow<List<Pair<Track, Int>>> = combine(
        statisticsRepository.topTracks,
        trackRepository.allTracks
    ) { topList, allTracks ->
        topList.mapNotNull { stat ->
            allTracks.find { it.id == stat.trackId }?.let { it to stat.playCount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTracks: StateFlow<List<Track>> = combine(
        statisticsRepository.recentHistory,
        trackRepository.allTracks
    ) { history, allTracks ->
        history.mapNotNull { event ->
            allTracks.find { it.id == event.trackId }
        }.distinctBy { it.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topArtists: StateFlow<List<Pair<String, Int>>> = topTracks.map { list ->
        list.groupBy { it.first.artist }
            .map { (artist, tracks) -> artist to tracks.sumOf { it.second } }
            .sortedByDescending { it.second }
            .take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalTimeFormatted: StateFlow<String> = statisticsRepository.totalListeningTime
        .map { timeMs ->
            if (timeMs == null || timeMs == 0L) return@map "0m"
            val minutes = (timeMs / (1000 * 60)) % 60
            val hours = timeMs / (1000 * 60 * 60)
            if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0m")
}
