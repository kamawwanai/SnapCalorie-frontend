package com.example.snapcalorie.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snapcalorie.ui.theme.*

enum class ButtonType {
    PRIMARY,
    SECONDARY
}

@Composable
fun ButtonS(
    onClick: () -> Unit,
    text: String,
    type: ButtonType,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        colors = when (type) {
            ButtonType.PRIMARY -> ButtonDefaults.buttonColors(
                containerColor = Green50,
                contentColor = Base0,
                disabledContainerColor = Green50.copy(alpha = 0.5f),
                disabledContentColor = Base0.copy(alpha = 0.5f)
            )
            ButtonType.SECONDARY -> ButtonDefaults.buttonColors(
                containerColor = Base10,
                contentColor = Base90,
                disabledContainerColor = Base10.copy(alpha = 0.5f),
                disabledContentColor = Base90.copy(alpha = 0.5f)
            )
        }
    ) {
        Text(
            text = text,
            fontFamily = montserratFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
} 