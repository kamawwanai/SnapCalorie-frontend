package com.example.snapcalorie.model

data class OnboardingPlanResponse(
    val status: String,
    val message: String,
    val profile: Profile,
    val nutrition_plan: NutritionPlan
)

data class Profile(
    val id: Int,
    val user_id: Int,
    val username: String,
    val age: Int,
    val gender: Int,
    val height: Int,
    val weight: Float,
    val activity_level: Int,
    val goal_type: String,
    val goal_kg: Float?
)

data class NutritionPlan(
    val id: Int,
    val user_id: Int,
    val calories_per_day: Int,
    val protein_g: Int,
    val fat_g: Int,
    val carb_g: Int,
    val duration_weeks: Int,
    val smart_goal: String,
    val created_at: String
) 