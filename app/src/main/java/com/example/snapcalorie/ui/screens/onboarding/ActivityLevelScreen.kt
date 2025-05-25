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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import com.example.snapcalorie.ui.theme.*
import com.example.snapcalorie.viewmodel.OnboardingPlanViewModel

@Composable
fun ActivityLevelScreen(
    viewModel: OnboardingPlanViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit
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
                    text = "Насколько вы активны на работе и в тренировках?",
                    fontFamily = montserratFamily,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = Base90
                )
                Text(
                    text = "Оцените свой уровень активности",
                    fontFamily = montserratFamily,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    color = Base90
                )
            }

            // Activity level selection
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val activityLevels = listOf(
                    "Очень малая физическая активность или её нет" to 1,
                    "Легкая активность (3 тренировки в неделю)" to 2,
                    "Умеренная активность (спорт каждый день, кроме выходных)" to 3,
                    "Средняя активность (интенсивные тренировки, кроме выходных)" to 4,
                    "Спорт каждый день без выходных" to 5,
                    "Ежедневные интенсивные нагрузки или 2 раза в день" to 6,
                    "Каждый день интенсивные тренировки + тяжёлая физическая работа" to 7
                )

                activityLevels.forEach { (description, level) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = viewModel.activityLevel == level,
                            onClick = { viewModel.activityLevel = level },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Green50,
                                unselectedColor = Base40
                            )
                        )
                        Text(
                            text = description,
                            fontFamily = montserratFamily,
                            color = Base90
                        )
                    }
                }
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.height(52.dp)
            ) {
                Text(
                    text = "Назад",
                    fontFamily = montserratFamily,
                    color = Base90
                )
            }

            FilledIconButton(
                onClick = onNext,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Green50,
                    contentColor = Base0
                ),
                modifier = Modifier
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
} 