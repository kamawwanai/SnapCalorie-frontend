package com.example.snapcalorie.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.snapcalorie.model.OnboardingPlanData
import com.example.snapcalorie.model.OnboardingPlanResponse
import com.example.snapcalorie.model.Profile
import com.example.snapcalorie.repository.OnboardingPlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class OnboardingPlanViewModel(
    private val repository: OnboardingPlanRepository
) : ViewModel() {

    private val _isOnboardingCompleted = MutableStateFlow<Boolean?>(null)
    val isOnboardingCompleted: StateFlow<Boolean?> = _isOnboardingCompleted

    private val _onboardingResponse = MutableStateFlow<OnboardingPlanResponse?>(null)
    val onboardingResponse: StateFlow<OnboardingPlanResponse?> = _onboardingResponse

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Флаг для отслеживания того, что данные отправлены, но пользователь еще не завершил процесс
    private var onboardingDataSubmitted = false

    private fun validateData(
        username: String,
        age: Int,
        gender: Int,
        height: Int,
        weight: Float,
        activityLevel: Int,
        goalType: String,
        goalKg: Float?
    ): String? {
        if (!username.matches(Regex("^[a-zA-Z0-9_-]{3,50}$"))) {
            return "Имя пользователя должно содержать только буквы, цифры, _ и - (от 3 до 50 символов)"
        }
        if (age !in 0..150) {
            return "Возраст должен быть от 0 до 150 лет"
        }
        if (gender !in 0..1) {
            return "Пол должен быть 0 (женский) или 1 (мужской)"
        }
        if (height !in 0..300) {
            return "Рост должен быть от 0 до 300 см"
        }
        if (weight !in 0f..500f) {
            return "Вес должен быть от 0 до 500 кг"
        }
        if (activityLevel !in 1..7) {
            return "Уровень активности должен быть от 1 до 7"
        }
        if (goalType !in listOf("loss", "maintain", "gain")) {
            return "Некорректный тип цели"
        }
        if (goalType != "maintain" && (goalKg == null || goalKg !in 0f..500f)) {
            return "Целевой вес должен быть от 0 до 500 кг"
        }
        return null
    }

    private fun handleApiError(e: Exception): String {
        return when (e) {
            is HttpException -> {
                when (e.code()) {
                    401 -> "Ошибка авторизации. Пожалуйста, войдите снова."
                    422 -> "Неверный формат данных. Пожалуйста, проверьте введенные значения."
                    else -> "Ошибка сервера: ${e.code()}"
                }
            }
            else -> e.message ?: "Неизвестная ошибка"
        }
    }

    fun checkOnboardingStatus() {
        viewModelScope.launch {
            try {
                val status = repository.getOnboardingPlanStatus()
                // Если данные онбординга уже отправлены, но пользователь еще не завершил процесс,
                // не устанавливаем isOnboardingCompleted в true
                if (onboardingDataSubmitted) {
                    _isOnboardingCompleted.value = false
                } else {
                    _isOnboardingCompleted.value = status.onboardingPlanCompleted
                }
                
                if (status.onboardingPlanCompleted && !onboardingDataSubmitted) {
                    loadCurrentPlan()
                }
            } catch (e: Exception) {
                _error.value = handleApiError(e)
            }
        }
    }

    private fun loadCurrentPlan() {
        viewModelScope.launch {
            try {
                val plan = repository.getCurrentPlan()
                _onboardingResponse.value = OnboardingPlanResponse(
                    status = "success",
                    message = "План успешно загружен",
                    profile = Profile(
                        id = 0,
                        user_id = 0,
                        username = "",
                        age = 0,
                        gender = 0,
                        height = 0,
                        weight = 0f,
                        activity_level = 0,
                        goal_type = "",
                        goal_kg = null
                    ),
                    nutrition_plan = plan
                )
            } catch (e: Exception) {
                _error.value = handleApiError(e)
            }
        }
    }

    fun completeOnboarding(
        username: String,
        age: Int,
        gender: Int,
        height: Int,
        weight: Float,
        activityLevel: Int,
        goalType: String,
        goalKg: Float?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val validationError = validateData(
                    username, age, gender, height, weight,
                    activityLevel, goalType, goalKg
                )
                
                if (validationError != null) {
                    _error.value = validationError
                    return@launch
                }

                val data = OnboardingPlanData(
                    username = username,
                    age = age,
                    gender = gender,
                    height = height,
                    weight = weight,
                    activity_level = activityLevel,
                    goal_type = goalType,
                    goal_kg = if (goalType != "maintain") goalKg else null
                )
                
                val response = repository.completeOnboardingPlan(data)
                _onboardingResponse.value = response
                onboardingDataSubmitted = true
                _isOnboardingCompleted.value = false // Явно устанавливаем false до нажатия "Начать"
                _error.value = null
            } catch (e: Exception) {
                _error.value = handleApiError(e)
                if (e is HttpException && e.code() == 401) {
                    _isOnboardingCompleted.value = null
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun finishOnboarding() {
        _isOnboardingCompleted.value = true
    }

    fun markOnboardingAsCompleted() {
        onboardingDataSubmitted = false
        _isOnboardingCompleted.value = true
    }
}

class OnboardingPlanViewModelFactory(
    private val repository: OnboardingPlanRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingPlanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OnboardingPlanViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 