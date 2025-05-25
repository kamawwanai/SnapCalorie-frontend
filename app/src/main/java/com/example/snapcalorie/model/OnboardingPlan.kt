package com.example.snapcalorie.model

data class OnboardingPlanStatus(
    val onboardingPlanCompleted: Boolean
)

data class OnboardingPlanData(
    val username: String,
    val age: Int,
    val gender: Int,
    val height: Int,
    val weight: Float,
    val activity_level: Int,
    val goal_type: String,
    val goal_kg: Float?
) 