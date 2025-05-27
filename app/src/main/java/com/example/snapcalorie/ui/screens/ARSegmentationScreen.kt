package com.example.snapcalorie.ui.screens

import android.graphics.Bitmap
import android.util.Log
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scale
import com.example.snapcalorie.ui.theme.*
import com.example.snapcalorie.util.SegmentationModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.snapcalorie.util.translateDishName
import com.example.snapcalorie.network.ApiModule
import com.example.snapcalorie.storage.TokenStorage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

data class ARSegmentationResult(
    val originalBitmap: Bitmap,
    val segmentedBitmap: Bitmap,
    val mask: Array<IntArray>,
    val labelColors: Map<Int, Color>,
    val labels: List<String>,
    val detectedClasses: Set<Int>,
    val arCaptureData: ARCaptureData,
    val classificationResult: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARSegmentationScreen(
    arCaptureData: ARCaptureData,
    captureStage: CaptureStage,
    onSegmentationComplete: (ARSegmentationResult) -> Unit,
    onNavigateBack: () -> Unit,
    topViewClassificationResult: String? = null
) {
    val context = LocalContext.current
    var segmentationResult by remember { mutableStateOf<ARSegmentationResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showMaskEditor by remember { mutableStateOf(false) }
    
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
    
    // Функция для применения классификации к маске
    val applyClassificationToMask = remember {
        { mask: Array<IntArray>, labels: List<String>, classificationResult: String? ->
            if (classificationResult != null && classificationResult != "unknown") {
                // Создаем новую маску с обновленными классами
                val newMask = Array(mask.size) { IntArray(mask[0].size) }
                
                // Найдем ID класса для результата классификации
                val classificationClassId = labels.indexOfFirst { it.lowercase() == classificationResult.lowercase() }
                
                // Если точного совпадения нет, создаем новый класс
                val targetClassId = if (classificationClassId != -1) {
                    classificationClassId
                } else {
                    // Создаем новый ID для классификации (используем следующий доступный ID)
                    labels.size
                }
                
                Log.d("ARSegmentation", "Applying classification '$classificationResult' with class ID $targetClassId")
                Log.d("ARSegmentation", "Classification found in labels: ${classificationClassId != -1}")
                
                for (y in mask.indices) {
                    for (x in mask[y].indices) {
                        val originalClassId = mask[y][x]
                        
                        // Если это пиксель еды (не фон, не контейнер, не инструменты)
                        if (originalClassId > 0 && 
                            originalClassId < labels.size && 
                            !excludedClasses.contains(labels[originalClassId].lowercase())) {
                            // Заменяем на класс из классификации
                            newMask[y][x] = targetClassId
                        } else {
                            // Оставляем как есть (фон, контейнеры, инструменты)
                            newMask[y][x] = originalClassId
                        }
                    }
                }
                
                newMask
            } else {
                // Если классификация не удалась, возвращаем оригинальную маску
                mask
            }
        }
    }

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

    // Perform segmentation
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                Log.d("ARSegmentation", "Starting segmentation process...")
                Log.d("ARSegmentation", "Original bitmap: ${arCaptureData.rgbFrame.width}x${arCaptureData.rgbFrame.height}")
                
                // Create copies of bitmaps to avoid recycling issues
                val originalBitmapCopy = arCaptureData.rgbFrame.copy(arCaptureData.rgbFrame.config ?: Bitmap.Config.ARGB_8888, false)
                Log.d("ARSegmentation", "Original bitmap copied: ${originalBitmapCopy.width}x${originalBitmapCopy.height}")
                
                withContext(Dispatchers.IO) {
                    val segmentationModel = SegmentationModel.create(context)
                    val mask = segmentationModel.segmentToMask(originalBitmapCopy)
                    val labels = segmentationModel.getLabels()
                    
                    Log.d("ARSegmentation", "Segmentation completed, mask size: ${mask.size}x${mask[0].size}")
                    
                    // Find detected classes
                    val detectedClasses = mutableSetOf<Int>()
                    for (row in mask) {
                        for (classId in row) {
                            if (classId > 0) {
                                detectedClasses.add(classId)
                            }
                        }
                    }
                    
                    Log.d("ARSegmentation", "Detected classes: ${detectedClasses.joinToString()}")
                    
                    // Perform classification for TOP_VIEW or use existing result for SIDE_VIEW
                    var classificationResult: String? = null
                    if (captureStage == CaptureStage.TOP_VIEW) {
                        try {
                            Log.d("ARSegmentation", "Starting server classification...")
                            val tokenStorage = TokenStorage(context)
                            val apiService = ApiModule.provideApiService { tokenStorage.token }
                            
                            // Convert bitmap to JPEG bytes
                            val stream = ByteArrayOutputStream()
                            originalBitmapCopy.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                            val imageBytes = stream.toByteArray()
                            
                            // Create multipart request
                            val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                            val imagePart = MultipartBody.Part.createFormData("file", "image.jpg", requestBody)
                            
                            // Call classification API
                            val response = apiService.classifyImage("Bearer ${tokenStorage.token}", imagePart)
                            classificationResult = response.predictedClass
                            
                            Log.d("ARSegmentation", "Classification result: $classificationResult")
                        } catch (e: Exception) {
                            Log.e("ARSegmentation", "Classification failed", e)
                            classificationResult = null
                        }
                    } else if (captureStage == CaptureStage.SIDE_VIEW) {
                        // Use classification result from TOP_VIEW
                        classificationResult = topViewClassificationResult
                        Log.d("ARSegmentation", "Using TOP_VIEW classification result for SIDE_VIEW: $classificationResult")
                    }
                    
                    // Apply classification to mask if available
                    val finalMask = applyClassificationToMask(mask, labels, classificationResult)
                    
                    // Create updated labels list that includes classification result if it's new
                    val updatedLabels = if (classificationResult != null && 
                                           classificationResult != "unknown" && 
                                           !labels.any { it.lowercase() == classificationResult.lowercase() }) {
                        // Add classification result as new label
                        labels + classificationResult
                    } else {
                        labels
                    }
                        
                    val labelColors = assignColorsByAreaWithClassification(
                        finalMask, 
                        updatedLabels, 
                        classificationResult
                    )
                    
                    val segmentedBitmap = createSegmentedBitmapWithClassification(
                        originalBitmapCopy,
                        finalMask,
                        labelColors,
                        updatedLabels,
                        classificationResult
                    )
                    
                    Log.d("ARSegmentation", "Segmented bitmap created: ${segmentedBitmap.width}x${segmentedBitmap.height}")
                    Log.d("ARSegmentation", "Updated labels: ${updatedLabels.joinToString()}")
                    
                    // Switch back to Main thread for UI updates
                    withContext(Dispatchers.Main) {
                        segmentationResult = ARSegmentationResult(
                            originalBitmap = originalBitmapCopy,
                            segmentedBitmap = segmentedBitmap,
                            mask = finalMask,
                            labelColors = labelColors,
                            labels = updatedLabels,
                            detectedClasses = detectedClasses,
                            arCaptureData = arCaptureData,
                            classificationResult = classificationResult
                        )
                        
                        Log.d("ARSegmentation", "Segmentation result created successfully with classification: $classificationResult")
                    }
                }
            } catch (e: Exception) {
                Log.e("ARSegmentation", "Segmentation failed", e)
                error = "Segmentation failed: ${e.message}"
            } finally {
                isLoading = false
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
                text = when (captureStage) {
                    CaptureStage.TOP_VIEW -> "Сегментация верхнего вида"
                    CaptureStage.SIDE_VIEW -> "Сегментация бокового вида"
                },
                                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Base90,
                modifier = Modifier.weight(1f)
            )
        }

        when {
            isLoading -> {
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
                            text = "Выполняется сегментация...",
                            color = Base80
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
            
            segmentationResult != null -> {
                val result = segmentationResult!!
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Original image
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
                                    text = "Оригинальное изображение",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Base90,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                if (result.originalBitmap != null && !result.originalBitmap.isRecycled) {
                                    Image(
                                        bitmap = result.originalBitmap.asImageBitmap(),
                                        contentDescription = "Original image",
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
                                            .background(Color.Gray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Изображение недоступно",
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Segmented image
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Base10)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Сегментированное изображение",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Base90
                                    )
                                    
                                    IconButton(
                                        onClick = { showMaskEditor = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit mask",
                                            tint = Green50
                                        )
                                    }
                                }
                                
                                if (result.segmentedBitmap != null && !result.segmentedBitmap.isRecycled) {
                                    Image(
                                        bitmap = result.segmentedBitmap.asImageBitmap(),
                                        contentDescription = "Segmented image",
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
                                            .background(Color.Gray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Сегментация недоступна",
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Detected classes
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
                                    text = "Обнаруженные классы",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Base90,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                val validClasses = result.detectedClasses.filter { classId ->
                                    classId < result.labels.size && 
                                    !excludedClasses.contains(result.labels[classId].lowercase()) &&
                                    result.labelColors.containsKey(classId)
                                }
                                
                                validClasses.forEach { classId ->
                                    val className = result.labels[classId]
                                    val color = result.labelColors[classId] ?: Color.Gray
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(
                                                    color = color,
                                                    shape = RoundedCornerShape(2.dp)
                                                )
                                        )
                                        
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        Text(
                                            text = translateDishName(className),
                                            fontSize = 14.sp,
                                            color = Base80
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Bottom button
                Button(
                    onClick = { onSegmentationComplete(result) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green50,
                        contentColor = Base0
                    )
                ) {
                    Text(
                        text = "Далее",
                        fontFamily = montserratFamily,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
} 