package com.example.snapcalorie.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.snapcalorie.ui.components.OnboardingTextField
import com.example.snapcalorie.ui.theme.*
import com.example.snapcalorie.viewmodel.OnboardingPlanViewModel

@Composable
fun GoalSelectionScreen(
    viewModel: OnboardingPlanViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var isGoalKgValid by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Какая у вас главная цель?",
                fontSize = 24.sp,
                lineHeight = 29.sp,
                fontFamily = montserratFamily,
                fontWeight = FontWeight.SemiBold,
                color = Base90
            )

            Text(
                text = "Выберите вариант, который ближе всего к вашим планам",
                fontFamily = montserratFamily,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                color = Base90
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Goal type selection
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "loss" to "Снижение веса",
                        "maintain" to "Поддержание веса",
                        "gain" to "Набор веса"
                    ).forEach { (type, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = viewModel.goalType == type,
                                onClick = { 
                                    viewModel.goalType = type
                                    if (type == "maintain") {
                                        viewModel.goalKg = ""
                                        isGoalKgValid = true
                                    }
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Green50,
                                    unselectedColor = Base40
                                )
                            )
                            Text(
                                text = label,
                                fontFamily = montserratFamily,
                                color = Base90
                            )
                        }
                    }
                }

                // Goal weight field (only shown if not maintaining)
                if (viewModel.goalType != "maintain") {
                    OnboardingTextField(
                        value = viewModel.goalKg,
                        onValueChange = { 
                            viewModel.goalKg = it
                            isGoalKgValid = try {
                                val weightFloat = it.toFloat()
                                weightFloat in 0f..500f
                            } catch (e: NumberFormatException) {
                                false
                            }
                        },
                        label = "Желаемый вес",
                        placeholder = "Введите желаемый вес (кг)",
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
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Base90
                )
            ) {
                Text(
                    text = "Назад",
                    fontFamily = montserratFamily
                )
            }

            Button(
                onClick = onNext,
                enabled = viewModel.goalType == "maintain" || 
                         (viewModel.goalKg.isNotEmpty() && isGoalKgValid),
                modifier = Modifier.width(200.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green50,
                    contentColor = Base0,
                    disabledContainerColor = Base20,
                    disabledContentColor = Base40
                )
            ) {
                Text(
                    text = "Получить план",
                    fontFamily = montserratFamily
                )
            }
        }
    }
} 