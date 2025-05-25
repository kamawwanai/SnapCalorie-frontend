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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scale
import coil.compose.rememberAsyncImagePainter
import com.example.snapcalorie.ui.components.NavBar
import com.example.snapcalorie.ui.components.Screen
import com.example.snapcalorie.ui.theme.*
import com.example.snapcalorie.util.SegmentationModel
import com.example.snapcalorie.util.MaskProcessor
import com.example.snapcalorie.util.OpenCVInitializer
import com.example.snapcalorie.util.BoundingBoxProcessor
import com.example.snapcalorie.util.ClassificationRegion
import com.example.snapcalorie.network.ApiModule
import com.example.snapcalorie.repository.ClassificationRepository
import com.example.snapcalorie.storage.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

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
    val regionClassifications: List<RegionClassificationResult> = emptyList()
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
                    classAreas[classId] = classAreas.getOrDefault(classId, 0) + 1
                }
            }
            
            // Фильтруем исключенные классы
            val validClasses = classAreas.filter { (classId, _) ->
                classId < labels.size && !excludedClasses.contains(labels[classId].lowercase())
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
                    // Проверяем, нужно ли исключить этот класс
                    val shouldExclude = labelId < labels.size && 
                                      excludedClasses.contains(labels[labelId].lowercase())
                    
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
                    
                    // 4. Объединяем близкие классы
                    val groupedBoxes = BoundingBoxProcessor.mergeCloseClasses(boundingBoxes)
                    
                    // 5. Создаем регионы для классификации
                    val classificationRegions = BoundingBoxProcessor.createClassificationRegions(
                        originalBitmap, groupedBoxes
                    )
                    
                    // 6. Классифицируем каждый регион и обновляем labels
                    val tokenStorage = TokenStorage(context)
                    val apiService = ApiModule.provideApiService { tokenStorage.token }
                    val classificationRepository = ClassificationRepository(apiService, tokenStorage)
                    
                    // Создаем копию labels для модификации
                    val updatedLabels = labels.toMutableList()
                    val regionClassifications = mutableListOf<RegionClassificationResult>()
                    
                    // Вычисляем общую площадь изображения
                    val totalImageArea = originalBitmap.width * originalBitmap.height
                    val minRegionAreaThreshold = totalImageArea * 0.05 // 5% от общей площади
                    
                    for (region in classificationRegions) {
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
                                
                                regionClassifications.add(
                                    RegionClassificationResult(
                                        region = region,
                                        classificationResult = classificationResponse.predictedClass
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            // Если классификация региона не удалась, оставляем оригинальные названия классов
                        }
                    }
                    
                    // 7. Выполняем детальную классификацию всего изображения
                    var finalUpdatedLabels = updatedLabels
                    var finalLabelColors = assignColorsByAreaWithClassification(mask, updatedLabels, null)
                    var finalSegmentedBitmap = createSegmentedBitmapWithClassification(
                        originalBitmap, mask, finalLabelColors, updatedLabels, null
                    )
                    var finalMask = mask
                    
                    try {
                        val detailedClassification = classificationRepository.classifyImageDetailed(context, imageUri)
                        
                        // Если уверенность больше 95%, объединяем все близкие классы
                        if (detailedClassification.confidencePercentage > 95.0) {
                            // Находим все bounding box снова для объединения
                            val allBoundingBoxes = BoundingBoxProcessor.findClassBoundingBoxes(
                                mask, updatedLabels, excludedClasses
                            )
                            
                            // Объединяем все близкие классы (в пределах 3 пикселей)
                            val mergedGroups = BoundingBoxProcessor.mergeAllCloseClasses(allBoundingBoxes, 3)
                            
                            // Физически объединяем классы на маске
                            finalMask = BoundingBoxProcessor.mergeClassesOnMask(mask, mergedGroups)
                            
                            // Создаем новую копию labels для финальных изменений
                            finalUpdatedLabels = updatedLabels.toMutableList()
                            
                            // Заменяем названия всех классов в объединенных группах
                            mergedGroups.forEach { group ->
                                if (group.size > 1) { // Только если группа содержит несколько классов
                                    group.forEach { boundingBox ->
                                        if (boundingBox.classId < finalUpdatedLabels.size) {
                                            finalUpdatedLabels[boundingBox.classId] = detailedClassification.predictedClass
                                        }
                                    }
                                }
                            }
                            
                            // Пересоздаем цветовую схему и изображение с новыми labels и объединенной маской
                            finalLabelColors = assignColorsByAreaWithClassification(finalMask, finalUpdatedLabels, null)
                            finalSegmentedBitmap = createSegmentedBitmapWithClassification(
                                originalBitmap, finalMask, finalLabelColors, finalUpdatedLabels, null
                            )
                        }
                    } catch (e: Exception) {
                        // Если детальная классификация не удалась, используем результаты региональной классификации
                    }
                    
                    // Находим все классы, присутствующие на изображении
                    val detectedClasses = finalMask.flatMap { it.toList() }.toSet()
                    
                    segmentationModel.close()
                    
                    SegmentationResult(
                        originalBitmap = originalBitmap,
                        segmentedBitmap = finalSegmentedBitmap,
                        mask = finalMask,
                        labelColors = finalLabelColors,
                        labels = finalUpdatedLabels,
                        detectedClasses = detectedClasses,
                        regionClassifications = regionClassifications
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
                                
                                Image(
                                    bitmap = result.segmentedBitmap.asImageBitmap(),
                                    contentDescription = "Сегментированное изображение",
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
                        
                        // Показываем результат классификации или обнаруженные классы
                        if (result.labelColors.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Обнаруженные продукты",
                                        style = TextStyle(
                                            fontFamily = montserratFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 16.sp,
                                            lineHeight = 20.sp,
                                            color = Base90
                                        )
                                    )
                                    
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Показываем все классы с обновленными названиями
                                        result.labelColors.toList().forEach { (labelId, color) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        color = Base5,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(
                                                            color = color.copy(alpha = 1f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                )
                                                
                                                Text(
                                                    text = if (labelId < result.labels.size) result.labels[labelId] else "Класс $labelId",
                                                    style = TextStyle(
                                                        fontFamily = montserratFamily,
                                                        fontSize = 14.sp,
                                                        color = Base90
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
            }
            
            NavBar(
                currentScreen = Screen.IMAGE_SEGMENTATION,
                onScreenSelected = onNavigateToScreen
            )
        }
    }
} 