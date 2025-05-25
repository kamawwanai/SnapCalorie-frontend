package com.example.snapcalorie.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snapcalorie.R
import com.example.snapcalorie.ui.theme.Base0
import com.example.snapcalorie.ui.theme.montserratFamily

@Composable
fun AppIcon(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_logo_72),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        
        Text(
            text = "SnapCalorie",
            fontFamily = montserratFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp,
            color = Base0
        )
    }
} 