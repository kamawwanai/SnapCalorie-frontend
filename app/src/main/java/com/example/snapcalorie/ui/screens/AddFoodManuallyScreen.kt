package com.example.snapcalorie.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snapcalorie.model.MealRecordCreate
import com.example.snapcalorie.network.ApiModule
import com.example.snapcalorie.storage.TokenStorage
import com.example.snapcalorie.ui.components.ButtonS
import com.example.snapcalorie.ui.components.ButtonType
import com.example.snapcalorie.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodManuallyScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Состояния для полей ввода
    var calories by remember { mutableStateOf("") }
    var proteins by remember { mutableStateOf("") }
    var fats by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    
    // Состояния для выбора типа блюда
    var selectedMealType by remember { mutableStateOf<String?>(null) }
    var selectedMealTypeValue by remember { mutableStateOf<Int?>(null) }
    var showMealTypeDropdown by remember { mutableStateOf(false) }
    
    // Состояния для обработки отправки
    var isSending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    
    val mealTypes = listOf(
        "Завтрак" to 1,
        "Утренний перекус" to 2,
        "Обед" to 3,
        "Дневной перекус" to 4,
        "Ужин" to 5,
        "Вечерний перекус" to 6,
        "Тренировка" to 7,
        "Другое" to 8
    )
    
    // Функция валидации
    fun validateInputs(): Boolean {
        return calories.isNotBlank() && 
               proteins.isNotBlank() && 
               fats.isNotBlank() && 
               carbs.isNotBlank() && 
               selectedMealType != null &&
               try {
                   calories.toDouble() >= 0 &&
                   proteins.toDouble() >= 0 &&
                   fats.toDouble() >= 0 &&
                   carbs.toDouble() >= 0
               } catch (e: NumberFormatException) {
                   false
               }
    }
    
    // Функция отправки данных
    fun sendMealData() {
        scope.launch {
            try {
                isSending = true
                error = null
                
                val tokenStorage = TokenStorage(context)
                val apiService = ApiModule.provideApiService { tokenStorage.token }
                
                // Создаем запись о приеме пищи
                val currentDateTime = LocalDateTime.now()
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                
                val mealRecord = MealRecordCreate(
                    datetime = currentDateTime.format(formatter),
                    calories = calories.toDouble(),
                    proteins = proteins.toDouble(),
                    fats = fats.toDouble(),
                    carbs = carbs.toDouble(),
                    meal_type = selectedMealTypeValue ?: 8,
                    image_path = null
                )
                
                // Отправляем на сервер
                val authHeader = "Bearer ${tokenStorage.token}"
                apiService.createMealRecord(authHeader, mealRecord)
                
                // Показываем сообщение об успехе и возвращаемся назад
                showSuccessMessage = true
                kotlinx.coroutines.delay(1500) // Показываем сообщение 1.5 секунды
                onNavigateBack()
                
            } catch (e: Exception) {
                error = "Ошибка при сохранении: ${e.message}"
            } finally {
                isSending = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Base0
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // TopBar
            TopAppBar(
                title = {
                    Text(
                        text = "Новый прием пищи",
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontFamily = montserratFamily,
                        fontWeight = FontWeight.Normal,
                        color = Base90
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Back",
                            tint = Base50
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Base0
                )
            )

            // Content Area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Выбор типа блюда
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedMealType ?: "",
                        onValueChange = { },
                        label = { 
                            Text(
                                "Тип приема пищи",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontSize = 14.sp
                                )
                            ) 
                        },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showMealTypeDropdown = true }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Выбрать тип"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Green50,
                            focusedLabelColor = Green50
                        ),
                        textStyle = TextStyle(
                            fontFamily = montserratFamily,
                            fontSize = 14.sp
                        )
                    )
                    
                    DropdownMenu(
                        expanded = showMealTypeDropdown,
                        onDismissRequest = { showMealTypeDropdown = false }
                    ) {
                        mealTypes.forEach { (name, value) ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        name,
                                        style = TextStyle(
                                            fontFamily = montserratFamily,
                                            fontSize = 14.sp
                                        )
                                    ) 
                                },
                                onClick = {
                                    selectedMealType = name
                                    selectedMealTypeValue = value
                                    showMealTypeDropdown = false
                                }
                            )
                        }
                    }
                }
                
                // Input Fields
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { newValue ->
                            // Разрешаем только цифры и точку
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                calories = newValue
                            }
                        },
                        label = { 
                            Text(
                                "Калорийность (ккал)",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontSize = 14.sp
                                )
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(
                            fontFamily = montserratFamily,
                            fontSize = 14.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Green50,
                            focusedLabelColor = Green50
                        )
                    )
                    
                    OutlinedTextField(
                        value = proteins,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                proteins = newValue
                            }
                        },
                        label = { 
                            Text(
                                "Белки (г)",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontSize = 14.sp
                                )
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(
                            fontFamily = montserratFamily,
                            fontSize = 14.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Green50,
                            focusedLabelColor = Green50
                        )
                    )
                    
                    OutlinedTextField(
                        value = fats,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                fats = newValue
                            }
                        },
                        label = { 
                            Text(
                                "Жиры (г)",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontSize = 14.sp
                                )
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(
                            fontFamily = montserratFamily,
                            fontSize = 14.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Green50,
                            focusedLabelColor = Green50
                        )
                    )
                    
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                carbs = newValue
                            }
                        },
                        label = { 
                            Text(
                                "Углеводы (г)",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontSize = 14.sp
                                )
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(
                            fontFamily = montserratFamily,
                            fontSize = 14.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Green50,
                            focusedLabelColor = Green50
                        )
                    )
                }

                // Error message
                if (error != null) {
                    Text(
                        text = error!!,
                        style = TextStyle(
                            fontFamily = montserratFamily,
                            fontSize = 14.sp,
                            color = Red50
                        )
                    )
                }
                
                // Success message
                if (showSuccessMessage) {
                    Text(
                        text = "Прием пищи успешно добавлен!",
                        style = TextStyle(
                            fontFamily = montserratFamily,
                            fontSize = 14.sp,
                            color = Green50
                        )
                    )
                }

                // Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { sendMealData() },
                        enabled = validateInputs() && !isSending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green50,
                            contentColor = Color.White,
                            disabledContainerColor = Base20,
                            disabledContentColor = Base50
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Добавить",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }
                    
                    OutlinedButton(
                        onClick = onNavigateBack,
                        enabled = !isSending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Green50
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Отмена",
                            style = TextStyle(
                                fontFamily = montserratFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }
} 