package com.nkds.hosikoouma.jasmine.core.di

import com.nkds.hosikoouma.jasmine.data.LrcLibService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
            install(Logging) {
                level = LogLevel.BODY
                logger = Logger.DEFAULT
            }
            defaultRequest {
                url("https://lrclib.net/api/")
                header("User-Agent", "JasmineMusicPlayer/1.0 (https://github.com/hosikoouma/Jasmine)")
            }
        }
    }

    @Provides
    @Singleton
    fun provideLrcLibService(httpClient: HttpClient): LrcLibService {
        return LrcLibService(httpClient)
    }
}
