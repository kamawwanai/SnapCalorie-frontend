package com.example.snapcalorie.ui.screens.onboarding

import com.example.snapcalorie.R

data class OnboardingPage(
    val title: String,
    val description: String,
    val image: Int
)

val onboardingPages = listOf(
    OnboardingPage(
        title = "Добро пожаловать в SnapCalorie!",
        description = "Приложение для подсчета калорий с помощью фотографий еды",
        image = R.drawable.img_onboarding1
    ),
    OnboardingPage(
        title = "Фотографируйте еду",
        description = "Просто сфотографируйте свою еду, и мы определим калорийность блюда",
        image = R.drawable.img_onboarding2
    ),
    OnboardingPage(
        title = "Отслеживайте прогресс",
        description = "Следите за своим прогрессом и достигайте целей по питанию",
        image = R.drawable.img_onboarding3
    ),
    OnboardingPage(
        title = "Анализируйте привычки",
        description = "Получайте подробную статистику о своем питании и рекомендации по улучшению",
        image = R.drawable.img_onboarding4
    )
) 