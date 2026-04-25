package com.nkds.hosikoouma.jasmine.data.telegram

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramClientManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "TelegramClientManager"
        
        init {
            try {
                System.loadLibrary("tdjni")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load TDLib native library", e)
            }
        }
    }

    private val _authorizationState = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authorizationState = _authorizationState.asStateFlow()

    private val _updates = MutableSharedFlow<TdApi.Object>(extraBufferCapacity = 64)
    val updates = _updates.asSharedFlow()

    private val _errors = MutableSharedFlow<TdApi.Error>(extraBufferCapacity = 16)
    val errors = _errors.asSharedFlow()

    private var client: Client? = null
    @Volatile
    private var recreateClientAfterClose = false

    private val updateHandler = Client.ResultHandler { update ->
        if (update is TdApi.Update) {
            when (update) {
                is TdApi.UpdateAuthorizationState -> {
                    onAuthorizationStateUpdated(update.authorizationState)
                }
                is TdApi.UpdateUser -> {
                }
                is TdApi.UpdateFile -> {
                    _updates.tryEmit(update)
                }
                else -> {}
            }
        } else if (update is TdApi.Error) {
            reportTdError(update)
        }
    }

    init {
        initializeClient()
    }

    @Synchronized
    private fun initializeClient() {
        if (client != null) return
        try {
            Client.execute(TdApi.SetLogVerbosityLevel(1))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set TDLib log verbosity", e)
        }

        client = Client.create(updateHandler, null, null)
    }

    private fun onAuthorizationStateUpdated(authState: TdApi.AuthorizationState) {
        _authorizationState.value = authState
        when (authState) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                // База (Auth) в filesDir - не удаляется системой
                val databaseDirectory = File(context.filesDir, "tdlib").absolutePath
                // Файлы (Media) в cacheDir - удаляются при очистке кэша
                val filesDirectory = File(context.cacheDir, "tdlib_files").absolutePath
                
                client?.send(TdApi.SetTdlibParameters(
                    false, // useTestDc
                    databaseDirectory,
                    filesDirectory,
                    null, // databaseEncryptionKey
                    true, // useFileDatabase
                    true, // useChatInfoDatabase
                    true, // useMessageDatabase
                    false, // useSecretChats
                    1858271, // apiId
                    "17a5a5bdb952ae30173c573ac497b366", // apiHash
                    "en", // systemLanguageCode
                    android.os.Build.MODEL, // deviceModel
                    android.os.Build.VERSION.RELEASE, // systemVersion
                    "1.0" // applicationVersion
                ), defaultHandler)
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                Log.d(TAG, "Wait phone number")
            }
            is TdApi.AuthorizationStateWaitCode -> {
                Log.d(TAG, "Wait authentication code")
            }
            is TdApi.AuthorizationStateReady -> {
                Log.d(TAG, "Telegram Client Ready")
            }
            is TdApi.AuthorizationStateClosed -> {
                Log.d(TAG, "Telegram Client Closed")
                client = null
                if (recreateClientAfterClose) {
                    recreateClientAfterClose = false
                    initializeClient()
                }
            }
            else -> {}
        }
    }

    fun sendPhoneNumber(phoneNumber: String) {
        val settings = TdApi.PhoneNumberAuthenticationSettings()
        client?.send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, settings), defaultHandler)
    }

    fun checkAuthenticationCode(code: String) {
        client?.send(TdApi.CheckAuthenticationCode(code), defaultHandler)
    }
    
    fun checkAuthenticationPassword(password: String) {
        client?.send(TdApi.CheckAuthenticationPassword(password), defaultHandler)
    }

    fun logout() {
        recreateClientAfterClose = true
        client?.send(TdApi.LogOut(), defaultHandler)
    }

    fun closeClient(recreate: Boolean = false) {
        recreateClientAfterClose = recreate
        client?.send(TdApi.Close(), defaultHandler)
    }

    suspend fun <T : TdApi.Object> sendRequest(function: TdApi.Function<*>): T = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val localClient = client
        if (localClient != null) {
            localClient.send(function) { result ->
                if (result is TdApi.Error) {
                    reportTdError(result)
                    continuation.resumeWith(
                        Result.failure(
                            TdlibRequestException(
                                code = result.code,
                                rawMessage = result.message
                            )
                        )
                    )
                } else {
                    @Suppress("UNCHECKED_CAST")
                    continuation.resumeWith(Result.success(result as T))
                }
            }
        } else {
            continuation.resumeWith(Result.failure(IllegalStateException("Telegram Client is not initialized")))
        }
    }

    private val defaultHandler = Client.ResultHandler { result ->
        if (result is TdApi.Error) {
            reportTdError(result)
        }
    }

    private fun reportTdError(error: TdApi.Error) {
        _errors.tryEmit(error)
        Log.e(TAG, "TDLib Error: ${error.code} - ${error.message}")
    }

    fun isReady(): Boolean = _authorizationState.value is TdApi.AuthorizationStateReady

    suspend fun awaitReady(timeoutMs: Long = 30_000L): Boolean {
        if (isReady()) return true
        return try {
            withTimeoutOrNull(timeoutMs) {
                authorizationState.first { state ->
                    state is TdApi.AuthorizationStateReady ||
                    state is TdApi.AuthorizationStateClosed
                }
            } is TdApi.AuthorizationStateReady
        } catch (e: Exception) {
            Log.w(TAG, "awaitReady failed: ${e.message}")
            false
        }
    }
}

class TdlibRequestException(
    val code: Int,
    rawMessage: String?
) : Exception(rawMessage ?: "Unknown TDLib error")
