package com.example.snapcalorie.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.snapcalorie.di.ViewModelModule
import com.example.snapcalorie.ui.theme.Base0
import com.example.snapcalorie.viewmodel.OnboardingPlanViewModel

enum class OnboardingPlanStep {
    PERSONAL_INFO,
    ACTIVITY_LEVEL,
    GOAL_SELECTION,
    PLAN_READY
}

@Composable
fun OnboardingPlanScreen(
    onNavigateToProfile: () -> Unit,
    viewModel: OnboardingPlanViewModel = viewModel(factory = ViewModelModule.Factory)
) {
    var currentStep by remember { mutableStateOf(OnboardingPlanStep.PERSONAL_INFO) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Base0)
    ) {
        when (currentStep) {
            OnboardingPlanStep.PERSONAL_INFO -> {
                PersonalInfoScreen(
                    viewModel = viewModel,
                    onNext = { currentStep = OnboardingPlanStep.ACTIVITY_LEVEL }
                )
            }
            OnboardingPlanStep.ACTIVITY_LEVEL -> {
                ActivityLevelScreen(
                    viewModel = viewModel,
                    onNext = { currentStep = OnboardingPlanStep.GOAL_SELECTION },
                    onBack = { currentStep = OnboardingPlanStep.PERSONAL_INFO }
                )
            }
            OnboardingPlanStep.GOAL_SELECTION -> {
                GoalSelectionScreen(
                    viewModel = viewModel,
                    onNext = {
                        viewModel.completeOnboarding()
                        currentStep = OnboardingPlanStep.PLAN_READY
                    },
                    onBack = { currentStep = OnboardingPlanStep.ACTIVITY_LEVEL }
                )
            }
            OnboardingPlanStep.PLAN_READY -> {
                PlanReadyScreen(
                    viewModel = viewModel,
                    onFinish = onNavigateToProfile
                )
            }
        }

        if (viewModel.error != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(text = viewModel.error ?: "An error occurred")
            }
        }
    }
} 