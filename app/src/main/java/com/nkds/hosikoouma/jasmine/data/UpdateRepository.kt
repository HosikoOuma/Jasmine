package com.nkds.hosikoouma.jasmine.data

import com.nkds.hosikoouma.jasmine.datamodels.GithubRelease
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val settingsRepository: SettingsRepository
) {
    private val githubApiUrl = "https://api.github.com/repos/HosikoOuma/Jasmine/releases/latest"

    suspend fun getLatestRelease(): GithubRelease? {
        return try {
            // Используем полный URL. Ktor должен игнорировать базовый URL, если передан абсолютный.
            httpClient.get("https://api.github.com/repos/HosikoOuma/Jasmine/releases/latest").body<GithubRelease>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun isNewerVersion(currentVersion: String, latestVersion: String): Boolean {
        // Простая проверка. Можно усложнить для учета семантического версионирования
        // Например: 3.0-beta1 vs 3.0-beta2
        val current = currentVersion.replace("v", "").lowercase()
        val latest = latestVersion.replace("v", "").lowercase()
        
        if (current == latest) return false
        
        // Если это бета-версии, можно попробовать сравнить число в конце
        // Но для начала просто проверим на неравенство, так как обычно тег на GitHub будет v3.0-beta2
        return latest != current
    }
}
