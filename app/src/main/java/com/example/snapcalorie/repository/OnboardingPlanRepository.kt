package com.example.snapcalorie.repository

import com.example.snapcalorie.model.OnboardingPlanData
import com.example.snapcalorie.model.OnboardingPlanResponse
import com.example.snapcalorie.model.OnboardingPlanStatus
import com.example.snapcalorie.model.NutritionPlan
import com.example.snapcalorie.network.ApiService
import com.example.snapcalorie.storage.TokenStorage

class OnboardingPlanRepository(
    private val apiService: ApiService,
    private val tokenStorage: TokenStorage
) {
    private fun getAuthHeader(): String {
        return "Bearer ${tokenStorage.token}"
    }

    suspend fun getOnboardingPlanStatus(): OnboardingPlanStatus {
        return apiService.getOnboardingPlanStatus(getAuthHeader())
    }

    suspend fun completeOnboardingPlan(data: OnboardingPlanData): OnboardingPlanResponse {
        return apiService.completeOnboardingPlan(getAuthHeader(), data)
    }

    suspend fun getCurrentPlan(): NutritionPlan {
        return apiService.getCurrentPlan(getAuthHeader())
    }
} 