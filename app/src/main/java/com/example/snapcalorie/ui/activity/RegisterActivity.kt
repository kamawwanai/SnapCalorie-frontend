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
import androidx.compose.ui.graphics.Color
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

// Функции валидации
private fun isEmailValid(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

private fun isPasswordValid(password: String): Boolean {
    val passwordRegex = "^[a-zA-Z0-9]{8,}$".toRegex()
    return passwordRegex.matches(password)
}

class RegisterActivity : ComponentActivity() {

    private lateinit var tokenStorage: TokenStorage
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
                RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSuccess = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    },
                    onNavigateToLogin = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    // Состояния для отслеживания первого ввода
    var isEmailTouched by remember { mutableStateOf(false) }
    var isPasswordTouched by remember { mutableStateOf(false) }
    var isConfirmPasswordTouched by remember { mutableStateOf(false) }
    
    // Состояния для ошибок валидации
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    
    // Состояние валидности формы
    var isFormValid by remember { mutableStateOf(false) }
    
    val error by viewModel.error.observeAsState()
    val isRegistrationSuccessful by viewModel.isRegistrationSuccessful.observeAsState()

    // Функция для очистки ошибки аутентификации
    fun clearAuthError() {
        if (error?.contains("Authentication failed") == true) {
            viewModel.clearError()
        }
    }

    // Преобразование ошибки в понятный пользователю текст
    val errorMessage = when (error) {
        "Authentication failed. Please check your credentials." -> "Ошибка регистрации. Пользователь с таким email уже существует"
        "Cannot connect to server. Please check your internet connection." -> "Не удалось подключиться к серверу. Проверьте подключение к интернету"
        "Connection failed. Server might be down." -> "Ошибка подключения. Сервер может быть недоступен"
        "Connection timed out. Please try again." -> "Превышено время ожидания. Попробуйте снова"
        else -> error
    }

    // Показываем ошибку регистрации в полях
    val showAuthError = error?.contains("Authentication failed") == true

    // Функция валидации формы
    fun validateForm(): Boolean {
        var isValid = true
        
        // Проверка email только если поле было тронуто
        if (isEmailTouched) {
            if (email.isEmpty()) {
                emailError = "Email не может быть пустым"
                isValid = false
            } else if (!isEmailValid(email)) {
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
            } else if (!isPasswordValid(password)) {
                passwordError = "Пароль должен содержать минимум 8 символов, только латинские буквы и цифры"
                isValid = false
            } else {
                passwordError = null
            }
        }
        
        // Проверка подтверждения пароля только если поле было тронуто
        if (isConfirmPasswordTouched) {
            if (confirmPassword.isEmpty()) {
                confirmPasswordError = "Подтвердите пароль"
                isValid = false
            } else if (password != confirmPassword) {
                confirmPasswordError = "Пароли не совпадают"
                isValid = false
            } else {
                confirmPasswordError = null
            }
        }
        
        // Форма валидна только если все поля заполнены корректно
        isFormValid = email.isNotEmpty() && 
                     isEmailValid(email) && 
                     password.isNotEmpty() && 
                     isPasswordValid(password) && 
                     confirmPassword.isNotEmpty() && 
                     password == confirmPassword
        
        return isValid
    }

    // Эффект для валидации при изменении полей
    LaunchedEffect(email, password, confirmPassword) {
        validateForm()
    }

    // При успешной регистрации переходим на экран входа
    LaunchedEffect(isRegistrationSuccessful) {
        if (isRegistrationSuccessful == true) {
            onRegisterSuccess()
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

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { 
                                confirmPassword = it
                                isConfirmPasswordTouched = true
                                validateForm()
                                clearAuthError()
                            },
                            label = { Text("Повторите пароль") },
                            placeholder = { Text("Введите пароль ещё раз") },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = confirmPasswordError != null || showAuthError,
                            supportingText = {
                                confirmPasswordError?.let {
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (confirmPasswordVisible) "Скрыть пароль" else "Показать пароль"
                                    )
                                }
                            }
                        )
                    }

                    // Buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { 
                                if (validateForm()) {
                                    viewModel.register(email, password)
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
                                text = "Зарегистрироваться",
                                fontFamily = montserratFamily,
                                fontSize = 16.sp,
                                lineHeight = 20.sp
                            )
                        }

                        Button(
                            onClick = onNavigateToLogin,
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
                                text = "У меня есть аккаунт",
                                fontFamily = montserratFamily,
                                fontSize = 16.sp,
                                lineHeight = 20.sp
                            )
                        }
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
                }
            }
            
            // Добавляем отступ снизу для скролла
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
