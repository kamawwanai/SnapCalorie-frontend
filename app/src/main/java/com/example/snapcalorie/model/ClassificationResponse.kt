package com.example.snapcalorie.model

import com.google.gson.annotations.SerializedName

data class ClassificationResponse(
    @SerializedName("class")
    val predictedClass: String,
    @SerializedName("message")
    val message: String
)

data class DetailedClassificationResponse(
    @SerializedName("class")
    val predictedClass: String,
    @SerializedName("confidence")
    val confidence: Double,
    @SerializedName("confidence_percentage")
    val confidencePercentage: Double,
    @SerializedName("threshold_met")
    val thresholdMet: Boolean,
    @SerializedName("message")
    val message: String
) 