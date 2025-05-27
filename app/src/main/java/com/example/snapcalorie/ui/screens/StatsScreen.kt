package com.example.snapcalorie.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snapcalorie.model.NutritionPlan
import com.example.snapcalorie.network.ApiService
import com.example.snapcalorie.ui.components.Screen
import com.example.snapcalorie.ui.components.TopBar
import com.example.snapcalorie.ui.components.NavBar
import com.example.snapcalorie.ui.screens.MealGroup
import com.example.snapcalorie.ui.theme.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*
import kotlin.math.abs

data class DailyCalorieData(
    val date: String,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
    val formattedDate: String,
    val dayOfWeek: String
)

data class WeeklyStats(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val dailyData: List<DailyCalorieData>,
    val averageCalories: Double,
    val averageProteins: Double,
    val averageFats: Double,
    val averageCarbs: Double
)

@Composable
fun StatsScreen(
    onNavigateToScreen: (Screen) -> Unit,
    apiService: ApiService,
    authToken: String
) {
    var mealGroups by remember { mutableStateOf<List<MealGroup>>(emptyList()) }
    var nutritionPlan by remember { mutableStateOf<NutritionPlan?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentWeekOffset by remember { mutableStateOf(0) } // 0 = текущая неделя, -1 = прошлая неделя, +1 = следующая неделя
    
    val scope = rememberCoroutineScope()
    
    // Загружаем данные при запуске экрана
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                error = null
                
                // Загружаем данные о приемах пищи и план питания параллельно
                val mealsDeferred = async { apiService.getGroupedMeals("Bearer $authToken") }
                val planDeferred = async { apiService.getCurrentPlan("Bearer $authToken") }
                
                val mealsResponse = mealsDeferred.await()
                nutritionPlan = planDeferred.await()
                
                // Логируем ответ сервера для отладки
                android.util.Log.d("StatsScreen", "Server response: $mealsResponse")
                mealsResponse.forEach { group ->
                    android.util.Log.d("StatsScreen", "Group: date=${group.date}, totalCalories=${group.totalCalories}")
                    group.meals.forEach { meal ->
                        android.util.Log.d("StatsScreen", "  Meal: id=${meal.id}, mealType=${meal.mealType}, calories=${meal.calories}")
                    }
                }
                
                mealGroups = mealsResponse
                
            } catch (e: Exception) {
                error = e.message ?: "Ошибка загрузки данных"
            } finally {
                isLoading = false
            }
        }
    }
    
    // Преобразуем данные для недельного представления
    val weeklyStats = remember(mealGroups, currentWeekOffset) {
        calculateWeeklyStats(mealGroups, currentWeekOffset)
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Base0
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Топбар
            TopBar(
                title = "Статистика"
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
                            color = Base60
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
                        text = "Ошибка: $error",
                        style = TextStyle(
                            fontFamily = montserratFamily,
                            fontSize = 16.sp,
                            color = Red50
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        // Навигация по неделям
                        WeekNavigationHeader(
                            weeklyStats = weeklyStats,
                            onPreviousWeek = { currentWeekOffset-- },
                            onNextWeek = { currentWeekOffset++ }
                        )
                    }
                    
                    item {
                        // Столбчатая диаграмма
                        if (weeklyStats.dailyData.isNotEmpty() && nutritionPlan != null) {
                            WeeklyCalorieBarChart(
                                weeklyStats = weeklyStats,
                                targetCalories = nutritionPlan!!.calories_per_day.toDouble(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(
                                        color = Base5,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Нет данных для отображения",
                                    style = TextStyle(
                                        fontFamily = montserratFamily,
                                        fontSize = 14.sp,
                                        color = Base60
                                    )
                                )
                            }
                        }
                    }
                    
                    item {
                        // Средние значения
                        AverageNutritionStats(weeklyStats = weeklyStats)
                    }
                }
            }
            
            // Нижняя навигация
            NavBar(
                currentScreen = Screen.STATS,
                onScreenSelected = onNavigateToScreen
            )
        }
    }
}

@Composable
fun WeekNavigationHeader(
    weeklyStats: WeeklyStats,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousWeek) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Предыдущая неделя",
                tint = Green50
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Неделя",
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontSize = 14.sp,
                    color = Base60
                )
            )
            Text(
                text = "${formatDate(weeklyStats.weekStart)} - ${formatDate(weeklyStats.weekEnd)}",
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontSize = 16.sp,
                    color = Base90
                )
            )
        }
        
        IconButton(onClick = onNextWeek) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Следующая неделя",
                tint = Green50
            )
        }
    }
}

@Composable
fun WeeklyCalorieBarChart(
    weeklyStats: WeeklyStats,
    targetCalories: Double,
    modifier: Modifier = Modifier
) {
    val daysOfWeek = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val maxCalories = (weeklyStats.dailyData.maxOfOrNull { it.calories } ?: targetCalories) * 1.1
    
    Column(
        modifier = modifier
    ) {
        // Подпись линии нормы
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Норма: ${targetCalories.toInt()} ккал",
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontSize = 12.sp,
                    color = Orange50
                ),
                modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
            )
        }
        
        // Значения калорий над столбиками
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { dayName ->
                val dayData = weeklyStats.dailyData.find { it.dayOfWeek == dayName }
                Text(
                    text = if (dayData != null) "${dayData.calories.toInt()}" else "",
                    style = TextStyle(
                        fontFamily = montserratFamily,
                        fontSize = 12.sp,
                        color = Base90,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // График
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 8.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barWidth = canvasWidth / 7 * 0.7f
            val barSpacing = canvasWidth / 7 * 0.3f
            
            // Рисуем столбики
            daysOfWeek.forEachIndexed { index, dayName ->
                val dayData = weeklyStats.dailyData.find { it.dayOfWeek == dayName }
                
                if (dayData != null) {
                    val barHeight = (dayData.calories / maxCalories * canvasHeight).toFloat()
                    val x = index * (barWidth + barSpacing) + barSpacing / 2
                    val y = canvasHeight - barHeight
                    
                    // Определяем цвет столбика
                    val barColor = getBarColor(dayData.calories, targetCalories)
                    
                    // Рисуем столбик
                    drawRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight)
                    )
                }
            }
            
            // Рисуем линию суточной нормы
            val targetLineY = canvasHeight - (targetCalories / maxCalories * canvasHeight).toFloat()
            drawLine(
                color = Orange50,
                start = Offset(0f, targetLineY),
                end = Offset(canvasWidth, targetLineY),
                strokeWidth = 3.dp.toPx()
            )
        }
        
        // Подписи дней недели
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { dayName ->
                Text(
                    text = dayName,
                    style = TextStyle(
                        fontFamily = montserratFamily,
                        fontSize = 12.sp,
                        color = Base60,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AverageNutritionStats(
    weeklyStats: WeeklyStats,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Base5,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Среднее потребление за неделю",
            style = TextStyle(
                fontFamily = montserratFamily,
                fontSize = 16.sp,
                color = Base90
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Калории:",
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontSize = 14.sp,
                    color = Base90
                )
            )
            Text(
                text = "${String.format("%.1f", weeklyStats.averageCalories)} ккал",
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontSize = 14.sp,
                    color = Green70
                )
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Белки:",
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontSize = 14.sp,
                    color = Base90
                )
            )
            Text(
                text = "${String.format("%.1f", weeklyStats.averageProteins)} г",
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontSize = 14.sp,
                    color = Green70
                )
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Жиры:",
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontSize = 14.sp,
                    color = Base90
                )
            )
            Text(
                text = "${String.format("%.1f", weeklyStats.averageFats)} г",
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontSize = 14.sp,
                    color = Green70
                )
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Углеводы:",
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontSize = 14.sp,
                    color = Base90
                )
            )
            Text(
                text = "${String.format("%.1f", weeklyStats.averageCarbs)} г",
                style = TextStyle(
                    fontFamily = montserratFamily,
                    fontSize = 14.sp,
                    color = Green70
                )
            )
        }
    }
}

private fun calculateWeeklyStats(mealGroups: List<MealGroup>, weekOffset: Int): WeeklyStats {
    val today = LocalDate.now()
    val weekFields = WeekFields.of(Locale.getDefault())
    val currentWeek = today.get(weekFields.weekOfWeekBasedYear())
    val currentYear = today.get(weekFields.weekBasedYear())
    
    // Вычисляем начало и конец недели
    val targetWeek = currentWeek + weekOffset
    val weekStart = today.with(weekFields.weekOfWeekBasedYear(), targetWeek.toLong())
        .with(weekFields.dayOfWeek(), 1) // Понедельник
    val weekEnd = weekStart.plusDays(6) // Воскресенье
    
    // Преобразуем данные для недели
    val dailyDataMap = mutableMapOf<String, DailyCalorieData>()
    
    mealGroups.forEach { group ->
        try {
            val groupDate = LocalDate.parse(group.date)
            if (!groupDate.isBefore(weekStart) && !groupDate.isAfter(weekEnd)) {
                // Рассчитываем общие значения локально
                val totalCalories = group.meals.sumOf { it.calories }
                val totalProteins = group.meals.sumOf { it.proteins }
                val totalFats = group.meals.sumOf { it.fats }
                val totalCarbs = group.meals.sumOf { it.carbs }
                
                val dayOfWeek = when (groupDate.dayOfWeek.value) {
                    1 -> "Пн"
                    2 -> "Вт"
                    3 -> "Ср"
                    4 -> "Чт"
                    5 -> "Пт"
                    6 -> "Сб"
                    7 -> "Вс"
                    else -> "Пн"
                }
                
                dailyDataMap[dayOfWeek] = DailyCalorieData(
                    date = group.date,
                    calories = totalCalories,
                    proteins = totalProteins,
                    fats = totalFats,
                    carbs = totalCarbs,
                    formattedDate = formatDate(groupDate),
                    dayOfWeek = dayOfWeek
                )
            }
        } catch (e: Exception) {
            // Игнорируем некорректные даты
        }
    }
    
    val dailyData = dailyDataMap.values.toList()
    
    // Рассчитываем средние значения
    val averageCalories = if (dailyData.isNotEmpty()) dailyData.map { it.calories }.average() else 0.0
    val averageProteins = if (dailyData.isNotEmpty()) dailyData.map { it.proteins }.average() else 0.0
    val averageFats = if (dailyData.isNotEmpty()) dailyData.map { it.fats }.average() else 0.0
    val averageCarbs = if (dailyData.isNotEmpty()) dailyData.map { it.carbs }.average() else 0.0
    
    return WeeklyStats(
        weekStart = weekStart,
        weekEnd = weekEnd,
        dailyData = dailyData,
        averageCalories = averageCalories,
        averageProteins = averageProteins,
        averageFats = averageFats,
        averageCarbs = averageCarbs
    )
}

private fun getBarColor(actualCalories: Double, targetCalories: Double): Color {
    val difference = abs(actualCalories - targetCalories)
    return if (difference <= 100) {
        Green50
    } else {
        Orange50
    }
}

private fun formatDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("dd.MM"))
} 