package com.example.snapcalorie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.snapcalorie.ui.theme.*

@Composable
fun AddFoodDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    onEnterManually: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Semi-transparent background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF868686).copy(alpha = 0.2f))
            )
            
            // Dialog content
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                color = Base0
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header with title and close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "Добавить прием пищи",
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Normal,
                            color = Base90
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Base70
                            )
                        }
                    }

                    // Options section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Выберите способ",
                            fontSize = 14.sp,
                            lineHeight = 16.sp,
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Normal,
                            color = Base70
                        )
                        
                        // Buttons
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ButtonS(
                                onClick = onTakePhoto,
                                text = "Сделать фото",
                                type = ButtonType.PRIMARY,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            ButtonS(
                                onClick = onPickFromGallery,
                                text = "Выбрать из галереи",
                                type = ButtonType.SECONDARY,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            ButtonS(
                                onClick = onEnterManually,
                                text = "Ввести данные вручную",
                                type = ButtonType.SECONDARY,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
} 