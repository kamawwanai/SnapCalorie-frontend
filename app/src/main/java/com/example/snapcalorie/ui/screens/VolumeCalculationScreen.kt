package com.example.snapcalorie.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.example.snapcalorie.ui.components.Screen
import com.example.snapcalorie.ui.components.NutritionResultDialog
import com.example.snapcalorie.ui.theme.*
import com.example.snapcalorie.util.VolumeCalculator
import com.example.snapcalorie.util.NutritionDataLoader
import com.example.snapcalorie.util.NutritionCalculationResult
import com.example.snapcalorie.util.translateDishName
import com.example.snapcalorie.model.MealRecordCreate
import com.example.snapcalorie.network.ApiModule
import com.example.snapcalorie.storage.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.*

data class VolumeCalculationData(
    val topViewResult: ARSegmentationResult,
    val sideViewResult: ARSegmentationResult
)

data class VolumeResult(
    val totalVolume: Float, // в мл
    val componentVolumes: Map<String, Float>, // объем каждого компонента в мл
    val nutritionResults: Map<String, NutritionCalculationResult>,
    val pointCloudTop: List<Point3D>,
    val pointCloudSide: List<Point3D>,
    val pointCloudCombined: List<Point3D>,
    val baseZ: Float,
    val dishType: DishType
)

data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float
)

enum class DishType {
    DEEP_PLATE, FLAT_PLATE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeCalculationScreen(
    volumeData: VolumeCalculationData,
    onNavigateToScreen: (Screen) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var volumeResult by remember { mutableStateOf<VolumeResult?>(null) }
    var isCalculating by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showMealTypeDialog by remember { mutableStateOf(false) }
    var isSavingMeal by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    
    // Состояния для диалога результатов (как в ImageSegmentationScreen)
    var showNutritionDialog by remember { mutableStateOf(false) }
    var nutritionResult by remember { mutableStateOf<NutritionCalculationResult?>(null) }
    var selectedMealTypeName by remember { mutableStateOf<String?>(null) }
    var selectedMealTypeValue by remember { mutableStateOf<Int?>(null) }
    
    // Сохраняем копии изображений в состоянии, чтобы они не исчезали при изменении состояния
    val topViewBitmap by remember {
        mutableStateOf(
            try {
                if (!volumeData.topViewResult.segmentedBitmap.isRecycled) {
                    volumeData.topViewResult.segmentedBitmap.copy(
                        volumeData.topViewResult.segmentedBitmap.config ?: Bitmap.Config.ARGB_8888, 
                        false
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("VolumeCalculation", "Error copying top view bitmap", e)
                null
            }
        )
    }
    val sideViewBitmap by remember {
        mutableStateOf(
            try {
                if (!volumeData.sideViewResult.segmentedBitmap.isRecycled) {
                    volumeData.sideViewResult.segmentedBitmap.copy(
                        volumeData.sideViewResult.segmentedBitmap.config ?: Bitmap.Config.ARGB_8888, 
                        false
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("VolumeCalculation", "Error copying side view bitmap", e)
                null
            }
        )
    }
    
    val scope = rememberCoroutineScope()
    
    // Типы приемов пищи (как в ImageSegmentationScreen)
    val mealTypes = listOf(
        "Завтрак" to 1,
        "Перекус (утро)" to 2,
        "Обед" to 3,
        "Перекус (день)" to 4,
        "Ужин" to 5,
        "Перекус (вечер)" to 6,
        "Тренировка" to 7,
        "Другое" to 8
    )

    // Perform volume calculation
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val calculator = VolumeCalculator()
                    val result = calculator.calculateVolume(context, volumeData)
                    volumeResult = result
                }
            } catch (e: Exception) {
                error = "Volume calculation failed: ${e.message}"
            } finally {
                isCalculating = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Base0)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = Base90
                )
            }
            
            Text(
                text = "Расчет объема",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Base90,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Error display
        error?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Red10),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = Red70,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp
                )
            }
        }
        
        // Success message display
        if (showSuccessMessage) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Green10),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Прием пищи успешно сохранен!",
                    color = Green70,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        when {
            isCalculating -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Green50)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Вычисляется объем...",
                            color = Base80
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Это может занять несколько секунд",
                            color = Base60,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Ошибка: $error",
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Green50
                            )
                        ) {
                            Text("Назад")
                        }
                    }
                }
            }
            
            volumeResult != null -> {
                val result = volumeResult!!
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Green10)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Общий объем",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Green50,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                Text(
                                    text = "${String.format("%.1f", result.totalVolume)} мл",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Base90
                                )
                                
                                Text(
                                    text = "Тип посуды: ${when(result.dishType) {
                                        DishType.DEEP_PLATE -> "Глубокая тарелка"
                                        DishType.FLAT_PLATE -> "Плоская тарелка"
                                    }}",
                                    fontSize = 14.sp,
                                    color = Base80,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    
                    // Component volumes
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Base10)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                                                    text = "Объем по компонентам",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Base90,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                
                                result.componentVolumes.forEach { (component, volume) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = translateDishName(component),
                                            fontSize = 14.sp,
                                            color = Base80
                                        )
                                        Text(
                                            text = "${String.format("%.1f", volume)} мл",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Base90
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Nutrition information
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Base10)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                                                    text = "Пищевая ценность",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Base90,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                
                                var totalCalories = 0f
                                var totalProteins = 0f
                                var totalFats = 0f
                                var totalCarbs = 0f
                                var totalWeight = 0f
                                
                                result.nutritionResults.forEach { (component, nutrition) ->
                                    totalCalories += nutrition.calories
                                    totalProteins += nutrition.proteins
                                    totalFats += nutrition.fats
                                    totalCarbs += nutrition.carbohydrates
                                    totalWeight += nutrition.weight
                                    
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = translateDishName(component),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Base90
                                        )
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Вес: ${String.format("%.1f", nutrition.weight)} г",
                                                fontSize = 12.sp,
                                                color = Base80
                                            )
                                            Text(
                                                text = "${String.format("%.0f", nutrition.calories)} ккал",
                                                fontSize = 12.sp,
                                                color = Base80
                                            )
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Б: ${String.format("%.1f", nutrition.proteins)}г",
                                                fontSize = 12.sp,
                                                color = Base80
                                            )
                                            Text(
                                                text = "Ж: ${String.format("%.1f", nutrition.fats)}г",
                                                fontSize = 12.sp,
                                                color = Base80
                                            )
                                            Text(
                                                text = "У: ${String.format("%.1f", nutrition.carbohydrates)}г",
                                                fontSize = 12.sp,
                                                color = Base80
                                            )
                                        }
                                    }
                                    
                                    Divider(
                                        color = Base20,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                
                                // Total nutrition
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Orange10)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = "Итого",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Orange50
                                        )
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Вес: ${String.format("%.1f", totalWeight)} г",
                                                fontSize = 12.sp,
                                                color = Base80
                                            )
                                            Text(
                                                text = "${String.format("%.0f", totalCalories)} ккал",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Orange50
                                            )
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Б: ${String.format("%.1f", totalProteins)}г",
                                                fontSize = 12.sp,
                                                color = Base80
                                            )
                                            Text(
                                                text = "Ж: ${String.format("%.1f", totalFats)}г",
                                                fontSize = 12.sp,
                                                color = Base80
                                            )
                                            Text(
                                                text = "У: ${String.format("%.1f", totalCarbs)}г",
                                                fontSize = 12.sp,
                                                color = Base80
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Images comparison
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Base10)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Использованные изображения",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Base90,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Вид сверху",
                                            fontSize = 12.sp,
                                            color = Base80,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        val topBitmap = topViewBitmap
                                        if (topBitmap != null && !topBitmap.isRecycled) {
                                            Image(
                                                bitmap = topBitmap.asImageBitmap(),
                                                contentDescription = "Top view",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f)
                                                    .background(Base20),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Изображение недоступно",
                                                    color = Base60,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                    
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Вид сбоку",
                                            fontSize = 12.sp,
                                            color = Base80,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        val sideBitmap = sideViewBitmap
                                        if (sideBitmap != null && !sideBitmap.isRecycled) {
                                            Image(
                                                bitmap = sideBitmap.asImageBitmap(),
                                                contentDescription = "Side view",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f)
                                                    .background(Base20),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Изображение недоступно",
                                                    color = Base60,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Bottom buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            Log.d("VolumeCalculation", "New photo button clicked")
                            onNavigateToScreen(Screen.CAMERA) 
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Green50
                        )
                    ) {
                        Text(
                            text = "Новое фото",
                            fontFamily = montserratFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Button(
                        onClick = { 
                            volumeResult?.let { result ->
                                // Создаем общий результат питания из всех компонентов
                                val totalCalories = result.nutritionResults.values.sumOf { it.calories.toDouble() }.toFloat()
                                val totalProteins = result.nutritionResults.values.sumOf { it.proteins.toDouble() }.toFloat()
                                val totalFats = result.nutritionResults.values.sumOf { it.fats.toDouble() }.toFloat()
                                val totalCarbs = result.nutritionResults.values.sumOf { it.carbohydrates.toDouble() }.toFloat()
                                val totalWeight = result.nutritionResults.values.sumOf { it.weight.toDouble() }.toFloat()
                                
                                nutritionResult = NutritionCalculationResult(
                                    weight = totalWeight,
                                    calories = totalCalories,
                                    proteins = totalProteins,
                                    fats = totalFats,
                                    carbohydrates = totalCarbs
                                )
                                
                                showMealTypeDialog = true
                            }
                        },
                        enabled = volumeResult != null && !isSavingMeal,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green50,
                            contentColor = Base0
                        )
                    ) {
                        if (isSavingMeal) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Base0,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "В дневник",
                                fontFamily = montserratFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Диалог выбора типа приема пищи
    if (showMealTypeDialog && nutritionResult != null) {
        AlertDialog(
            onDismissRequest = { showMealTypeDialog = false },
            title = {
                Text(
                    text = "Выберите тип приема пищи",
                    fontWeight = FontWeight.Medium,
                    color = Base90
                )
            },
            text = {
                LazyColumn {
                    items(mealTypes.size) { index ->
                        val (mealName, mealValue) = mealTypes[index]
                        
                        TextButton(
                            onClick = {
                                selectedMealTypeName = mealName
                                selectedMealTypeValue = mealValue
                                showMealTypeDialog = false
                                showNutritionDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = mealName,
                                color = Base90,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                        
                        if (index < mealTypes.size - 1) {
                            Divider(color = Base20)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showMealTypeDialog = false }
                ) {
                    Text(
                        text = "Отмена",
                        color = Green50
                    )
                }
            }
        )
    }
    
    // Диалог результатов расчета (как в ImageSegmentationScreen)
    if (showNutritionDialog && nutritionResult != null && selectedMealTypeName != null) {
        NutritionResultDialog(
            mealTypeName = selectedMealTypeName!!,
            nutritionResult = nutritionResult!!,
            isSending = isSavingMeal,
            onSend = {
                scope.launch {
                    try {
                        isSavingMeal = true
                        
                        val tokenStorage = TokenStorage(context)
                        val apiService = ApiModule.provideApiService { tokenStorage.token }
                        
                        // Создаем запись о приеме пищи
                        val currentDateTime = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        
                        val mealRecord = MealRecordCreate(
                            datetime = currentDateTime.format(formatter),
                            calories = nutritionResult!!.calories.toDouble(),
                            proteins = nutritionResult!!.proteins.toDouble(),
                            fats = nutritionResult!!.fats.toDouble(),
                            carbs = nutritionResult!!.carbohydrates.toDouble(),
                            meal_type = selectedMealTypeValue ?: 8, // По умолчанию "Другое"
                            image_path = null
                        )
                        
                        // Отправляем на сервер
                        val authHeader = "Bearer ${tokenStorage.token}"
                        apiService.createMealRecord(authHeader, mealRecord)
                        
                        // Закрываем диалог и переходим к профилю
                        showNutritionDialog = false
                        onNavigateToScreen(Screen.PROFILE)
                        
                    } catch (e: Exception) {
                        error = "Ошибка при сохранении: ${e.message}"
                    } finally {
                        isSavingMeal = false
                    }
                }
            },
            onDismiss = {
                showNutritionDialog = false
                nutritionResult = null
                selectedMealTypeName = null
                selectedMealTypeValue = null
            }
        )
    }
} 