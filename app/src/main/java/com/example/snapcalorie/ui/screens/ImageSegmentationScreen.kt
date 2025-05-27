package com.example.snapcalorie.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scale
import coil.compose.rememberAsyncImagePainter
import com.example.snapcalorie.ui.components.NavBar
import com.example.snapcalorie.ui.components.Screen
import com.example.snapcalorie.ui.components.NutritionResultDialog
import com.example.snapcalorie.ui.theme.*
import com.example.snapcalorie.util.SegmentationModel
import com.example.snapcalorie.util.MaskProcessor
import com.example.snapcalorie.util.OpenCVInitializer
import com.example.snapcalorie.util.BoundingBoxProcessor
import com.example.snapcalorie.util.ClassificationRegion
import com.example.snapcalorie.util.DishTypeClassificationRegion
import com.example.snapcalorie.util.DishTypeClassificationResult
import com.example.snapcalorie.network.ApiModule
import com.example.snapcalorie.repository.ClassificationRepository
import com.example.snapcalorie.storage.TokenStorage
import com.example.snapcalorie.model.MealRecordCreate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import com.example.snapcalorie.util.translateDishName
import com.example.snapcalorie.util.NutritionDataLoader
import com.example.snapcalorie.util.NutritionCalculationResult
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class RegionClassificationResult(
    val region: ClassificationRegion,
    val classificationResult: String
)

data class SegmentationResult(
    val originalBitmap: Bitmap,
    val segmentedBitmap: Bitmap,
    val mask: Array<IntArray>,
    val labelColors: Map<Int, Color>,
    val labels: List<String>,
    val detectedClasses: Set<Int>,
    val regionClassifications: List<RegionClassificationResult> = emptyList(),
    val dishTypeClassifications: List<DishTypeClassificationResult> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSegmentationScreen(
    imageUri: Uri,
    onNavigateToScreen: (Screen) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var segmentationResult by remember { mutableStateOf<SegmentationResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isOpenCVReady by remember { mutableStateOf(false) }
    var showMaskEditor by remember { mutableStateOf(false) }
    
    // Состояния для диалога результатов
    var showNutritionDialog by remember { mutableStateOf(false) }
    var nutritionResult by remember { mutableStateOf<NutritionCalculationResult?>(null) }
    var selectedMealTypeName by remember { mutableStateOf<String?>(null) }
    var selectedMealTypeValue by remember { mutableStateOf<Int?>(null) }
    var isCalculating by remember { mutableStateOf(false) }
    var isSendingMeal by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    // Классы, которые нужно исключить из отображения
    val excludedClasses = setOf("background", "food_containers", "dining_tools")
    
    // Предопределенные цвета для классов
    val predefinedColors = listOf(
        Color(0xFFCA054D), // CA054D
        Color(0xFF3F88C5), // 3F88C5
        Color(0xFF7FB069), // 7FB069
        Color(0xFFE6AA68)  // E6AA68
    )
    
    // Функция для подсчета площади каждого класса и присвоения цветов с учетом классификации
    val assignColorsByAreaWithClassification = remember {
        { mask: Array<IntArray>, labels: List<String>, classificationResult: String? ->
            // Подсчитываем площадь каждого класса
            val classAreas = mutableMapOf<Int, Int>()
            for (row in mask) {
                for (classId in row) {
                    if (classId > 0) { // Исключаем фон (класс 0)
                        classAreas[classId] = classAreas.getOrDefault(classId, 0) + 1
                    }
                }
            }
            
            // Фильтруем исключенные классы и классы, которых нет в маске
            val validClasses = classAreas.filter { (classId, area) ->
                area > 0 && // Класс должен присутствовать в маске
                classId < labels.size && 
                !excludedClasses.contains(labels[classId].lowercase())
            }
            
            val colorMap = mutableMapOf<Int, Color>()
            
            if (classificationResult != null && classificationResult != "unknown") {
                // Если есть результат классификации, объединяем все валидные классы в один
                val totalValidClassIds = validClasses.keys
                totalValidClassIds.forEach { classId ->
                    colorMap[classId] = Orange50 // Используем основной цвет для всех объединенных классов
                }
            } else {
                // Если классификация не удалась, используем старую логику
                val sortedClasses = validClasses.toList().sortedByDescending { it.second }
                
                sortedClasses.forEachIndexed { index, (classId, _) ->
                    colorMap[classId] = when (index) {
                        0 -> Orange50 // Самый большой класс
                        in 1..predefinedColors.size -> predefinedColors[index - 1]
                        else -> Base20 // Если цветов не хватило
                    }
                }
            }
            
            colorMap
        }
    }
    
    // Функция для создания сегментированного изображения с учетом классификации
    val createSegmentedBitmapWithClassification = remember {
        { originalBitmap: Bitmap, mask: Array<IntArray>, labelColors: Map<Int, Color>, labels: List<String>, classificationResult: String? ->
            val segmentedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(segmentedBitmap)
            val paint = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            }
            
            val maskHeight = mask.size
            val maskWidth = if (maskHeight > 0) mask[0].size else 0
            
            // Создаем bitmap для маски
            val maskBitmap = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888)
            
            for (y in 0 until maskHeight) {
                for (x in 0 until maskWidth) {
                    val labelId = mask[y][x]
                    // Проверяем, нужно ли исключить этот класс или он отсутствует в labelColors
                    val shouldExclude = labelId <= 0 || // Исключаем фон и недопустимые значения
                                      labelId >= labels.size || 
                                      excludedClasses.contains(labels[labelId].lowercase()) ||
                                      !labelColors.containsKey(labelId) // Исключаем классы без цвета
                    
                    val color = if (shouldExclude) {
                        Color.Transparent
                    } else {
                        // Применяем прозрачность 80% (alpha = 0.8f)
                        labelColors[labelId]?.copy(alpha = 0.8f) ?: Color.Transparent
                    }
                    maskBitmap.setPixel(x, y, color.toArgb())
                }
            }
            
            // Масштабируем маску до размера оригинального изображения
            val scaledMask = maskBitmap.scale(originalBitmap.width, originalBitmap.height)
            
            // Накладываем маску на оригинальное изображение
            canvas.drawBitmap(scaledMask, 0f, 0f, paint)
            
            segmentedBitmap
        }
    }
    
    // API сервисы
    val tokenStorage = remember { TokenStorage(context) }
    val apiService = remember { ApiModule.provideApiService { tokenStorage.token } }
    
    // Инициализируем OpenCV
    LaunchedEffect(Unit) {
        OpenCVInitializer.initializeOpenCV(context) {
            isOpenCVReady = true
        }
    }
    
    LaunchedEffect(imageUri, isOpenCVReady) {
        if (!isOpenCVReady) return@LaunchedEffect
        
        scope.launch {
            try {
                isLoading = true
                error = null
                
                val result = withContext(Dispatchers.IO) {
                    // 1. Выполняем сегментацию
                    val segmentationModel = SegmentationModel.create(context)
                    
                    // Загружаем изображение из URI
                    val inputStream = context.contentResolver.openInputStream(imageUri)
                    val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    
                    if (originalBitmap == null) {
                        throw Exception("Не удалось загрузить изображение")
                    }
                    
                    // Выполняем сегментацию
                    var mask = segmentationModel.segmentToMask(originalBitmap)
                    
                    // Получаем реальные лейблы из модели
                    val labels = segmentationModel.getLabels()
                    
                    // 2. Улучшаем маску с помощью OpenCV (кроме фона) - ВРЕМЕННО ОТКЛЮЧЕНО
                    /*
                    try {
                        val numClasses = labels.size
                        val backgroundClassId = labels.indexOfFirst { it.lowercase() == "background" }.takeIf { it >= 0 } ?: 0
                        mask = MaskProcessor.refineMultiClassMask(originalBitmap, mask, numClasses, backgroundClassId)
                    } catch (e: Exception) {
                        // Если обработка OpenCV не удалась, используем оригинальную маску
                    }
                    */
                    
                    // 3. Находим bounding box для каждого класса
                    val boundingBoxes = BoundingBoxProcessor.findClassBoundingBoxes(
                        mask, labels, excludedClasses
                    )
                    
                    // 4. Группируем классы по типу блюда для более точной классификации
                    val dishTypeGroups = BoundingBoxProcessor.groupClassesByDishType(boundingBoxes)
                    
                    // 5. Создаем регионы для классификации по типу блюда
                    val dishTypeClassificationRegions = BoundingBoxProcessor.createDishTypeClassificationRegions(
                        originalBitmap, dishTypeGroups
                    )
                    
                    // 6. Классифицируем каждый регион по типу блюда и обновляем labels
                    val classificationRepository = ClassificationRepository(apiService, tokenStorage)
                    
                    // Создаем копию labels для модификации
                    val updatedLabels = labels.toMutableList()
                    val dishTypeClassifications = mutableListOf<DishTypeClassificationResult>()
                    
                    // Вычисляем общую площадь изображения
                    val totalImageArea = originalBitmap.width * originalBitmap.height
                    val minRegionAreaThreshold = totalImageArea * 0.05 // 5% от общей площади
                    
                    for (region in dishTypeClassificationRegions) {
                        try {
                            // Вычисляем площадь региона
                            val regionArea = region.boundingBox.width() * region.boundingBox.height()
                            
                            // Пропускаем классификацию если регион слишком маленький
                            if (regionArea < minRegionAreaThreshold) {
                                continue
                            }
                            
                            val classificationResponse = classificationRepository.classifyImageBitmap(region.croppedBitmap)
                            if (classificationResponse.predictedClass != "unknown") {
                                // Заменяем названия классов в этом регионе на результат классификации
                                region.classIds.forEach { classId ->
                                    if (classId < updatedLabels.size) {
                                        updatedLabels[classId] = classificationResponse.predictedClass
                                    }
                                }
                                
                                dishTypeClassifications.add(
                                    DishTypeClassificationResult(
                                        region = region,
                                        classificationResult = classificationResponse.predictedClass
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            // Если классификация региона не удалась, оставляем оригинальные названия классов
                        }
                    }
                    
                    // Физически объединяем классы на маске для успешно классифицированных групп блюд
                    var processedMask = mask
                    if (dishTypeClassifications.isNotEmpty()) {
                        processedMask = BoundingBoxProcessor.mergeClassesOnMaskByDishType(mask, dishTypeClassifications)
                    }
                    
                    // Создаем финальные результаты
                    val finalLabelColors = assignColorsByAreaWithClassification(processedMask, updatedLabels, null)
                    val finalSegmentedBitmap = createSegmentedBitmapWithClassification(
                        originalBitmap, processedMask, finalLabelColors, updatedLabels, null
                    )
                    
                    // Находим все классы, присутствующие на изображении
                    val detectedClasses = processedMask.flatMap { it.toList() }.toSet()
                    
                    segmentationModel.close()
                    
                    SegmentationResult(
                        originalBitmap = originalBitmap,
                        segmentedBitmap = finalSegmentedBitmap,
                        mask = processedMask,
                        labelColors = finalLabelColors,
                        labels = updatedLabels,
                        detectedClasses = detectedClasses,
                        regionClassifications = emptyList(),
                        dishTypeClassifications = dishTypeClassifications
                    )
                }
                
                segmentationResult = result
            } catch (e: Exception) {
                error = e.message ?: "Произошла ошибка при сегментации"
            } finally {
                isLoading = false
            }
        }
    }
    
    // Функция для обновления результата сегментации после редактирования
    val updateSegmentationResult = remember {
        { newMask: Array<IntArray>, updatedLabels: List<String>, updatedLabelColors: Map<Int, Color> ->
            segmentationResult?.let { result ->
                // Отладочная информация
                println("ImageSegmentationScreen: Получены обновленные данные")
                println("Оригинальные лейблы: ${result.labels}")
                println("Обновленные лейблы: $updatedLabels")
                println("Оригинальные цвета: ${result.labelColors}")
                println("Обновленные цвета: $updatedLabelColors")
                
                // Находим все уникальные классы в новой маске
                val detectedClassesInMask = mutableSetOf<Int>()
                for (row in newMask) {
                    for (classId in row) {
                        if (classId > 0) { // Исключаем фон (класс 0)
                            detectedClassesInMask.add(classId)
                        }
                    }
                }
                
                // Используем обновленные цвета вместо пересчета
                val finalSegmentedBitmap = createSegmentedBitmapWithClassification(
                    result.originalBitmap, newMask, updatedLabelColors, updatedLabels, null
                )
                
                segmentationResult = result.copy(
                    mask = newMask,
                    segmentedBitmap = finalSegmentedBitmap,
                    labelColors = updatedLabelColors,
                    labels = updatedLabels,
                    detectedClasses = detectedClassesInMask
                )
                
                println("ImageSegmentationScreen: Результат обновлен, новые лейблы: ${segmentationResult?.labels}")
                println("ImageSegmentationScreen: Результат обновлен, новые цвета: ${segmentationResult?.labelColors}")
            }
            showMaskEditor = false
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Custom Top Bar - Fixed at top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color.Transparent)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Назад",
                        tint = Base50,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Text(
                    text = "Новый прием пищи",
                    style = TextStyle(
                        fontFamily = montserratFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        color = Base90
                    )
                )
            }
            
            // Content Container - Scrollable
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Green50
                                )
                                Text(
                                    text = if (!isOpenCVReady) {
                                        "Инициализация OpenCV..."
                                    } else {
                                        "Выполняется сегментация и классификация регионов..."
                                    },
                                    style = TextStyle(
                                        fontFamily = montserratFamily,
                                        fontSize = 16.sp,
                                        color = Base70
                                    )
                                )
                            }
                        }
                    }
                } else if (error != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
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
                    }
                } else {
                    segmentationResult?.let { result ->
                        // Автолейаут с оригинальным изображением
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Ваше изображение",
                                    style = TextStyle(
                                        fontFamily = montserratFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 16.sp,
                                        lineHeight = 20.sp,
                                        color = Base90
                                    )
                                )
                                
                                Image(
                                    bitmap = result.originalBitmap.asImageBitmap(),
                                    contentDescription = "Оригинальное изображение",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .background(
                                            color = Base5,
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        
                        // Автолейаут с сегментированным изображением
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Сегментированное блюдо",
                                    style = TextStyle(
                                        fontFamily = montserratFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 16.sp,
                                        lineHeight = 20.sp,
                                        color = Base90
                                    )
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                ) {
                                    Image(
                                        bitmap = result.segmentedBitmap.asImageBitmap(),
                                        contentDescription = "Сегментированное изображение",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                color = Base5,
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentScale = ContentScale.Crop
                                    )
                                    
                                    // Кнопка редактирования маски
                                    FloatingActionButton(
                                        onClick = { showMaskEditor = true },
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(12.dp)
                                            .size(48.dp),
                                        containerColor = Orange50,
                                        contentColor = Color.White
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Редактировать маску",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Показываем результат классификации или обнаруженные классы
                        if (result.labelColors.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Заголовок с обозначениями колонок
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Левая часть: пустое место под прямоугольник и название
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Spacer(modifier = Modifier.size(20.dp)) // Место под прямоугольник
                                            Text(
                                                text = "", // Пустое место под название
                                                style = TextStyle(
                                                    fontFamily = montserratFamily,
                                                    fontSize = 12.sp,
                                                    color = Base90
                                                )
                                            )
                                        }
                                        
                                        // Правая часть: заголовки колонок
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Калории
                                            Text(
                                                text = "к/100 г",
                                                modifier = Modifier.weight(1f),
                                                style = TextStyle(
                                                    fontFamily = montserratFamily,
                                                    fontWeight = FontWeight.Normal,
                                                    fontSize = 12.sp,
                                                    color = Base70
                                                ),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            
                                            // Белки
                                            Text(
                                                text = "п/100 г",
                                                modifier = Modifier.weight(1f),
                                                style = TextStyle(
                                                    fontFamily = montserratFamily,
                                                    fontWeight = FontWeight.Normal,
                                                    fontSize = 12.sp,
                                                    color = Base70
                                                ),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            
                                            // Жиры
                                            Text(
                                                text = "ж/100 г",
                                                modifier = Modifier.weight(1f),
                                                style = TextStyle(
                                                    fontFamily = montserratFamily,
                                                    fontWeight = FontWeight.Normal,
                                                    fontSize = 12.sp,
                                                    color = Base70
                                                ),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            
                                            // Углеводы
                                            Text(
                                                text = "у/\n100 г",
                                                modifier = Modifier.weight(1f),
                                                style = TextStyle(
                                                    fontFamily = montserratFamily,
                                                    fontWeight = FontWeight.Normal,
                                                    fontSize = 12.sp,
                                                    color = Base70
                                                ),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                    
                                    // Показываем все классы с обновленными названиями и питательной информацией
                                    result.labelColors.toList().forEach { (labelId, color) ->
                                        val categoryName = if (labelId < result.labels.size) result.labels[labelId] else "unknown"
                                        val nutritionInfo = NutritionDataLoader().getNutritionInfo(context, categoryName)
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = Color.White,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Левая часть: прямоугольник и название
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .background(
                                                            color = color.copy(alpha = 1f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                )
                                                
                                                Text(
                                                    text = translateDishName(categoryName),
                                                    style = TextStyle(
                                                        fontFamily = montserratFamily,
                                                        fontSize = 12.sp,
                                                        color = Base90
                                                    )
                                                )
                                            }
                                            
                                            // Правая часть: питательная информация
                                            if (nutritionInfo != null) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    // Калории
                                                    Text(
                                                        text = nutritionInfo.calories.toString(),
                                                        modifier = Modifier.weight(1f),
                                                        style = TextStyle(
                                                            fontFamily = montserratFamily,
                                                            fontWeight = FontWeight.Normal,
                                                            fontSize = 12.sp,
                                                            color = Base70
                                                        ),
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                    
                                                    // Белки
                                                    Text(
                                                        text = String.format("%.1f", nutritionInfo.protein),
                                                        modifier = Modifier.weight(1f),
                                                        style = TextStyle(
                                                            fontFamily = montserratFamily,
                                                            fontWeight = FontWeight.Normal,
                                                            fontSize = 12.sp,
                                                            color = Base70
                                                        ),
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                    
                                                    // Жиры
                                                    Text(
                                                        text = String.format("%.1f", nutritionInfo.fat),
                                                        modifier = Modifier.weight(1f),
                                                        style = TextStyle(
                                                            fontFamily = montserratFamily,
                                                            fontWeight = FontWeight.Normal,
                                                            fontSize = 12.sp,
                                                            color = Base70
                                                        ),
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                    
                                                    // Углеводы
                                                    Text(
                                                        text = String.format("%.1f", nutritionInfo.carbs),
                                                        modifier = Modifier.weight(1f),
                                                        style = TextStyle(
                                                            fontFamily = montserratFamily,
                                                            fontWeight = FontWeight.Normal,
                                                            fontSize = 12.sp,
                                                            color = Base70
                                                        ),
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Поля ввода для расчета
                        item {
                            var selectedMealType by remember { mutableStateOf<String?>(null) }
                            var weightText by remember { mutableStateOf("") }
                            var showMealTypeDropdown by remember { mutableStateOf(false) }
                            
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
                            
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Выбор категории приема пищи
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedMealType ?: "",
                                        onValueChange = { },
                                        label = { Text("Выбор категории") },
                                        readOnly = true,
                                        trailingIcon = {
                                            IconButton(onClick = { showMealTypeDropdown = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = "Выбрать категорию"
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Green50,
                                            focusedLabelColor = Green50
                                        )
                                    )
                                    
                                    DropdownMenu(
                                        expanded = showMealTypeDropdown,
                                        onDismissRequest = { showMealTypeDropdown = false }
                                    ) {
                                        mealTypes.forEach { (name, value) ->
                                            DropdownMenuItem(
                                                text = { Text(name) },
                                                onClick = {
                                                    selectedMealType = name
                                                    showMealTypeDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                                
                                // Ввод массы
                                OutlinedTextField(
                                    value = weightText,
                                    onValueChange = { newValue ->
                                        // Разрешаем только цифры
                                        if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                                            weightText = newValue
                                        }
                                    },
                                    label = { Text("Масса (г)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Green50,
                                        focusedLabelColor = Green50
                                    )
                                )
                                
                                // Кнопка расчета по правому краю
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            segmentationResult?.let { result ->
                                                scope.launch {
                                                    try {
                                                        isCalculating = true
                                                        
                                                        // Получаем объем в мл из введенной массы
                                                        val volumeMl = weightText.toDoubleOrNull() ?: 0.0
                                                        
                                                        // Рассчитываем КБЖУ
                                                        val calculationResult = NutritionDataLoader().calculateNutritionFromMask(
                                                            context = context,
                                                            mask = result.mask,
                                                            labels = result.labels,
                                                            labelColors = result.labelColors,
                                                            volumeMl = volumeMl,
                                                            excludedClasses = excludedClasses
                                                        )
                                                        
                                                        // Сохраняем результаты и показываем диалог
                                                        nutritionResult = calculationResult
                                                        selectedMealTypeName = selectedMealType
                                                        selectedMealTypeValue = mealTypes.find { it.first == selectedMealType }?.second
                                                        showNutritionDialog = true
                                                        
                                                    } catch (e: Exception) {
                                                        error = "Ошибка при расчете: ${e.message}"
                                                    } finally {
                                                        isCalculating = false
                                                    }
                                                }
                                            }
                                        },
                                        enabled = selectedMealType != null && weightText.isNotEmpty() && !isCalculating,
                                        modifier = Modifier.height(52.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Green50,
                                            contentColor = Color.White,
                                            disabledContainerColor = Base20,
                                            disabledContentColor = Base50
                                        ),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        if (isCalculating) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = "Рассчитать",
                                                style = TextStyle(
                                                    fontFamily = montserratFamily,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 16.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            NavBar(
                currentScreen = Screen.IMAGE_SEGMENTATION,
                onScreenSelected = onNavigateToScreen
            )
        }
    }
    
    // Экран редактирования маски
    if (showMaskEditor && segmentationResult != null) {
        MaskEditingScreen(
            originalBitmap = segmentationResult!!.originalBitmap,
            segmentedBitmap = segmentationResult!!.segmentedBitmap,
            mask = segmentationResult!!.mask,
            labelColors = segmentationResult!!.labelColors,
            labels = segmentationResult!!.labels,
            onSave = updateSegmentationResult,
            onCancel = { showMaskEditor = false }
        )
    }
    
    // Диалог результатов расчета
    if (showNutritionDialog && nutritionResult != null && selectedMealTypeName != null) {
        NutritionResultDialog(
            mealTypeName = selectedMealTypeName!!,
            nutritionResult = nutritionResult!!,
            isSending = isSendingMeal,
            onSend = {
                scope.launch {
                    try {
                        isSendingMeal = true
                        
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
                            image_path = null // Как указано в требованиях
                        )
                        
                        // Отправляем на сервер
                        val authHeader = "Bearer ${tokenStorage.token}"
                        apiService.createMealRecord(authHeader, mealRecord)
                        
                        // Закрываем диалог и возвращаемся назад
                        showNutritionDialog = false
                        onNavigateBack()
                        
                    } catch (e: Exception) {
                        error = "Ошибка при сохранении: ${e.message}"
                    } finally {
                        isSendingMeal = false
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