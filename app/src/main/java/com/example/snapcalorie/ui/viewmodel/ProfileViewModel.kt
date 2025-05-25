package com.example.snapcalorie.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snapcalorie.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class UserData(
    val id: Int,
    val email: String,
    val registered_at: String
) {
    val formattedRegistrationDate: String
        get() = try {
            val dateTime = LocalDateTime.parse(registered_at)
            val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy г.")
            dateTime.format(formatter)
        } catch (e: DateTimeParseException) {
            registered_at // возвращаем оригинальную строку в случае ошибки парсинга
        }
}

data class PlanData(
    val calories_per_day: Int,
    val protein_g: Int,
    val fat_g: Int,
    val carb_g: Int,
    val duration_weeks: Int,
    val smart_goal: String,
    val id: Int,
    val user_id: Int,
    val created_at: String
)

data class ProfileData(
    val username: String,
    val age: Int,
    val gender: Int,
    val height: Int,
    val weight: Int,
    val activity_level: Int,
    val goal_type: String,
    val goal_kg: Int,
    val id: Int,
    val user_id: Int
)

class ProfileViewModel(
    private val apiService: ApiService,
    private val getToken: () -> String = { "" }
) : ViewModel() {
    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData

    private val _planData = MutableStateFlow<PlanData?>(null)
    val planData: StateFlow<PlanData?> = _planData

    private val _profileData = MutableStateFlow<ProfileData?>(null)
    val profileData: StateFlow<ProfileData?> = _profileData

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadUserData() {
        viewModelScope.launch {
            try {
                val response = apiService.getUserData(getToken())
                _userData.value = response
            } catch (e: Exception) {
                _error.value = "Не удалось загрузить данные пользователя: ${e.message}"
            }
        }
    }

    fun loadPlanData() {
        viewModelScope.launch {
            try {
                val response = apiService.getPlanData(getToken())
                _planData.value = response
            } catch (e: Exception) {
                _error.value = "Не удалось загрузить данные плана: ${e.message}"
            }
        }
    }

    fun loadProfileData() {
        viewModelScope.launch {
            try {
                val response = apiService.getProfileData(getToken())
                _profileData.value = response
            } catch (e: Exception) {
                _error.value = "Не удалось загрузить данные профиля: ${e.message}"
            }
        }
    }
} 