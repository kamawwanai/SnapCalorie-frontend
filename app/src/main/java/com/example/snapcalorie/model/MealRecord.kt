package com.example.snapcalorie.model

data class MealRecordCreate(
    val datetime: String,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
    val meal_type: Int,
    val image_path: String?
)

data class MealRecordSchema(
    val id: Int,
    val user_id: Int,
    val datetime: String,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
    val meal_type: Int,
    val image_path: String?
) 