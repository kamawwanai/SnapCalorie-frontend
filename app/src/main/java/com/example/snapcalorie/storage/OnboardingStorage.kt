package com.example.snapcalorie.storage

import android.content.Context
import android.util.Log

class OnboardingStorage(context: Context) {
    private val prefs = context
        .getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)

    var isInitialOnboardingCompleted: Boolean
        get() = prefs.getBoolean("is_initial_onboarding_completed", false)
        set(value) {
            Log.d("OnboardingStorage", "Setting isInitialOnboardingCompleted: $value")
            prefs.edit().putBoolean("is_initial_onboarding_completed", value).apply()
        }

    var isPlanOnboardingCompleted: Boolean
        get() = prefs.getBoolean("is_plan_onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("is_plan_onboarding_completed", value).apply()
    
    // Метод для сброса всех данных онбординга
    // Для тестирования: вызовите onboardingStorage.reset() в MainActivity.onCreate()
    fun reset() {
        Log.d("OnboardingStorage", "Resetting all onboarding data")
        prefs.edit().clear().apply()
    }
} 