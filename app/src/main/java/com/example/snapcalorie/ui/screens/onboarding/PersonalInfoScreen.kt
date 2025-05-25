package com.example.snapcalorie.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import com.example.snapcalorie.ui.components.OnboardingTextField
import com.example.snapcalorie.ui.theme.*
import com.example.snapcalorie.viewmodel.OnboardingPlanViewModel

@Composable
fun PersonalInfoScreen(
    viewModel: OnboardingPlanViewModel,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header texts
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Пришло время действовать!",
                    fontFamily = montserratFamily,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = Base90
                )
                Text(
                    text = "Заполни данные о себе, чтобы получить персональный план питания и быстрее достичь результата",
                    fontFamily = montserratFamily,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    color = Base90
                )
            }

            // Input fields
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OnboardingTextField(
                    value = viewModel.username,
                    onValueChange = { viewModel.username = it },
                    label = "Имя пользователя",
                    placeholder = "Например, ivan_ivanov",
                    validate = { username -> 
                        username.length in 3..50 && username.matches(Regex("^[a-zA-Z0-9_-]+$"))
                    },
                    errorMessage = "3-50 символов, только a-z, A-Z, 0-9, _ и -"
                )

                OnboardingTextField(
                    value = viewModel.age,
                    onValueChange = { viewModel.age = it },
                    label = "Возраст",
                    placeholder = "Введите возраст (лет)",
                    keyboardType = KeyboardType.Number,
                    validate = { age ->
                        try {
                            val ageInt = age.toInt()
                            ageInt in 0..150
                        } catch (e: NumberFormatException) {
                            false
                        }
                    },
                    errorMessage = "Возраст должен быть от 0 до 150 лет"
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Пол",
                        fontFamily = montserratFamily,
                        color = Base40
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = viewModel.gender == 1,
                                onClick = { viewModel.gender = 1 },
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = viewModel.gender == 0,
                                onClick = { viewModel.gender = 0 },
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
                    value = viewModel.height,
                    onValueChange = { viewModel.height = it },
                    label = "Рост",
                    placeholder = "Введите рост (см)",
                    keyboardType = KeyboardType.Number,
                    validate = { height ->
                        try {
                            val heightInt = height.toInt()
                            heightInt in 0..300
                        } catch (e: NumberFormatException) {
                            false
                        }
                    },
                    errorMessage = "Рост должен быть от 0 до 300 см"
                )

                OnboardingTextField(
                    value = viewModel.weight,
                    onValueChange = { viewModel.weight = it },
                    label = "Вес",
                    placeholder = "Введите вес (кг)",
                    keyboardType = KeyboardType.Decimal,
                    validate = { weight ->
                        try {
                            val weightFloat = weight.toFloat()
                            weightFloat in 0f..500f
                        } catch (e: NumberFormatException) {
                            false
                        }
                    },
                    errorMessage = "Вес должен быть от 0 до 500 кг"
                )
            }
        }

        // Next button
        FilledIconButton(
            onClick = onNext,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Green50,
                contentColor = Base0
            ),
            modifier = Modifier
                .align(Alignment.Start)
                .size(52.dp)
                .background(
                    color = Green50,
                    shape = RoundedCornerShape(6.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Next"
            )
        }
    }
} 