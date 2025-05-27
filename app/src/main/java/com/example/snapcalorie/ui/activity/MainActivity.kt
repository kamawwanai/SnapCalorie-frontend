package com.example.snapcalorie.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.snapcalorie.network.ApiModule
import com.example.snapcalorie.repository.AuthRepository
import com.example.snapcalorie.repository.OnboardingPlanRepository
import com.example.snapcalorie.storage.OnboardingStorage
import com.example.snapcalorie.storage.TokenStorage
import com.example.snapcalorie.ui.navigation.AppNavigation
import com.example.snapcalorie.ui.theme.SnapCalorieTheme
import com.example.snapcalorie.ui.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var tokenStorage: TokenStorage
    private lateinit var onboardingStorage: OnboardingStorage
    
    private val authViewModel by viewModels<AuthViewModel> {
        tokenStorage = TokenStorage(this)
        AuthViewModelFactory(
            AuthRepository(
                ApiModule.provideApiService { tokenStorage.token },
                tokenStorage
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("MainActivity", "onCreate started")
        
        // Инициализируем хранилища
        tokenStorage = TokenStorage(this)
        onboardingStorage = OnboardingStorage(this)
        
        // Логируем текущие состояния
        val isOnboardingCompleted = onboardingStorage.isInitialOnboardingCompleted
        val token = tokenStorage.token
        
        Log.d("MainActivity", "isInitialOnboardingCompleted: $isOnboardingCompleted")
        Log.d("MainActivity", "token exists: ${!token.isNullOrBlank()}")
        
        // Устанавливаем сплеш скрин
        val splashScreen = installSplashScreen()
        var isLoading by mutableStateOf(true)
        splashScreen.setKeepOnScreenCondition { isLoading }

        // Определяем логику навигации после сплеш скрина
        lifecycleScope.launch {
            delay(1500) // Показываем сплеш скрин 1.5 секунды
            
            Log.d("MainActivity", "Starting navigation logic")
            
            when {
                // 1. Если приложение запущено в первый раз - показываем приветственный онбординг
                !onboardingStorage.isInitialOnboardingCompleted -> {
                    Log.d("MainActivity", "First launch detected - starting OnboardingActivity")
                    isLoading = false
                    startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                    finish()
                }
                
                // 2. Если пользователь не авторизован - показываем экран входа
                tokenStorage.token.isNullOrBlank() -> {
                    Log.d("MainActivity", "No token - starting LoginActivity")
                    isLoading = false
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
                
                // 3. Если пользователь авторизован - переходим к основному приложению
                else -> {
                    Log.d("MainActivity", "User authenticated - setting up main app")
                    isLoading = false
                    setupMainApp()
                }
            }
        }
    }

    private fun setupMainApp() {
        setContent {
            SnapCalorieTheme {
                // Создаем OnboardingPlanViewModel для проверки статуса
                val onboardingPlanViewModel = remember {
                    OnboardingPlanViewModelFactory(
                        OnboardingPlanRepository(
                            ApiModule.provideApiService { tokenStorage.token },
                            tokenStorage
                        )
                    ).create(OnboardingPlanViewModel::class.java)
                }
                
                var startDestination by remember { mutableStateOf<String?>(null) }
                var showError by remember { mutableStateOf<String?>(null) }
                var isTokenValidated by remember { mutableStateOf(false) }

                // Сначала проверяем валидность токена
                LaunchedEffect(authViewModel) {
                    authViewModel.loadProfile()
                }

                // Отслеживаем результат проверки токена
                val user by authViewModel.user.observeAsState()
                val authError by authViewModel.error.observeAsState()

                LaunchedEffect(user, authError) {
                    when {
                        user != null -> {
                            // Токен валидный, пользователь загружен
                            isTokenValidated = true
                            // Теперь проверяем статус onboardingPlan
                            onboardingPlanViewModel.checkOnboardingStatus()
                        }
                        authError != null -> {
                            // Ошибка авторизации - токен недействительный
                            val errorMessage = authError
                            Log.w("MainActivity", "Auth error detected: $errorMessage")
                            // Только очищаем токен если это действительно ошибка авторизации
                            if (errorMessage?.contains("Authentication failed") == true || 
                                errorMessage?.contains("401") == true ||
                                errorMessage?.contains("Unauthorized") == true) {
                                Log.w("MainActivity", "Clearing token due to auth error")
                                tokenStorage.clear()
                                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                                finish()
                            } else {
                                Log.w("MainActivity", "Non-auth error, not clearing token: $errorMessage")
                                showError = errorMessage
                            }
                        }
                    }
                }

                // Определяем начальный экран на основе статуса onboardingPlan (только после валидации токена)
                LaunchedEffect(isTokenValidated) {
                    if (isTokenValidated) {
                        onboardingPlanViewModel.isOnboardingCompleted.collectLatest { isCompleted ->
                            when (isCompleted) {
                                true -> {
                                    // Онбординг плана завершен - переходим к профилю
                                    startDestination = "profile"
                                }
                                false -> {
                                    // Онбординг плана не завершен - показываем onboardingPlan
                                    startDestination = "onboarding_plan"
                                }
                                null -> {
                                    // Загрузка - ждем результата
                                }
                            }
                        }
                    }
                }

                // Обрабатываем ошибки onboarding плана
                LaunchedEffect(Unit) {
                    onboardingPlanViewModel.error.collectLatest { error ->
                        if (error != null) {
                            val errorMessage = error
                            Log.w("MainActivity", "OnboardingPlan error: $errorMessage")
                            showError = errorMessage
                            // Только очищаем токен если это критическая ошибка авторизации
                            if (errorMessage?.contains("401") == true || 
                                errorMessage?.contains("Authentication failed") == true ||
                                errorMessage?.contains("Unauthorized") == true) {
                                Log.w("MainActivity", "Clearing token due to onboarding auth error")
                                tokenStorage.clear()
                                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                                finish()
                            }
                        }
                    }
                }

                // Показываем диалог ошибки
                if (showError != null) {
                    AlertDialog(
                        onDismissRequest = { showError = null },
                        title = { Text("Ошибка") },
                        text = { Text(showError!!) },
                        confirmButton = {
                            TextButton(onClick = { showError = null }) {
                                Text("OK")
                            }
                        }
                    )
                }

                // Запускаем навигацию когда определен начальный экран
                startDestination?.let { destination ->
                    AppNavigation(
                        startDestination = destination,
                        authViewModel = authViewModel,
                        onboardingPlanViewModel = onboardingPlanViewModel,
                        onSignOut = {
                            tokenStorage.clear()
                            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val user by viewModel.user.observeAsState()
    val isLoggedOut = user == null

    if (isLoggedOut) {
        onLogout()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome, ${user!!.email}!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            viewModel.logout()
            onLogout()
        }) {
            Text("Logout")
        }
    }
}