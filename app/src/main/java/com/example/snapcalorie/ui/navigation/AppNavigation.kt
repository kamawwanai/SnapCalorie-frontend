package com.example.snapcalorie.ui.navigation

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.snapcalorie.ui.screens.*
import com.example.snapcalorie.ui.components.Screen
import com.example.snapcalorie.ui.viewmodel.AuthViewModel
import com.example.snapcalorie.ui.viewmodel.OnboardingPlanViewModel
import com.example.snapcalorie.ui.viewmodel.ProfileViewModel
import com.example.snapcalorie.network.ApiModule
import com.example.snapcalorie.storage.TokenStorage

@Composable
fun AppNavigation(
    startDestination: String,
    authViewModel: AuthViewModel,
    onboardingPlanViewModel: OnboardingPlanViewModel,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenStorage = remember { TokenStorage(context) }
    val apiService = remember { ApiModule.provideApiService { tokenStorage.token } }
    val profileViewModel = remember {
        ProfileViewModel(
            apiService = apiService,
            getToken = { "Bearer ${tokenStorage.token}" }
        )
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("home") {
            MainScreen(
                viewModel = authViewModel,
                onLogout = onSignOut
            )
        }
        
        composable("onboarding_plan") {
            OnboardingPlanScreen(
                viewModel = onboardingPlanViewModel,
                onComplete = {
                    // После завершения онбординга переходим к экрану готового плана
                    navController.navigate("plan_ready") {
                        popUpTo("onboarding_plan") { inclusive = true }
                    }
                }
            )
        }

        composable("plan_ready") {
            PlanReadyScreen(
                apiService = apiService,
                authToken = tokenStorage.token ?: "",
                onFinish = {
                    // Отмечаем онбординг как завершенный
                    onboardingPlanViewModel.markOnboardingAsCompleted()
                    // После просмотра плана переходим к профилю
                    navController.navigate("profile") {
                        popUpTo("plan_ready") { inclusive = true }
                    }
                }
            )
        }

        composable("profile") {
            ProfileScreen(
                viewModel = profileViewModel,
                onLogout = onSignOut,
                onNavigateToScreen = { screen ->
                    when (screen) {
                        Screen.DIARY -> navController.navigate("diary")
                        Screen.STATS -> navController.navigate("stats")
                        Screen.CAMERA -> navController.navigate("camera")
                        Screen.RECOMMENDATIONS -> navController.navigate("recommendations")
                        Screen.PROFILE -> {} // Already on profile screen
                        Screen.IMAGE_SEGMENTATION -> {} // Not accessible from profile
                    }
                }
            )
        }

        // Add other screen routes
        composable("diary") {
            MyDiaryScreen(
                onNavigateToScreen = { screen ->
                    when (screen) {
                        Screen.DIARY -> {} // Already on diary screen
                        Screen.STATS -> navController.navigate("stats")
                        Screen.CAMERA -> navController.navigate("camera")
                        Screen.RECOMMENDATIONS -> navController.navigate("recommendations")
                        Screen.PROFILE -> navController.navigate("profile")
                        Screen.IMAGE_SEGMENTATION -> {} // Handled by onNavigateToImageSegmentation
                    }
                },
                onNavigateToAddFoodManually = {
                    navController.navigate("add_food_manually")
                },
                onNavigateToImageSegmentation = { uri ->
                    navController.navigate("image_segmentation/${Uri.encode(uri.toString())}")
                },
                apiService = apiService,
                authToken = "Bearer ${tokenStorage.token}"
            )
        }
        
        composable("add_food_manually") {
            AddFoodManuallyScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            "image_segmentation/{imageUri}",
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val imageUriString = backStackEntry.arguments?.getString("imageUri")
            val imageUri = Uri.parse(imageUriString)
            
            ImageSegmentationScreen(
                imageUri = imageUri,
                onNavigateToScreen = { screen ->
                    when (screen) {
                        Screen.DIARY -> navController.navigate("diary") {
                            popUpTo("image_segmentation/{imageUri}") { inclusive = true }
                        }
                        Screen.STATS -> navController.navigate("stats") {
                            popUpTo("image_segmentation/{imageUri}") { inclusive = true }
                        }
                        Screen.CAMERA -> navController.navigate("camera") {
                            popUpTo("image_segmentation/{imageUri}") { inclusive = true }
                        }
                        Screen.RECOMMENDATIONS -> navController.navigate("recommendations") {
                            popUpTo("image_segmentation/{imageUri}") { inclusive = true }
                        }
                        Screen.PROFILE -> navController.navigate("profile") {
                            popUpTo("image_segmentation/{imageUri}") { inclusive = true }
                        }
                        Screen.IMAGE_SEGMENTATION -> {} // Already on segmentation screen
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("stats") {
            StatsScreen()
        }

        composable("camera") {
            CameraScreen(
                onNavigateToScreen = { screen ->
                    when (screen) {
                        Screen.DIARY -> navController.navigate("diary")
                        Screen.STATS -> navController.navigate("stats")
                        Screen.CAMERA -> {} // Already on camera screen
                        Screen.RECOMMENDATIONS -> navController.navigate("recommendations")
                        Screen.PROFILE -> navController.navigate("profile")
                        Screen.IMAGE_SEGMENTATION -> {} // Not accessible from camera
                    }
                }
            )
        }
        
        composable("recommendations") {
            RecommendationsScreen(
                onNavigateToScreen = { screen ->
                    when (screen) {
                        Screen.DIARY -> navController.navigate("diary")
                        Screen.STATS -> navController.navigate("stats")
                        Screen.CAMERA -> navController.navigate("camera")
                        Screen.RECOMMENDATIONS -> {} // Already on recommendations screen
                        Screen.PROFILE -> navController.navigate("profile")
                        Screen.IMAGE_SEGMENTATION -> {} // Not accessible from recommendations
                    }
                }
            )
        }
    }
} 