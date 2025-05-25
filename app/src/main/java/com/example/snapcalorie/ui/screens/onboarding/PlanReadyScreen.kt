package com.example.snapcalorie.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.snapcalorie.ui.theme.*
import com.example.snapcalorie.viewmodel.OnboardingPlanViewModel
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.snapcalorie.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background

@Composable
fun PlanReadyScreen(
    viewModel: OnboardingPlanViewModel,
    onFinish: () -> Unit
) {
    val nutritionPlan = viewModel.onboardingResponse?.nutrition_plan

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Base0),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_ready_plan_back),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(Base0, RoundedCornerShape(22.dp))
                .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Header container
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Поздравляем, ваш план питание готов!",
                    fontFamily = montserratFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    color = Base90
                )
                Text(
                    text = "На основе ваших данных мы рассчитали суточную норму калорий и макросов, чтобы вы уверенно движлись к цели.",
                    fontFamily = montserratFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    color = Base90
                )
            }

            // 2. Daily plan container
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ваш план на день",
                    fontFamily = montserratFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    color = Green50
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Калорийность (ккал)",
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 16.sp,
                            color = Base90
                        )
                        Text(
                            text = nutritionPlan?.calories_per_day?.toString() ?: "-",
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 16.sp,
                            color = Green70
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Белки (г)",
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 16.sp,
                            color = Base90
                        )
                        Text(
                            text = nutritionPlan?.protein_g?.toString() ?: "-",
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 16.sp,
                            color = Green70
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Жиры (г)",
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 16.sp,
                            color = Base90
                        )
                        Text(
                            text = nutritionPlan?.fat_g?.toString() ?: "-",
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 16.sp,
                            color = Green70
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Углеводы (г)",
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 16.sp,
                            color = Base90
                        )
                        Text(
                            text = nutritionPlan?.carb_g?.toString() ?: "-",
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 16.sp,
                            color = Green70
                        )
                    }
                }
            }

            // 3. Goal container
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Цель",
                    fontFamily = montserratFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    color = Green50
                )
                Text(
                    text = nutritionPlan?.smart_goal ?: "",
                    fontFamily = montserratFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    color = Base90
                )
            }

            // Start button
            Button(
                onClick = {
                    viewModel.finishOnboarding()
                    onFinish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green50,
                    contentColor = Base0
                )
            ) {
                Text(
                    text = "Начать",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = montserratFamily,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
} 