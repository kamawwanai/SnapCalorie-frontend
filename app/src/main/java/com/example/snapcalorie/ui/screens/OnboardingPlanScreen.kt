package com.example.snapcalorie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.snapcalorie.model.OnboardingPlanResponse
import com.example.snapcalorie.ui.theme.*
import com.example.snapcalorie.ui.viewmodel.OnboardingPlanViewModel
import com.example.snapcalorie.ui.components.OnboardingTextField
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.snapcalorie.R

@Composable
fun OnboardingPlanScreen(
    viewModel: OnboardingPlanViewModel,
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var username by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(1) }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var activityLevel by remember { mutableStateOf(1) }
    var goalType by remember { mutableStateOf("loss") }
    var goalKg by remember { mutableStateOf("") }

    val onboardingResponse by viewModel.onboardingResponse.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Base0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (currentStep) {
                0 -> PersonalInfoStep(
                    username = username,
                    age = age,
                    gender = gender,
                    height = height,
                    weight = weight,
                    onUsernameChange = { username = it },
                    onAgeChange = { age = it },
                    onGenderChange = { gender = it },
                    onHeightChange = { height = it },
                    onWeightChange = { weight = it },
                    onNext = { currentStep++ }
                )
                1 -> ActivityLevelStep(
                    activityLevel = activityLevel,
                    onActivityLevelChange = { activityLevel = it },
                    onNext = { currentStep++ },
                    onBack = { currentStep-- }
                )
                2 -> GoalSettingStep(
                    goalType = goalType,
                    goalKg = goalKg,
                    onGoalTypeChange = { goalType = it },
                    onGoalKgChange = { goalKg = it },
                    onBack = { currentStep-- },
                    onComplete = {
                        val ageInt = age.toIntOrNull()
                        val heightInt = height.toIntOrNull()
                        val weightFloat = weight.toFloatOrNull()
                        val goalKgFloat = if (goalType != "maintain" && goalKg.isNotBlank()) {
                            goalKg.toFloatOrNull()
                        } else null

                        if (ageInt == null || heightInt == null || weightFloat == null ||
                            (goalType != "maintain" && goalKgFloat == null)) {
                            return@GoalSettingStep
                        }

                        viewModel.completeOnboarding(
                            username = username,
                            age = ageInt,
                            gender = gender,
                            height = heightInt,
                            weight = weightFloat,
                            activityLevel = activityLevel,
                            goalType = goalType,
                            goalKg = goalKgFloat
                        )
                        onComplete()
                    }
                )
            }
        }

        if (error != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(text = error ?: "")
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Base0.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Подготавливаем ваш план...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Base90
                    )
                }
            }
        }
    }
}

@Composable
fun PersonalInfoStep(
    username: String,
    age: String,
    gender: Int,
    height: String,
    weight: String,
    onUsernameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onGenderChange: (Int) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.End
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "Пришло время действовать!",
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    color = Base90
                )
            )
            Text(
                text = "Заполни данные о себе, чтобы получить персональный план питания и быстрее достичь результата",
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    color = Base90
                )
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OnboardingTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = "Имя пользователя",
                placeholder = "Например, ivan_ivanov",
                validate = { value -> 
                    val usernameRegex = "^[a-zA-Z0-9_]{3,20}$".toRegex()
                    usernameRegex.matches(value)
                },
                errorMessage = "Имя пользователя должно содержать от 3 до 20 символов (только латинские буквы, цифры и _)"
            )

            OnboardingTextField(
                value = age,
                onValueChange = onAgeChange,
                label = "Возраст",
                placeholder = "Введите возраст (лет)",
                keyboardType = KeyboardType.Number,
                validate = { value -> 
                    if (value.isBlank()) true
                    else try {
                        val ageNum = value.toInt()
                        ageNum in 14..100
                    } catch (e: NumberFormatException) {
                        false
                    }
                },
                errorMessage = "Возраст должен быть от 14 до 100 лет"
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Пол",
                    fontFamily = montserratFamily,
                    color = Base90
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RadioButton(
                            selected = gender == 1,
                            onClick = { onGenderChange(1) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Green50,
                                unselectedColor = Base40
                            )
                        )
                        Text(
                            text = "Мужчина",
                            fontFamily = montserratFamily,
                            color = Base90
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RadioButton(
                            selected = gender == 0,
                            onClick = { onGenderChange(0) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Green50,
                                unselectedColor = Base40
                            )
                        )
                        Text(
                            text = "Женщина",
                            fontFamily = montserratFamily,
                            color = Base90
                        )
                    }
                }
            }

            OnboardingTextField(
                value = height,
                onValueChange = onHeightChange,
                label = "Рост",
                placeholder = "Введите рост (см)",
                keyboardType = KeyboardType.Number,
                validate = { value -> 
                    if (value.isBlank()) true
                    else try {
                        val heightNum = value.toInt()
                        heightNum in 100..250
                    } catch (e: NumberFormatException) {
                        false
                    }
                },
                errorMessage = "Рост должен быть от 100 до 250 см"
            )

            OnboardingTextField(
                value = weight,
                onValueChange = onWeightChange,
                label = "Вес",
                placeholder = "Введите вес (кг)",
                keyboardType = KeyboardType.Decimal,
                validate = { value -> 
                    if (value.isBlank()) true
                    else try {
                        val weightNum = value.toFloat()
                        weightNum in 30f..300f
                    } catch (e: NumberFormatException) {
                        false
                    }
                },
                errorMessage = "Вес должен быть от 30 до 300 кг"
            )
        }

        IconButton(
            onClick = onNext,
            modifier = Modifier
                .size(52.dp)
                .background(
                    color = Green50,
                    shape = RoundedCornerShape(6.dp)
                ),
            enabled = username.isNotBlank() && 
                     age.isNotBlank() && 
                     height.isNotBlank() && 
                     weight.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.Default.NavigateNext,
                contentDescription = "Next",
                tint = if (username.isNotBlank() && 
                         age.isNotBlank() && 
                         height.isNotBlank() && 
                         weight.isNotBlank()) Base0 else Base0.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ActivityLevelStep(
    activityLevel: Int,
    onActivityLevelChange: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.End
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "Насколько вы активны на работе и в тренировках?",
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    color = Base90
                )
            )
            Text(
                text = "Оцените свой уровень активности",
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    color = Base90
                )
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActivityLevelOption(
                selected = activityLevel == 1,
                onClick = { onActivityLevelChange(1) },
                text = "Очень малая физическая активность или её нет"
            )
            ActivityLevelOption(
                selected = activityLevel == 2,
                onClick = { onActivityLevelChange(2) },
                text = "Легкая активность (3 тренировки в неделю)"
            )
            ActivityLevelOption(
                selected = activityLevel == 3,
                onClick = { onActivityLevelChange(3) },
                text = "Умеренная активность (спорт каждый день, кроме выходных)"
            )
            ActivityLevelOption(
                selected = activityLevel == 4,
                onClick = { onActivityLevelChange(4) },
                text = "Средняя активность (интенсивные тренировки, кроме выходных)"
            )
            ActivityLevelOption(
                selected = activityLevel == 5,
                onClick = { onActivityLevelChange(5) },
                text = "Спорт каждый день без выходных"
            )
            ActivityLevelOption(
                selected = activityLevel == 6,
                onClick = { onActivityLevelChange(6) },
                text = "Ежедневные интенсивные нагрузки или 2 раза в день"
            )
            ActivityLevelOption(
                selected = activityLevel == 7,
                onClick = { onActivityLevelChange(7) },
                text = "Каждый день интенсивные тренировки + тяжёлая физическая работа"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.height(52.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Base90
                )
            ) {
                Text(
                    text = "Назад",
                    fontFamily = montserratFamily,
                    fontSize = 16.sp,
                    lineHeight = 20.sp
                )
            }
            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = Green50,
                        shape = RoundedCornerShape(6.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.NavigateNext,
                    contentDescription = "Next",
                    tint = Base0
                )
            }
        }
    }
}

@Composable
fun ActivityLevelOption(
    selected: Boolean,
    onClick: () -> Unit,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Green50,
                unselectedColor = Base40
            )
        )
        Text(
            text = text,
            fontFamily = montserratFamily,
            color = Base90
        )
    }
}

@Composable
fun GoalSettingStep(
    goalType: String,
    goalKg: String,
    onGoalTypeChange: (String) -> Unit,
    onGoalKgChange: (String) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.End
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "Какая у вас главная цель?",
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    color = Base90
                )
            )
            Text(
                text = "Выберите вариант, который ближе всего к вашим планам",
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    color = Base90
                )
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = goalType == "loss",
                        onClick = { onGoalTypeChange("loss") }
                    )
                    Text("Похудеть", color = Base90)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = goalType == "maintain",
                        onClick = { onGoalTypeChange("maintain") }
                    )
                    Text("Поддерживать вес", color = Base90)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = goalType == "gain",
                        onClick = { onGoalTypeChange("gain") }
                    )
                    Text("Набрать массу", color = Base90)
                }
            }

            if (goalType != "maintain") {
                OutlinedTextField(
                    value = goalKg,
                    onValueChange = onGoalKgChange,
                    label = { 
                        Text(
                            text = "Желаемый вес (кг)",
                            fontFamily = montserratFamily,
                            color = Base40
                        )
                    },
                    textStyle = TextStyle(
                        fontFamily = montserratFamily,
                        color = Base90
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = Base90,
                        focusedTextColor = Base90,
                        unfocusedBorderColor = Base40,
                        focusedBorderColor = Green50
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.height(52.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Base90
                )
            ) {
                Text(
                    text = "Назад",
                    fontFamily = montserratFamily,
                    fontSize = 16.sp,
                    lineHeight = 20.sp
                )
            }
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .height(52.dp)
                    .widthIn(min = 200.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green50,
                    contentColor = Base0,
                    disabledContainerColor = Green50.copy(alpha = 0.5f),
                    disabledContentColor = Base0.copy(alpha = 0.5f)
                ),
                enabled = if (goalType != "maintain") goalKg.isNotBlank() else true
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Получить план",
                        fontFamily = montserratFamily,
                        fontSize = 16.sp,
                        lineHeight = 20.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Complete",
                        tint = Base0
                    )
                }
            }
        }
    }
} 