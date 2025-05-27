package com.example.snapcalorie.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.snapcalorie.ui.components.NavBar
import com.example.snapcalorie.ui.components.Screen
import androidx.compose.ui.unit.dp
import com.example.snapcalorie.ui.theme.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background

@Composable
fun CameraScreen(
    onNavigateToScreen: (Screen) -> Unit,
    onNavigateToPhotoCapture: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Base0),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Main content area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Выберите способ анализа блюда",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Base90,
                fontFamily = montserratFamily
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onNavigateToScreen(Screen.AR_CAMERA_TOP) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green50,
                        contentColor = Base0
                    )
                ) {
                    Text(
                        text = "3D сканирование блюда",
                        fontFamily = montserratFamily,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                OutlinedButton(
                    onClick = { onNavigateToPhotoCapture() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Green50
                    )
                ) {
                    Text(
                        text = "Обычное фото",
                        fontFamily = montserratFamily,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // NavBar at the bottom
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Base0
        ) {
            NavBar(
                currentScreen = Screen.CAMERA,
                onScreenSelected = onNavigateToScreen
            )
        }
    }
} 