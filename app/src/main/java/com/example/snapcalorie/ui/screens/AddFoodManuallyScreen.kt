package com.example.snapcalorie.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snapcalorie.ui.components.ButtonS
import com.example.snapcalorie.ui.components.ButtonType
import com.example.snapcalorie.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodManuallyScreen(
    onNavigateBack: () -> Unit
) {
    var calories by remember { mutableStateOf("") }
    var proteins by remember { mutableStateOf("") }
    var fats by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
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
                }
            )

            // Content Area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Input Fields
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it },
                        label = { Text("Калорийность (ккал)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = montserratFamily,
                            fontSize = 14.sp
                        )
                    )
                    
                    OutlinedTextField(
                        value = proteins,
                        onValueChange = { proteins = it },
                        label = { Text("Белки (г)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = montserratFamily,
                            fontSize = 14.sp
                        )
                    )
                    
                    OutlinedTextField(
                        value = fats,
                        onValueChange = { fats = it },
                        label = { Text("Жиры (г)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = montserratFamily,
                            fontSize = 14.sp
                        )
                    )
                    
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text("Углеводы (г)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = montserratFamily,
                            fontSize = 14.sp
                        )
                    )
                }

                // Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ButtonS(
                        onClick = { /* Handle add */ },
                        text = "Добавить",
                        type = ButtonType.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    ButtonS(
                        onClick = onNavigateBack,
                        text = "Отмена",
                        type = ButtonType.SECONDARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
} 