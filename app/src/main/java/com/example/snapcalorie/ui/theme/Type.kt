package com.example.snapcalorie.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.snapcalorie.R

val montserratFamily = FontFamily(
    Font(R.font.montserrat_light, FontWeight.Light),
    Font(R.font.montserrat_regular, FontWeight.Normal),
    Font(R.font.montserrat_medium, FontWeight.Medium),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
    Font(R.font.montserrat_bold, FontWeight.Bold)
)

val Typography = Typography(
    // H4 - Medium - 24/28
    headlineMedium = TextStyle(
        fontFamily = montserratFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 28.sp
    ),
    
    // H5 - Regular - 18/22
    headlineSmall = TextStyle(
        fontFamily = montserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 22.sp
    ),
    
    // Body M - Regular - 16/20
    bodyLarge = TextStyle(
        fontFamily = montserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 20.sp
    ),
    
    // Body M - Medium - 16/20
    bodyMedium = TextStyle(
        fontFamily = montserratFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp
    ),
    
    // Body S - Regular - 14/16
    bodySmall = TextStyle(
        fontFamily = montserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 16.sp
    ),
    
    // Body S - Medium - 14/16
    labelMedium = TextStyle(
        fontFamily = montserratFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 16.sp
    ),
    
    // Caption - Regular - 12/16
    labelSmall = TextStyle(
        fontFamily = montserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    
    // Caption - Medium - 12/16
    labelLarge = TextStyle(
        fontFamily = montserratFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)