package com.example.snapcalorie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.snapcalorie.ui.theme.*
import com.example.snapcalorie.util.NutritionCalculationResult

@Composable
fun NutritionResultDialog(
    mealTypeName: String,
    nutritionResult: NutritionCalculationResult,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
    isSending: Boolean = false
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Заголовок
                Text(
                    text = "Результат расчета",
                    style = TextStyle(
                        fontFamily = montserratFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = Base90
                    )
                )
                
                // Тип блюда
                Text(
                    text = "Тип приема пищи: $mealTypeName",
                    style = TextStyle(
                        fontFamily = montserratFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        color = Base70
                    )
                )
                
                // Результаты КБЖУ
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NutritionRow(
                        label = "Калории",
                        value = String.format("%.1f ккал", nutritionResult.calories),
                        color = Orange50
                    )
                    
                    NutritionRow(
                        label = "Белки",
                        value = String.format("%.1f г", nutritionResult.proteins),
                        color = Green50
                    )
                    
                    NutritionRow(
                        label = "Жиры",
                        value = String.format("%.1f г", nutritionResult.fats),
                        color = Blue50
                    )
                    
                    NutritionRow(
                        label = "Углеводы",
                        value = String.format("%.1f г", nutritionResult.carbohydrates),
                        color = Purple50
                    )
                }
                
                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Base70
                        ),
                        shape = RoundedCornerShape(8.dp)
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
                    
                    Button(
                        onClick = onSend,
                        modifier = Modifier.weight(1f),
                        enabled = !isSending,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green50,
                            contentColor = Color.White,
                            disabledContainerColor = Base20,
                            disabledContentColor = Base50
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Отправить",
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
}

@Composable
private fun NutritionRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Base5,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = color,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = Base90
                )
            )
        }
        
        Text(
            text = value,
            style = TextStyle(
                fontFamily = montserratFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Base90
            )
        )
    }
} 