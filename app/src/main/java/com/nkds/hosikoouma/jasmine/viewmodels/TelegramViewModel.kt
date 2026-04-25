package com.nkds.hosikoouma.jasmine.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nkds.hosikoouma.jasmine.data.telegram.TelegramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import javax.inject.Inject

data class TelegramAuthState(
    val state: TdApi.AuthorizationState? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TelegramViewModel @Inject constructor(
    private val repository: TelegramRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(TelegramAuthState())
    val authState = _authState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.authorizationState.collect { state ->
                _authState.value = _authState.value.copy(state = state, isLoading = false)
            }
        }
        
        viewModelScope.launch {
            repository.authErrors.collect { error ->
                _authState.value = _authState.value.copy(error = error.message, isLoading = false)
            }
        }
    }

    fun sendPhoneNumber(phone: String) {
        _authState.value = _authState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = repository.sendPhoneNumberAwait(phone)
            if (result.isFailure) {
                _authState.value = _authState.value.copy(error = result.exceptionOrNull()?.message, isLoading = false)
            }
        }
    }

    fun sendCode(code: String) {
        _authState.value = _authState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = repository.checkAuthenticationCodeAwait(code)
            if (result.isFailure) {
                _authState.value = _authState.value.copy(error = result.exceptionOrNull()?.message, isLoading = false)
            }
        }
    }

    fun sendPassword(password: String) {
        _authState.value = _authState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = repository.checkAuthenticationPasswordAwait(password)
            if (result.isFailure) {
                _authState.value = _authState.value.copy(error = result.exceptionOrNull()?.message, isLoading = false)
            }
        }
    }

    fun logout() {
        repository.logout()
    }
}
