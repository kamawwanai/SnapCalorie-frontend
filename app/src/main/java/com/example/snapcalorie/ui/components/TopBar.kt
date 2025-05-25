package com.example.snapcalorie.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snapcalorie.ui.theme.Base90
import com.example.snapcalorie.ui.theme.Green50
import com.example.snapcalorie.ui.theme.montserratFamily

@Composable
fun TopBar(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    onTakePhoto: () -> Unit = {},
    onPickFromGallery: () -> Unit = {},
    onEnterManually: () -> Unit = {}
) {
    var showAddFoodDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontFamily = montserratFamily,
            fontWeight = FontWeight.Normal,
            color = Base90,
            modifier = Modifier.weight(1f)
        )
        
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions()
            if (onTakePhoto != {}) {
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = { showAddFoodDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Green50
                    )
                }
            }
        }
    }

    if (showAddFoodDialog) {
        AddFoodDialog(
            onDismiss = { showAddFoodDialog = false },
            onTakePhoto = {
                showAddFoodDialog = false
                onTakePhoto()
            },
            onPickFromGallery = {
                showAddFoodDialog = false
                onPickFromGallery()
            },
            onEnterManually = {
                showAddFoodDialog = false
                onEnterManually()
            }
        )
    }
} 