package com.example.snapcalorie.ui.viewmodel

import androidx.lifecycle.*
import com.example.snapcalorie.model.UserRead
import com.example.snapcalorie.repository.AuthRepository
import kotlinx.coroutines.launch
import java.net.UnknownHostException

// Файл: AuthViewModel.kt (тип — Kotlin Class, наследник ViewModel)
class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _user = MutableLiveData<UserRead?>()
    val user: LiveData<UserRead?> = _user

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isRegistrationSuccessful = MutableLiveData<Boolean>()
    val isRegistrationSuccessful: LiveData<Boolean> = _isRegistrationSuccessful

    private val _currentUserEmail = MutableLiveData<String>()
    val currentUserEmail: LiveData<String> = _currentUserEmail

    // Загрузка профиля
    fun loadProfile() {
        viewModelScope.launch {
            loadProfileSuspend()
        }
    }

    // Загрузка профиля (suspend версия)
    private suspend fun loadProfileSuspend() {
        try {
            val profile = repo.getProfile()
            _user.value = profile
            _currentUserEmail.value = profile.email
            _error.value = null
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is UnknownHostException -> "Не удалось подключиться к серверу. Проверьте подключение к интернету."
                else -> e.localizedMessage ?: "Неизвестная ошибка"
            }
            _error.value = errorMessage
            
            // Только очищаем пользователя если это действительно ошибка авторизации
            if (errorMessage.contains("Authentication failed") || 
                errorMessage.contains("401") ||
                errorMessage.contains("Unauthorized")) {
                _user.value = null
                _currentUserEmail.value = null
            }
            // При сетевых ошибках оставляем пользователя, чтобы не сбрасывать состояние
        }
    }

    // Регистрация
    fun register(email: String, password: String) {
        viewModelScope.launch {
            try {
                repo.register(email, password)
                _error.value = null
                _isRegistrationSuccessful.value = true
                _currentUserEmail.value = email
            } catch (e: Exception) {
                _error.value = when (e) {
                    is UnknownHostException -> "Не удалось подключиться к серверу. Проверьте подключение к интернету."
                    else -> e.localizedMessage ?: "Неизвестная ошибка"
                }
                _isRegistrationSuccessful.value = false
                _currentUserEmail.value = null
            }
        }
    }

    // Вход
    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val profile = repo.login(email, password)
                _user.value = profile
                _currentUserEmail.value = email
                _error.value = null
            } catch (e: Exception) {
                _error.value = when (e) {
                    is UnknownHostException -> "Не удалось подключиться к серверу. Проверьте подключение к интернету."
                    else -> e.localizedMessage ?: "Неизвестная ошибка"
                }
                _user.value = null
                _currentUserEmail.value = null
            }
        }
    }

    // Выход
    fun logout() {
        repo.logout()
        _user.value = null
        _error.value = null
        _currentUserEmail.value = null
    }

    // Очистка ошибки
    fun clearError() {
        _error.value = null
    }
}

// Фабрика для ViewModel, чтобы передать в неё AuthRepository
class AuthViewModelFactory(
    private val repo: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
