package com.example.snapcalorie.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.example.snapcalorie.ui.theme.montserratFamily

@Composable
fun StatsScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Статистика",
            style = TextStyle(
                fontFamily = montserratFamily,
                fontSize = 20.sp
            )
        )
    }
} 