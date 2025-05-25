package com.example.snapcalorie.model

data class AnalyzeResponse(
    val dishClass: String,
    val confidence: Float,
    val volumeMl: Float,
    val massGrams: Float,
    val calories: Float,
    val protein: Float,
    val fats: Float,
    val carbs: Float
) 