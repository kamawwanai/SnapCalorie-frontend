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
import com.example.snapcalorie.ui.viewmodel.ARSessionViewModel
import com.example.snapcalorie.network.ApiModule
import com.example.snapcalorie.storage.TokenStorage
import androidx.lifecycle.viewmodel.compose.viewModel

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
    val apiService = remember(tokenStorage) { ApiModule.provideApiService { tokenStorage.token } }
    val profileViewModel = remember {
        ProfileViewModel(
            apiService = apiService,
            getToken = { "Bearer ${tokenStorage.token}" }
        )
    }
    
    // Create shared ARSessionViewModel at NavHost level
    val arSessionViewModel: ARSessionViewModel = viewModel()

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
                        Screen.AR_CAMERA_TOP -> {} // Not accessible from profile
                        Screen.AR_SEGMENTATION_TOP -> {} // Not accessible from profile
                        Screen.AR_CAMERA_SIDE -> {} // Not accessible from profile
                        Screen.AR_SEGMENTATION_SIDE -> {} // Not accessible from profile
                        Screen.VOLUME_CALCULATION -> {} // Not accessible from profile
                    }
                }
            )
        }

        // Add other screen routes
        composable("diary") {
            // Стабилизируем токен, чтобы избежать recomposition
            val stableAuthToken = remember(tokenStorage.token) { tokenStorage.token ?: "" }
            
            MyDiaryScreen(
                onNavigateToScreen = { screen ->
                    when (screen) {
                        Screen.DIARY -> {} // Already on diary screen
                        Screen.STATS -> navController.navigate("stats")
                        Screen.CAMERA -> navController.navigate("camera")
                        Screen.RECOMMENDATIONS -> navController.navigate("recommendations")
                        Screen.PROFILE -> navController.navigate("profile")
                        Screen.IMAGE_SEGMENTATION -> {} // Handled by onNavigateToImageSegmentation
                        Screen.AR_CAMERA_TOP -> {} // Not accessible from diary
                        Screen.AR_SEGMENTATION_TOP -> {} // Not accessible from diary
                        Screen.AR_CAMERA_SIDE -> {} // Not accessible from diary
                        Screen.AR_SEGMENTATION_SIDE -> {} // Not accessible from diary
                        Screen.VOLUME_CALCULATION -> {} // Not accessible from diary
                    }
                },
                onNavigateToAddFoodManually = {
                    navController.navigate("add_food_manually")
                },
                onNavigateToImageSegmentation = { uri ->
                    navController.navigate("image_segmentation/${Uri.encode(uri.toString())}")
                },
                apiService = apiService,
                authToken = stableAuthToken
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
                        Screen.AR_CAMERA_TOP -> {} // Not accessible from image segmentation
                        Screen.AR_SEGMENTATION_TOP -> {} // Not accessible from image segmentation
                        Screen.AR_CAMERA_SIDE -> {} // Not accessible from image segmentation
                        Screen.AR_SEGMENTATION_SIDE -> {} // Not accessible from image segmentation
                        Screen.VOLUME_CALCULATION -> {} // Not accessible from image segmentation
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("stats") {
            StatsScreen(
                onNavigateToScreen = { screen ->
                    when (screen) {
                        Screen.DIARY -> navController.navigate("diary")
                        Screen.STATS -> {} // Already on stats screen
                        Screen.CAMERA -> navController.navigate("camera")
                        Screen.RECOMMENDATIONS -> navController.navigate("recommendations")
                        Screen.PROFILE -> navController.navigate("profile")
                        Screen.IMAGE_SEGMENTATION -> {} // Not accessible from stats
                        Screen.AR_CAMERA_TOP -> {} // Not accessible from stats
                        Screen.AR_SEGMENTATION_TOP -> {} // Not accessible from stats
                        Screen.AR_CAMERA_SIDE -> {} // Not accessible from stats
                        Screen.AR_SEGMENTATION_SIDE -> {} // Not accessible from stats
                        Screen.VOLUME_CALCULATION -> {} // Not accessible from stats
                    }
                },
                apiService = apiService,
                authToken = tokenStorage.token ?: ""
            )
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
                        Screen.AR_CAMERA_TOP -> navController.navigate("ar_camera_top")
                        Screen.AR_SEGMENTATION_TOP -> {} // Not directly accessible
                        Screen.AR_CAMERA_SIDE -> {} // Not directly accessible
                        Screen.AR_SEGMENTATION_SIDE -> {} // Not directly accessible
                        Screen.VOLUME_CALCULATION -> {} // Not directly accessible
                    }
                },
                onNavigateToPhotoCapture = {
                    navController.navigate("photo_capture")
                }
            )
        }
        
        // Photo Capture Screen
        composable("photo_capture") {
            PhotoCaptureScreen(
                onImageCaptured = { uri ->
                    navController.navigate("image_segmentation/${Uri.encode(uri.toString())}")
                },
                onNavigateBack = {
                    navController.popBackStack()
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
                        Screen.AR_CAMERA_TOP -> {} // Not accessible from recommendations
                        Screen.AR_SEGMENTATION_TOP -> {} // Not accessible from recommendations
                        Screen.AR_CAMERA_SIDE -> {} // Not accessible from recommendations
                        Screen.AR_SEGMENTATION_SIDE -> {} // Not accessible from recommendations
                        Screen.VOLUME_CALCULATION -> {} // Not accessible from recommendations
                    }
                }
            )
        }
        
        // AR Camera Top View
        composable("ar_camera_top") {
            ARCameraScreen(
                captureStage = CaptureStage.TOP_VIEW,
                onCaptureComplete = { arCaptureData ->
                    try {
                        android.util.Log.d("Navigation", "AR Camera Top - onCaptureComplete called")
                        android.util.Log.d("Navigation", "ARCaptureData - RGB: ${arCaptureData.rgbFrame.width}x${arCaptureData.rgbFrame.height}")
                        android.util.Log.d("Navigation", "ARCaptureData - Depth: ${arCaptureData.depthMap.size} values")
                        
                        // Store captured data in ViewModel
                        arSessionViewModel.setTopViewCaptureData(arCaptureData)
                        android.util.Log.d("Navigation", "Data stored in ViewModel, navigating to segmentation")
                        
                        navController.navigate("ar_segmentation_top")
                        android.util.Log.d("Navigation", "Navigation to ar_segmentation_top completed")
                    } catch (e: Exception) {
                        android.util.Log.e("Navigation", "Error in onCaptureComplete", e)
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // AR Segmentation Top View
        composable("ar_segmentation_top") {
            val captureData = arSessionViewModel.topViewCaptureData
            
            android.util.Log.d("Navigation", "AR Segmentation Top - composable called")
            android.util.Log.d("Navigation", "Capture data available: ${captureData != null}")
            
            if (captureData != null) {
                android.util.Log.d("Navigation", "Creating ARSegmentationScreen with data: ${captureData.rgbFrame.width}x${captureData.rgbFrame.height}")
                
                ARSegmentationScreen(
                    arCaptureData = captureData,
                    captureStage = CaptureStage.TOP_VIEW,
                    onSegmentationComplete = { topViewResult ->
                        try {
                            android.util.Log.d("Navigation", "Segmentation completed, storing result")
                            // Store segmentation result and navigate to side view camera
                            arSessionViewModel.setTopViewSegmentationResult(topViewResult)
                            navController.navigate("ar_camera_side")
                        } catch (e: Exception) {
                            android.util.Log.e("Navigation", "Error in onSegmentationComplete", e)
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            } else {
                android.util.Log.e("Navigation", "No capture data available, going back")
                // Handle error case - no capture data available
                navController.popBackStack()
            }
        }
        
        // AR Camera Side View
        composable("ar_camera_side") {
            ARCameraScreen(
                captureStage = CaptureStage.SIDE_VIEW,
                onCaptureComplete = { arCaptureData ->
                    // Store captured data in ViewModel
                    arSessionViewModel.setSideViewCaptureData(arCaptureData)
                    navController.navigate("ar_segmentation_side")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // AR Segmentation Side View
        composable("ar_segmentation_side") {
            val captureData = arSessionViewModel.sideViewCaptureData
            val topViewClassificationResult = arSessionViewModel.getTopViewClassificationResult()
            
            if (captureData != null) {
                ARSegmentationScreen(
                    arCaptureData = captureData,
                    captureStage = CaptureStage.SIDE_VIEW,
                    topViewClassificationResult = topViewClassificationResult,
                    onSegmentationComplete = { sideViewResult ->
                        // Store segmentation result and navigate to volume calculation
                        arSessionViewModel.setSideViewSegmentationResult(sideViewResult)
                        navController.navigate("volume_calculation")
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            } else {
                // Handle error case - no capture data available
                navController.popBackStack()
            }
        }
        
        // Volume Calculation
        composable("volume_calculation") {
            val topViewResult = arSessionViewModel.topViewSegmentationResult
            val sideViewResult = arSessionViewModel.sideViewSegmentationResult
            
            if (topViewResult != null && sideViewResult != null) {
                val volumeData = VolumeCalculationData(
                    topViewResult = topViewResult,
                    sideViewResult = sideViewResult
                )
                
                VolumeCalculationScreen(
                    volumeData = volumeData,
                    onNavigateToScreen = { screen ->
                        when (screen) {
                            Screen.DIARY -> {
                                try {
                                    arSessionViewModel.clearSession()
                                    navController.navigate("diary") {
                                        // Очищаем back stack для предотвращения проблем с памятью
                                        popUpTo("volume_calculation") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("Navigation", "Error navigating to diary", e)
                                    // Fallback navigation
                                    try {
                                        navController.navigate("diary")
                                    } catch (e2: Exception) {
                                        android.util.Log.e("Navigation", "Fallback navigation failed", e2)
                                    }
                                }
                            }
                            Screen.PROFILE -> {
                                try {
                                    android.util.Log.d("Navigation", "Navigating to profile from volume calculation")
                                    // Сначала переходим к профилю, потом очищаем сессию
                                    navController.navigate("profile") {
                                        // Очищаем back stack для предотвращения проблем с памятью
                                        popUpTo("volume_calculation") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                    // Очищаем AR сессию после навигации
                                    arSessionViewModel.clearSession()
                                    android.util.Log.d("Navigation", "Navigation to profile completed successfully")
                                } catch (e: Exception) {
                                    android.util.Log.e("Navigation", "Error navigating to profile", e)
                                    // Fallback navigation без очистки back stack
                                    try {
                                        navController.navigate("profile")
                                        arSessionViewModel.clearSession()
                                    } catch (e2: Exception) {
                                        android.util.Log.e("Navigation", "Fallback navigation failed", e2)
                                    }
                                }
                            }
                            Screen.CAMERA -> {
                                try {
                                    arSessionViewModel.clearSession()
                                    navController.navigate("camera")
                                } catch (e: Exception) {
                                    android.util.Log.e("Navigation", "Error navigating to camera", e)
                                }
                            }
                            else -> {}
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            } else {
                // Handle error case - missing segmentation results
                navController.popBackStack()
            }
        }
    }
} 