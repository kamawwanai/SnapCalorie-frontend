package com.example.snapcalorie.network

import com.example.snapcalorie.model.TokenResponse
import com.example.snapcalorie.model.UserCreate
import com.example.snapcalorie.model.UserRead
import com.example.snapcalorie.model.OnboardingPlanStatus
import com.example.snapcalorie.model.OnboardingPlanData
import com.example.snapcalorie.model.OnboardingPlanResponse
import com.example.snapcalorie.model.NutritionPlan
import com.example.snapcalorie.model.ClassificationResponse
import com.example.snapcalorie.model.DetailedClassificationResponse
import com.example.snapcalorie.ui.viewmodel.UserData
import com.example.snapcalorie.ui.viewmodel.PlanData
import com.example.snapcalorie.ui.viewmodel.ProfileData
import com.example.snapcalorie.ui.screens.MealGroup
import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {

    // 1) Регистрация: посылаем JSON {email, password}, получаем UserRead
    @POST("auth/register")
    suspend fun register(@Body user: UserCreate): UserRead

    // 2) Логин: посылаем форму username=email&password, получаем токен
    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") email: String,
        @Field("password") password: String
    ): TokenResponse

    // 3) «Кто я»: посылаем заголовок Authorization, получаем профиль
    @GET("users/me")
    suspend fun me(@Header("Authorization") bearer: String): UserRead

    @GET("onboardingPlan/status")
    suspend fun getOnboardingPlanStatus(@Header("Authorization") bearer: String): OnboardingPlanStatus
    
    @POST("onboardingPlan/complete")
    @Headers("Content-Type: application/json")
    suspend fun completeOnboardingPlan(
        @Header("Authorization") bearer: String,
        @Body data: OnboardingPlanData
    ): OnboardingPlanResponse

    @GET("plans/me")
    suspend fun getCurrentPlan(@Header("Authorization") bearer: String): NutritionPlan

    @GET("users/me")
    suspend fun getUserData(@Header("Authorization") bearer: String): UserData

    @GET("plans/me")
    suspend fun getPlanData(@Header("Authorization") bearer: String): PlanData

    @GET("profiles/me")
    suspend fun getProfileData(@Header("Authorization") bearer: String): ProfileData

    @GET("meals/grouped")
    suspend fun getGroupedMeals(@Header("Authorization") bearer: String): List<MealGroup>

    @Multipart
    @POST("classification/classify")
    suspend fun classifyImage(
        @Header("Authorization") bearer: String,
        @Part file: MultipartBody.Part
    ): ClassificationResponse

    @Multipart
    @POST("classification/classify-detailed")
    suspend fun classifyImageDetailed(
        @Header("Authorization") bearer: String,
        @Part file: MultipartBody.Part
    ): DetailedClassificationResponse
}
