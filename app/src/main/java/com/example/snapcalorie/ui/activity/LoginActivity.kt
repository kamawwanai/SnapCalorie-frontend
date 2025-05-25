package com.example.snapcalorie.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.graphics.graphicsLayer
import com.example.snapcalorie.network.ApiModule
import com.example.snapcalorie.repository.AuthRepository
import com.example.snapcalorie.storage.TokenStorage
import com.example.snapcalorie.ui.components.AppIcon
import com.example.snapcalorie.ui.theme.*
import com.example.snapcalorie.ui.viewmodel.AuthViewModel
import com.example.snapcalorie.ui.viewmodel.AuthViewModelFactory
import com.example.snapcalorie.ui.theme.montserratFamily

class LoginActivity : ComponentActivity() {

    private lateinit var tokenStorage: TokenStorage

    // Передаём в фабрику репозиторий с поставщиком токена
    private val viewModel by viewModels<AuthViewModel> {
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

        setContent {
            SnapCalorieTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onNavigateToRegister = {
                        startActivity(Intent(this, RegisterActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Состояния для отслеживания первого ввода
    var isEmailTouched by remember { mutableStateOf(false) }
    var isPasswordTouched by remember { mutableStateOf(false) }
    
    // Состояния для ошибок валидации
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    
    // Состояние валидности формы
    var isFormValid by remember { mutableStateOf(false) }
    
    val user by viewModel.user.observeAsState()
    val error by viewModel.error.observeAsState()

    // Функция для очистки ошибки аутентификации
    fun clearAuthError() {
        if (error?.contains("Authentication failed") == true) {
            viewModel.clearError()
        }
    }

    // Преобразование ошибки в понятный пользователю текст
    val errorMessage = when (error) {
        "Authentication failed. Please check your credentials." -> "Неверный email или пароль"
        "Cannot connect to server. Please check your internet connection." -> "Не удалось подключиться к серверу. Проверьте подключение к интернету"
        "Connection failed. Server might be down." -> "Ошибка подключения. Сервер может быть недоступен"
        "Connection timed out. Please try again." -> "Превышено время ожидания. Попробуйте снова"
        else -> error
    }

    // Показываем ошибку аутентификации в полях
    val showAuthError = error?.contains("Authentication failed") == true

    // Функция валидации формы
    fun validateForm(): Boolean {
        var isValid = true
        
        // Проверка email только если поле было тронуто
        if (isEmailTouched) {
            if (email.isEmpty()) {
                emailError = "Email не может быть пустым"
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailError = "Введите корректный email адрес"
                isValid = false
            } else {
                emailError = null
            }
        }
        
        // Проверка пароля только если поле было тронуто
        if (isPasswordTouched) {
            if (password.isEmpty()) {
                passwordError = "Пароль не может быть пустым"
                isValid = false
            } else {
                passwordError = null
            }
        }
        
        // Форма валидна только если все поля заполнены корректно
        isFormValid = email.isNotEmpty() && 
                     android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() && 
                     password.isNotEmpty()
        
        return isValid
    }

    // Эффект для валидации при изменении полей
    LaunchedEffect(email, password) {
        validateForm()
    }

    // Если пользователь загрузился — переходим дальше
    LaunchedEffect(user) {
        if (user != null) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Green10, Orange20)
                )
            )
            .systemBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AppIcon()
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Base0
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Inputs
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { 
                                email = it
                                isEmailTouched = true
                                validateForm()
                                clearAuthError()
                            },
                            label = { Text("E-mail") },
                            placeholder = { Text("Введите e-mail") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = emailError != null || showAuthError,
                            supportingText = {
                                emailError?.let {
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { 
                                password = it
                                isPasswordTouched = true
                                validateForm()
                                clearAuthError()
                            },
                            label = { Text("Пароль") },
                            placeholder = { Text("Введите пароль") },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = passwordError != null || showAuthError,
                            supportingText = {
                                passwordError?.let {
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль"
                                    )
                                }
                            }
                        )

                        Text(
                            text = "Забыли пароль?",
                            color = Green50,
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontFamily = montserratFamily,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }

                    // Показываем общую ошибку под полями ввода
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontFamily = montserratFamily,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { 
                                if (validateForm()) {
                                    viewModel.login(email, password)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Green50,
                                contentColor = Base0,
                                disabledContainerColor = Base20,
                                disabledContentColor = Base0
                            ),
                            enabled = isFormValid
                        ) {
                            Text(
                                text = "Войти",
                                fontFamily = montserratFamily,
                                fontSize = 16.sp,
                                lineHeight = 20.sp
                            )
                        }

                        Button(
                            onClick = onNavigateToRegister,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Base5,
                                contentColor = Base90
                            )
                        ) {
                            Text(
                                text = "У меня нет аккаунта",
                                fontFamily = montserratFamily,
                                fontSize = 16.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
            
            // Добавляем отступ снизу для скролла
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
