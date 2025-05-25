package com.example.snapcalorie.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.snapcalorie.R
import com.example.snapcalorie.network.ApiService
import com.example.snapcalorie.ui.components.NavBar
import com.example.snapcalorie.ui.components.Screen
import com.example.snapcalorie.ui.components.TopBar
import com.example.snapcalorie.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Meal(
    val id: Int,
    val userId: Int,
    val datetime: String,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
    val mealType: Int,
    val imagePath: String?
)

data class MealGroup(
    val date: String,
    val totalCalories: Double,
    val meals: List<Meal>
)

enum class MealType(val value: Int) {
    BREAKFAST(1),
    MORNING_SNACK(2),
    LUNCH(3),
    AFTERNOON_SNACK(4),
    DINNER(5),
    EVENING_SNACK(6),
    WORKOUT(7),
    OTHER(8)
}

fun getMealTypeName(type: Int): String {
    return when (type) {
        MealType.BREAKFAST.value -> "Завтрак"
        MealType.MORNING_SNACK.value -> "Утренний перекус"
        MealType.LUNCH.value -> "Обед"
        MealType.AFTERNOON_SNACK.value -> "Дневной перекус"
        MealType.DINNER.value -> "Ужин"
        MealType.EVENING_SNACK.value -> "Вечерний перекус"
        MealType.WORKOUT.value -> "Тренировка"
        MealType.OTHER.value -> "Другое"
        else -> "Неизвестно"
    }
}

fun formatTime(datetime: String): String {
    val formatter = DateTimeFormatter.ISO_DATE_TIME
    val dateTime = LocalDateTime.parse(datetime, formatter)
    return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
}

@Composable
fun MyDiaryScreen(
    onNavigateToScreen: (Screen) -> Unit,
    onNavigateToAddFoodManually: () -> Unit,
    onNavigateToImageSegmentation: (Uri) -> Unit,
    apiService: ApiService,
    authToken: String
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var mealGroups by remember { mutableStateOf<List<MealGroup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            onNavigateToImageSegmentation(it)
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                mealGroups = apiService.getGroupedMeals("Bearer $authToken")
                error = null
            } catch (e: Exception) {
                error = e.message ?: "Failed to load meals"
            } finally {
                isLoading = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopBar(
                title = "Мой дневник",
                onTakePhoto = {
                    onNavigateToScreen(Screen.CAMERA)
                },
                onPickFromGallery = {
                    galleryLauncher.launch("image/*")
                },
                onEnterManually = {
                    onNavigateToAddFoodManually()
                }
            )
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Загрузка...",
                        style = TextStyle(
                            fontFamily = montserratFamily,
                            fontSize = 16.sp,
                            color = Base70
                        )
                    )
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = error ?: "Произошла ошибка",
                        style = TextStyle(
                            fontFamily = montserratFamily,
                            fontSize = 16.sp,
                            color = Base70
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(mealGroups) { group ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = group.date,
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontSize = 16.sp,
                                    lineHeight = 20.sp,
                                    color = Green50
                                )
                            )

                            group.meals.forEach { meal ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Base5,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        ) {
                                            val imageUrl = if (meal.imagePath != null) {
                                                "http://localhost:8000/static/${meal.imagePath}"
                                            } else {
                                                null
                                            }
                                            if (imageUrl != null) {
                                                Image(
                                                    painter = rememberAsyncImagePainter(
                                                        ImageRequest.Builder(LocalContext.current)
                                                            .data(imageUrl)
                                                            .build()
                                                    ),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Image(
                                                    painter = painterResource(id = R.drawable.img_base_meal),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .wrapContentHeight(),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = getMealTypeName(meal.mealType),
                                                    style = TextStyle(
                                                        fontFamily = montserratFamily,
                                                        fontSize = 14.sp,
                                                        lineHeight = 16.sp,
                                                        color = Base90
                                                    )
                                                )
                                                Text(
                                                    text = formatTime(meal.datetime),
                                                    style = TextStyle(
                                                        fontFamily = montserratFamily,
                                                        fontSize = 12.sp,
                                                        lineHeight = 16.sp,
                                                        color = Base70
                                                    )
                                                )
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Калорийность(ккал)",
                                                    style = TextStyle(
                                                        fontFamily = montserratFamily,
                                                        fontSize = 14.sp,
                                                        lineHeight = 16.sp,
                                                        color = Base90
                                                    )
                                                )
                                                Text(
                                                    text = meal.calories.toString(),
                                                    style = TextStyle(
                                                        fontFamily = montserratFamily,
                                                        fontSize = 14.sp,
                                                        lineHeight = 16.sp,
                                                        color = Green70
                                                    )
                                                )
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Белки(г)",
                                                    style = TextStyle(
                                                        fontFamily = montserratFamily,
                                                        fontSize = 14.sp,
                                                        lineHeight = 16.sp,
                                                        color = Base90
                                                    )
                                                )
                                                Text(
                                                    text = meal.proteins.toString(),
                                                    style = TextStyle(
                                                        fontFamily = montserratFamily,
                                                        fontSize = 14.sp,
                                                        lineHeight = 16.sp,
                                                        color = Green70
                                                    )
                                                )
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Жиры(г)",
                                                    style = TextStyle(
                                                        fontFamily = montserratFamily,
                                                        fontSize = 14.sp,
                                                        lineHeight = 16.sp,
                                                        color = Base90
                                                    )
                                                )
                                                Text(
                                                    text = meal.fats.toString(),
                                                    style = TextStyle(
                                                        fontFamily = montserratFamily,
                                                        fontSize = 14.sp,
                                                        lineHeight = 16.sp,
                                                        color = Green70
                                                    )
                                                )
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Углеводы(г)",
                                                    style = TextStyle(
                                                        fontFamily = montserratFamily,
                                                        fontSize = 14.sp,
                                                        lineHeight = 16.sp,
                                                        color = Base90
                                                    )
                                                )
                                                Text(
                                                    text = meal.carbs.toString(),
                                                    style = TextStyle(
                                                        fontFamily = montserratFamily,
                                                        fontSize = 14.sp,
                                                        lineHeight = 16.sp,
                                                        color = Green70
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Всего за день",
                                    style = TextStyle(
                                        fontFamily = montserratFamily,
                                        fontSize = 16.sp,
                                        lineHeight = 20.sp,
                                        color = Orange50
                                    )
                                )
                                Text(
                                    text = group.totalCalories.toString(),
                                    style = TextStyle(
                                        fontFamily = montserratFamily,
                                        fontSize = 16.sp,
                                        lineHeight = 20.sp,
                                        color = Base90
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            NavBar(
                currentScreen = Screen.DIARY,
                onScreenSelected = onNavigateToScreen
            )
        }
    }
} 