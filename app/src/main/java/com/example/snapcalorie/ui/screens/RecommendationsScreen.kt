package com.example.snapcalorie.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.snapcalorie.ui.components.NavBar
import com.example.snapcalorie.ui.components.Screen
import com.example.snapcalorie.ui.components.TopBar
import com.example.snapcalorie.ui.theme.Base0

@Composable
fun RecommendationsScreen(
    onNavigateToScreen: (Screen) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Base0
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopBar(title = "Рекомендации")
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Empty container for future content
            }
            
            NavBar(
                currentScreen = Screen.RECOMMENDATIONS,
                onScreenSelected = onNavigateToScreen
            )
        }
    }
} 