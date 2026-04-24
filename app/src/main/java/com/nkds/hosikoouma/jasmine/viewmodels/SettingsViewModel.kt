package com.nkds.hosikoouma.jasmine.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.core.models.AppFontFamily
import com.nkds.hosikoouma.jasmine.core.models.DarkMode
import com.nkds.hosikoouma.jasmine.core.models.ProgressBarStyle
import com.nkds.hosikoouma.jasmine.core.models.SortType
import com.nkds.hosikoouma.jasmine.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val isCrossfadeEnabled: Boolean = true,
    val crossfadeDuration: Long = 3000L,
    val minTrackDuration: Int = 0,
    val defaultSortType: SortType = SortType.BY_DATE,
    val isDefaultSortReversed: Boolean = false,
    val progressBarStyle: ProgressBarStyle = ProgressBarStyle.STANDARD,
    val appFontFamily: AppFontFamily = AppFontFamily.DEFAULT,
    val darkMode: DarkMode = DarkMode.FOLLOW_SYSTEM,
    val paletteStyle: String = "TonalSpot",
    val amoledDarkMode: Boolean = false,
    val useDynamicColor: Boolean = true,
    val useAlbumArtColor: Boolean = true,
    val seedColor: Int = 0xFF6750A4.toInt(),
    val navigationItems: List<String> = listOf("tracks", "radio", "library", "settings"),
    val playerControlsOrder: List<String> = listOf("shuffle", "previous", "play_pause", "next", "repeat"),
    val manageAudioFocus: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val repository: SettingsRepository
) : AndroidViewModel(application) {

    val settingsState: StateFlow<SettingsState> = combine(
        repository.isCrossfadeEnabled,
        repository.crossfadeDuration,
        repository.minTrackDuration,
        repository.defaultSortType,
        repository.isDefaultSortReversed,
        repository.progressBarStyle,
        repository.appFontFamily,
        repository.darkMode,
        repository.paletteStyle,
        repository.amoledDarkMode,
        repository.useDynamicColor,
        repository.useAlbumArtColor,
        repository.seedColor,
        repository.navigationItems,
        repository.playerControlsOrder,
        repository.manageAudioFocus
    ) { args ->
        SettingsState(
            isCrossfadeEnabled = args[0] as Boolean,
            crossfadeDuration = args[1] as Long,
            minTrackDuration = args[2] as Int,
            defaultSortType = safeValueOf(args[3] as String, SortType.BY_DATE),
            isDefaultSortReversed = args[4] as Boolean,
            progressBarStyle = safeValueOf(args[5] as String, ProgressBarStyle.STANDARD),
            appFontFamily = safeValueOf(args[6] as String, AppFontFamily.DEFAULT),
            darkMode = safeValueOf(args[7] as String, DarkMode.FOLLOW_SYSTEM),
            paletteStyle = args[8] as String,
            amoledDarkMode = args[9] as Boolean,
            useDynamicColor = args[10] as Boolean,
            useAlbumArtColor = args[11] as Boolean,
            seedColor = args[12] as Int,
            navigationItems = (args[13] as String).split(",").filter { it.isNotBlank() },
            playerControlsOrder = (args[14] as String).split(",").filter { it.isNotBlank() },
            manageAudioFocus = args[15] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState()
    )

    // Individual setters with proper typing
    fun setCrossfadeEnabled(enabled: Boolean) = launchUpdate { repository.setCrossfadeEnabled(enabled) }
    fun setCrossfadeDuration(duration: Long) = launchUpdate { repository.setCrossfadeDuration(duration) }
    fun setMinTrackDuration(seconds: Int) = launchUpdate { repository.setMinTrackDuration(seconds) }
    fun setDefaultSortType(sortType: SortType) = launchUpdate { repository.setDefaultSortType(sortType.name) }
    fun setDefaultSortReversed(reversed: Boolean) = launchUpdate { repository.setDefaultSortReversed(reversed) }
    fun setProgressBarStyle(style: ProgressBarStyle) = launchUpdate { repository.setProgressBarStyle(style.name) }
    fun setAppFontFamily(fontFamily: AppFontFamily) = launchUpdate { repository.setAppFontFamily(fontFamily.name) }
    fun setDarkMode(mode: DarkMode) = launchUpdate { repository.setDarkMode(mode.name) }
    fun setPaletteStyle(style: String) = launchUpdate { repository.setPaletteStyle(style) }
    fun setAmoledDarkMode(enabled: Boolean) = launchUpdate { repository.setAmoledDarkMode(enabled) }
    fun setUseDynamicColor(enabled: Boolean) = launchUpdate { repository.setUseDynamicColor(enabled) }
    fun setUseAlbumArtColor(enabled: Boolean) = launchUpdate { repository.setUseAlbumArtColor(enabled) }
    fun setSeedColor(color: Int) = launchUpdate { repository.setSeedColor(color) }
    
    fun setNavigationItems(items: List<String>) = launchUpdate { 
        repository.setNavigationItems(items.joinToString(",")) 
    }
    
    fun setPlayerControlsOrder(order: List<String>) = launchUpdate {
        repository.setPlayerControlsOrder(order.joinToString(","))
    }

    fun setManageAudioFocus(enabled: Boolean) = launchUpdate { repository.setManageAudioFocus(enabled) }

    private fun launchUpdate(block: suspend () -> Unit) = viewModelScope.launch { block() }

    companion object {
        private inline fun <reified T : Enum<T>> safeValueOf(name: String, fallback: T): T {
            return try { enumValueOf<T>(name) } catch (e: Exception) { fallback }
        }
    }
}
