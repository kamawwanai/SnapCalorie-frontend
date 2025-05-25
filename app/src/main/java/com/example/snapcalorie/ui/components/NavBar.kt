package com.example.snapcalorie.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.snapcalorie.ui.theme.Green10
import com.example.snapcalorie.ui.theme.Green50

enum class Screen {
    DIARY, STATS, CAMERA, RECOMMENDATIONS, PROFILE, IMAGE_SEGMENTATION
}

@Composable
fun NavBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavBarItem(
            icon = Icons.Default.Book,
            isSelected = currentScreen == Screen.DIARY,
            onClick = { onScreenSelected(Screen.DIARY) }
        )
        NavBarItem(
            icon = Icons.Default.BarChart,
            isSelected = currentScreen == Screen.STATS,
            onClick = { onScreenSelected(Screen.STATS) }
        )
        NavBarItem(
            icon = Icons.Default.PhotoCamera,
            isSelected = currentScreen == Screen.CAMERA,
            onClick = { onScreenSelected(Screen.CAMERA) }
        )
        NavBarItem(
            icon = Icons.Default.Recommend,
            isSelected = currentScreen == Screen.RECOMMENDATIONS,
            onClick = { onScreenSelected(Screen.RECOMMENDATIONS) }
        )
        NavBarItem(
            icon = Icons.Default.Person,
            isSelected = currentScreen == Screen.PROFILE,
            onClick = { onScreenSelected(Screen.PROFILE) }
        )
    }
}

@Composable
private fun NavBarItem(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) Green50 else Green10
        )
    }
} 