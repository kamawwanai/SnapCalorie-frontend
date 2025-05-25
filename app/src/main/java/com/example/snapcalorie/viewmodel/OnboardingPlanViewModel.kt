package com.example.snapcalorie.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapcalorie.model.OnboardingPlanResponse
import com.example.snapcalorie.model.Profile
import com.example.snapcalorie.storage.OnboardingStorage
import kotlinx.coroutines.launch
import retrofit2.Response

class OnboardingPlanViewModel(private val context: Context) : ViewModel() {
    var username by mutableStateOf("")
    var age by mutableStateOf("")
    var gender by mutableStateOf(1) // 1 - male, 0 - female
    var height by mutableStateOf("")
    var weight by mutableStateOf("")
    var activityLevel by mutableStateOf(1) // 1-7
    var goalType by mutableStateOf("maintain") // "loss", "maintain", "gain"
    var goalKg by mutableStateOf("")
    
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var onboardingCompleted by mutableStateOf(false)
    var onboardingResponse by mutableStateOf<OnboardingPlanResponse?>(null)

    fun validateUsername(): Boolean {
        return username.length in 3..50 && username.matches(Regex("^[a-zA-Z0-9_-]+$"))
    }

    fun validateAge(): Boolean {
        return try {
            val ageInt = age.toInt()
            ageInt in 0..150
        } catch (e: NumberFormatException) {
            false
        }
    }

    fun validateHeight(): Boolean {
        return try {
            val heightInt = height.toInt()
            heightInt in 0..300
        } catch (e: NumberFormatException) {
            false
        }
    }

    fun validateWeight(): Boolean {
        return try {
            val weightFloat = weight.toFloat()
            weightFloat in 0f..500f
        } catch (e: NumberFormatException) {
            false
        }
    }

    fun validateGoalKg(): Boolean {
        if (goalKg.isEmpty()) return true // Optional field
        return try {
            val goalKgFloat = goalKg.toFloat()
            goalKgFloat in 0f..500f
        } catch (e: NumberFormatException) {
            false
        }
    }

    fun validateForm(): Boolean {
        return validateUsername() &&
                validateAge() &&
                validateHeight() &&
                validateWeight() &&
                validateGoalKg() &&
                activityLevel in 1..7 &&
                goalType in listOf("loss", "maintain", "gain")
    }

    fun checkOnboardingStatus() {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                // Get the completion status from local storage
                val storage = OnboardingStorage(context)
                onboardingCompleted = storage.isPlanOnboardingCompleted
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun completeOnboarding() {
        if (!validateForm()) {
            error = "Please check all fields"
            return
        }

        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                // TODO: Make API call to save onboarding plan data
                // Не устанавливаем здесь флаг завершения onboarding
                // Он будет установлен после нажатия кнопки "Начать" на PlanReadyScreen
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            try {
                val storage = OnboardingStorage(context)
                storage.isPlanOnboardingCompleted = true
                onboardingCompleted = true
            } catch (e: Exception) {
                error = e.message
            }
        }
    }
} 