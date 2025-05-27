package com.example.snapcalorie.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.scale
import com.example.snapcalorie.ui.theme.*
import kotlin.math.roundToInt
import kotlin.random.Random
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.BorderStroke
import com.example.snapcalorie.util.translateDishName

data class ColorClass(
    val classId: Int,
    val className: String, // Переведенное название для отображения
    val originalName: String, // Оригинальное английское название для поиска питательной информации
    val color: Color,
    val isCustom: Boolean = false
)

data class DrawingStroke(
    val path: List<Offset>,
    val colorClass: ColorClass,
    val strokeWidth: Float = 30f,
    val isEraser: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MaskEditingScreen(
    originalBitmap: Bitmap,
    segmentedBitmap: Bitmap,
    mask: Array<IntArray>,
    labelColors: Map<Int, Color>,
    labels: List<String>,
    onSave: (Array<IntArray>, List<String>, Map<Int, Color>) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    var currentMask by remember { mutableStateOf(mask.map { it.clone() }.toTypedArray()) }
    var drawingStrokes by remember { mutableStateOf<List<DrawingStroke>>(emptyList()) }
    var selectedColorClass by remember { mutableStateOf<ColorClass?>(null) }
    var showClassSelector by remember { mutableStateOf(false) }
    var isAddingNewClass by remember { mutableStateOf(false) }
    var allAvailableLabels by remember { mutableStateOf<List<String>>(emptyList()) }
    var editingColorClass by remember { mutableStateOf<ColorClass?>(null) }
    var isIntelligentMode by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var classToDelete by remember { mutableStateOf<ColorClass?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuClass by remember { mutableStateOf<ColorClass?>(null) }
    var isEraserMode by remember { mutableStateOf(false) }
    var brushSize by remember { mutableStateOf(30f) }
    
    // Переменные для масштабирования изображения
    var imageScale by remember { mutableStateOf(1f) }
    var imageOffsetX by remember { mutableStateOf(0f) }
    var imageOffsetY by remember { mutableStateOf(0f) }
    var scaledImageWidth by remember { mutableStateOf(0f) }
    var scaledImageHeight by remember { mutableStateOf(0f) }
    
    // Переменная для отложенного обновления
    var updateTrigger by remember { mutableStateOf(0) }
    var isUpdatingMask by remember { mutableStateOf(false) }
    
    // Загружаем все доступные лейблы из assets
    LaunchedEffect(Unit) {
        try {
            val inputStream = context.assets.open("categories.json")
            val jsonText = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()
            
            // Парсим JSON и извлекаем категории
            val categoriesPattern = """"category":\s*"([^"]+)"""".toRegex()
            val categories = categoriesPattern.findAll(jsonText)
                .map { it.groupValues[1] }
                .toList()
            
            allAvailableLabels = categories
        } catch (e: Exception) {
            // Если не удалось загрузить, используем переданные лейблы
            allAvailableLabels = labels
        }
    }
    
    // Создаем список доступных цветовых классов
    val availableColorClasses = remember(labelColors, labels) {
        labelColors.map { (classId, color) ->
            val originalName = if (classId < labels.size) labels[classId] else "unknown"
            ColorClass(
                classId = classId,
                className = translateDishName(originalName),
                originalName = originalName,
                color = color
            )
        }
    }
    
    var colorClasses by remember { mutableStateOf(availableColorClasses) }
    
    // Функция для генерации случайного цвета
    val generateRandomColor = remember {
        {
            val predefinedColors = listOf(
                Color(0xFFCA054D),
                Color(0xFF3F88C5),
                Color(0xFF7FB069),
                Color(0xFFE6AA68),
                Color(0xFF9B59B6),
                Color(0xFFE74C3C),
                Color(0xFF2ECC71),
                Color(0xFFF39C12)
            )
            predefinedColors[Random.nextInt(predefinedColors.size)]
        }
    }
    
    // Функция для сглаживания пути
    val smoothPath = remember {
        { path: List<Offset> ->
            if (path.size < 3) return@remember path
            
            val smoothed = mutableListOf<Offset>()
            smoothed.add(path.first())
            
            for (i in 1 until path.size - 1) {
                val prev = path[i - 1]
                val current = path[i]
                val next = path[i + 1]
                
                // Добавляем промежуточные точки для более плавной линии
                val smoothPoint1 = Offset(
                    (prev.x + current.x) / 2f,
                    (prev.y + current.y) / 2f
                )
                val smoothPoint2 = Offset(
                    (current.x + next.x) / 2f,
                    (current.y + next.y) / 2f
                )
                
                smoothed.add(smoothPoint1)
                smoothed.add(current)
                smoothed.add(smoothPoint2)
            }
            
            smoothed.add(path.last())
            smoothed
        }
    }
    
    // Функция для интеллектуального рисования с учетом цветов пикселей
    val intelligentPaintStroke = remember {
        { stroke: DrawingStroke, originalBitmap: Bitmap, maskWidth: Int, maskHeight: Int ->
            val bitmapWidth = originalBitmap.width
            val bitmapHeight = originalBitmap.height
            val affectedPixels = mutableSetOf<Pair<Int, Int>>()
            
            stroke.path.forEach { point ->
                // Преобразуем координаты экрана в координаты изображения
                val imageX = (point.x - imageOffsetX) / imageScale
                val imageY = (point.y - imageOffsetY) / imageScale
                
                val bitmapX = imageX.toInt().coerceIn(0, bitmapWidth - 1)
                val bitmapY = imageY.toInt().coerceIn(0, bitmapHeight - 1)
                
                // Получаем цвет пикселя в точке касания
                val touchPixelColor = originalBitmap.getPixel(bitmapX, bitmapY)
                val touchRed = android.graphics.Color.red(touchPixelColor)
                val touchGreen = android.graphics.Color.green(touchPixelColor)
                val touchBlue = android.graphics.Color.blue(touchPixelColor)
                
                // Определяем радиус поиска похожих пикселей
                val searchRadius = (stroke.strokeWidth / imageScale / 2).toInt()
                val colorTolerance = 30 // Допустимое отклонение цвета
                
                // Ищем похожие пиксели в радиусе
                for (dy in -searchRadius..searchRadius) {
                    for (dx in -searchRadius..searchRadius) {
                        val x = bitmapX + dx
                        val y = bitmapY + dy
                        
                        if (x in 0 until bitmapWidth && y in 0 until bitmapHeight) {
                            val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())
                            if (distance <= searchRadius) {
                                val pixelColor = originalBitmap.getPixel(x, y)
                                val red = android.graphics.Color.red(pixelColor)
                                val green = android.graphics.Color.green(pixelColor)
                                val blue = android.graphics.Color.blue(pixelColor)
                                
                                // Проверяем схожесть цветов
                                val colorDistance = kotlin.math.sqrt(
                                    ((red - touchRed) * (red - touchRed) +
                                     (green - touchGreen) * (green - touchGreen) +
                                     (blue - touchBlue) * (blue - touchBlue)).toDouble()
                                )
                                
                                if (colorDistance <= colorTolerance) {
                                    // Преобразуем координаты в координаты маски
                                    val maskX = (x * maskWidth / bitmapWidth).toInt()
                                    val maskY = (y * maskHeight / bitmapHeight).toInt()
                                    
                                    if (maskX in 0 until maskWidth && maskY in 0 until maskHeight) {
                                        affectedPixels.add(Pair(maskX, maskY))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            affectedPixels
        }
    }
    
    // Обновленная функция для применения штрихов к маске с выбором режима
    val applyStrokesToMask = remember {
        { strokes: List<DrawingStroke>, originalMask: Array<IntArray>, bitmapWidth: Int, bitmapHeight: Int ->
            val newMask = originalMask.map { it.clone() }.toTypedArray()
            val maskHeight = newMask.size
            val maskWidth = if (maskHeight > 0) newMask[0].size else 0
            
            strokes.forEach { stroke ->
                // Ограничиваем количество точек для производительности
                val maxPoints = 50
                val step = maxOf(1, stroke.path.size / maxPoints)
                val simplifiedPath = stroke.path.filterIndexed { index, _ -> index % step == 0 }
                
                if (isIntelligentMode) {
                    // Используем интеллектуальное рисование только для первых нескольких точек
                    val intelligentPoints = simplifiedPath.take(5) // Ограничиваем до 5 точек
                    intelligentPoints.forEach { point ->
                        try {
                            val imageX = (point.x - imageOffsetX) / imageScale
                            val imageY = (point.y - imageOffsetY) / imageScale
                            
                            val bitmapX = imageX.toInt().coerceIn(0, originalBitmap.width - 1)
                            val bitmapY = imageY.toInt().coerceIn(0, originalBitmap.height - 1)
                            
                            // Упрощенное интеллектуальное рисование
                            val searchRadius = 10 // Фиксированный небольшой радиус
                            for (dy in -searchRadius..searchRadius step 2) { // Шаг 2 для производительности
                                for (dx in -searchRadius..searchRadius step 2) {
                                    val x = bitmapX + dx
                                    val y = bitmapY + dy
                                    
                                    if (x in 0 until originalBitmap.width && y in 0 until originalBitmap.height) {
                                        val maskX = (x * maskWidth / originalBitmap.width).toInt()
                                        val maskY = (y * maskHeight / originalBitmap.height).toInt()
                                        
                                        if (maskX in 0 until maskWidth && maskY in 0 until maskHeight) {
                                            newMask[maskY][maskX] = stroke.colorClass.classId
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Игнорируем ошибки для стабильности
                        }
                    }
                }
                
                // Применяем упрощенное обычное рисование
                for (i in 0 until simplifiedPath.size - 1) {
                    val startPoint = simplifiedPath[i]
                    val endPoint = simplifiedPath[i + 1]
                    
                    try {
                        // Преобразуем координаты экрана в координаты изображения
                        val startImageX = (startPoint.x - imageOffsetX) / imageScale
                        val startImageY = (startPoint.y - imageOffsetY) / imageScale
                        val endImageX = (endPoint.x - imageOffsetX) / imageScale
                        val endImageY = (endPoint.y - imageOffsetY) / imageScale
                        
                        // Упрощенное рисование линии
                        val steps = maxOf(
                            kotlin.math.abs(endImageX - startImageX).toInt(),
                            kotlin.math.abs(endImageY - startImageY).toInt()
                        ).coerceAtMost(20) // Ограничиваем количество шагов
                        
                        for (step in 0..steps) {
                            val t = if (steps > 0) step.toFloat() / steps else 0f
                            val currentX = startImageX + (endImageX - startImageX) * t
                            val currentY = startImageY + (endImageY - startImageY) * t
                            
                            val maskX = (currentX * maskWidth / originalBitmap.width).toInt()
                            val maskY = (currentY * maskHeight / originalBitmap.height).toInt()
                            
                            val brushRadius = (stroke.strokeWidth / imageScale / 2).toInt().coerceAtLeast(2)
                            
                            for (dy in -brushRadius..brushRadius) {
                                for (dx in -brushRadius..brushRadius) {
                                    val x = maskX + dx
                                    val y = maskY + dy
                                    if (x in 0 until maskWidth && y in 0 until maskHeight) {
                                        val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())
                                        if (distance <= brushRadius) {
                                            if (stroke.isEraser) {
                                                newMask[y][x] = 0 // Ластик устанавливает фон
                                            } else {
                                                newMask[y][x] = stroke.colorClass.classId
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Игнорируем ошибки для стабильности
                    }
                }
            }
            newMask
        }
    }
    
    // Функция для создания обновленного сегментированного изображения
    val createUpdatedSegmentedBitmap = remember {
        { updatedMask: Array<IntArray> ->
            val newBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(newBitmap)
            val paint = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            }
            
            val maskHeight = updatedMask.size
            val maskWidth = if (maskHeight > 0) updatedMask[0].size else 0
            
            // Создаем bitmap для маски
            val maskBitmap = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888)
            
            for (y in 0 until maskHeight) {
                for (x in 0 until maskWidth) {
                    val labelId = updatedMask[y][x]
                    val colorClass = colorClasses.find { it.classId == labelId }
                    val color = colorClass?.color?.copy(alpha = 0.8f) ?: Color.Transparent
                    maskBitmap.setPixel(x, y, color.toArgb())
                }
            }
            
            // Масштабируем маску до размера оригинального изображения
            val scaledMask = maskBitmap.scale(originalBitmap.width, originalBitmap.height)
            
            // Накладываем маску на оригинальное изображение
            canvas.drawBitmap(scaledMask, 0f, 0f, paint)
            
            newBitmap
        }
    }
    
    // Обновляем маску при изменении штрихов - ОПТИМИЗИРУЕМ с дебаунсингом
    LaunchedEffect(updateTrigger) {
        if (updateTrigger > 0) {
            isUpdatingMask = true
            kotlinx.coroutines.delay(300) // Увеличиваем задержку до 300мс
            
            // Выполняем обновление в фоновом потоке
            withContext(Dispatchers.Default) {
                if (drawingStrokes.isNotEmpty()) {
                    val newMask = applyStrokesToMask(drawingStrokes, mask, segmentedBitmap.width, segmentedBitmap.height)
                    withContext(Dispatchers.Main) {
                        currentMask = newMask
                        isUpdatingMask = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isUpdatingMask = false
                    }
                }
            }
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color.Transparent)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Назад",
                            tint = Base50,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Text(
                        text = "Редактирование",
                        style = TextStyle(
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            color = Base90
                        )
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { isIntelligentMode = !isIntelligentMode },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isIntelligentMode) Icons.Default.AutoFixHigh else Icons.Default.Brush,
                            contentDescription = if (isIntelligentMode) "Интеллектуальный режим" else "Обычный режим",
                            tint = if (isIntelligentMode) Green50 else Base50,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { 
                            isEraserMode = !isEraserMode
                            if (isEraserMode) {
                                selectedColorClass = null // Сбрасываем выбранный класс при включении ластика
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "Ластик",
                            tint = if (isEraserMode) Red50 else Base50,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            drawingStrokes = emptyList()
                            currentMask = mask.map { it.clone() }.toTypedArray()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Отменить изменения",
                            tint = Base50,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Кнопка очистки пользовательских классов
                    if (colorClasses.any { it.isCustom }) {
                        IconButton(
                            onClick = {
                                // Удаляем все пользовательские классы
                                val customClassIds = colorClasses.filter { it.isCustom }.map { it.classId }
                                println("MaskEditingScreen: Удаляем пользовательские классы: $customClassIds")
                                
                                colorClasses = colorClasses.filter { !it.isCustom }
                                
                                // Сбрасываем выбор если был выбран пользовательский класс
                                if (selectedColorClass?.isCustom == true) {
                                    selectedColorClass = null
                                }
                                
                                // Удаляем штрихи пользовательских классов
                                drawingStrokes = drawingStrokes.filter { !customClassIds.contains(it.colorClass.classId) }
                                
                                // Очищаем маску от пользовательских классов
                                for (y in currentMask.indices) {
                                    for (x in currentMask[y].indices) {
                                        if (customClassIds.contains(currentMask[y][x])) {
                                            currentMask[y][x] = 0 // Заменяем на фон
                                        }
                                    }
                                }
                                
                                println("MaskEditingScreen: Оставшиеся классы после удаления: ${colorClasses.map { "${it.classId}: ${it.className}" }}")
                                
                                updateTrigger += 1
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Очистить пользовательские классы",
                                tint = Red50,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            // Drawing Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
                
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(selectedColorClass, isEraserMode, brushSize) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (selectedColorClass != null || isEraserMode) {
                                        // Преобразуем координаты экрана в координаты изображения
                                        val imageX = (offset.x - imageOffsetX) / imageScale
                                        val imageY = (offset.y - imageOffsetY) / imageScale
                                        
                                        // Проверяем, что касание внутри изображения
                                        if (imageX >= 0 && imageX <= originalBitmap.width && 
                                            imageY >= 0 && imageY <= originalBitmap.height) {
                                            currentStroke = listOf(offset)
                                        }
                                    }
                                },
                                onDrag = { change, _ ->
                                    if ((selectedColorClass != null || isEraserMode) && currentStroke.isNotEmpty()) {
                                        // Преобразуем координаты экрана в координаты изображения
                                        val imageX = (change.position.x - imageOffsetX) / imageScale
                                        val imageY = (change.position.y - imageOffsetY) / imageScale
                                        
                                        // Проверяем, что рисование внутри изображения
                                        if (imageX >= 0 && imageX <= originalBitmap.width && 
                                            imageY >= 0 && imageY <= originalBitmap.height) {
                                            
                                            // Добавляем точку только если она достаточно далеко от предыдущей
                                            val lastPoint = currentStroke.lastOrNull()
                                            val minDistance = brushSize / 4f // Адаптивное расстояние в зависимости от размера кисти
                                            if (lastPoint == null || 
                                                kotlin.math.sqrt(
                                                    (change.position.x - lastPoint.x).let { it * it } +
                                                    (change.position.y - lastPoint.y).let { it * it }
                                                ) > minDistance) {
                                                currentStroke = currentStroke + change.position
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if ((selectedColorClass != null || isEraserMode) && currentStroke.isNotEmpty()) {
                                        val strokeColorClass = if (isEraserMode) {
                                            // Для ластика создаем специальный класс с ID 0 (фон)
                                            ColorClass(0, "background", "background", Color.Transparent)
                                        } else {
                                            selectedColorClass!!
                                        }
                                        
                                        drawingStrokes = drawingStrokes + DrawingStroke(
                                            path = currentStroke,
                                            colorClass = strokeColorClass,
                                            strokeWidth = brushSize,
                                            isEraser = isEraserMode
                                        )
                                        currentStroke = emptyList()
                                        updateTrigger += 1 // Запускаем отложенное обновление
                                    }
                                }
                            )
                        }
                ) {
                    // Рисуем обновленное сегментированное изображение с сохранением пропорций
                    val updatedBitmap = createUpdatedSegmentedBitmap(currentMask)
                    val imageBitmap = updatedBitmap.asImageBitmap()
                    
                    // Вычисляем масштаб для сохранения пропорций
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val imageWidth = imageBitmap.width.toFloat()
                    val imageHeight = imageBitmap.height.toFloat()
                    
                    val scaleX = canvasWidth / imageWidth
                    val scaleY = canvasHeight / imageHeight
                    val scale = minOf(scaleX, scaleY) // Используем меньший масштаб для сохранения пропорций
                    
                    val scaledWidth = imageWidth * scale
                    val scaledHeight = imageHeight * scale
                    
                    // Центрируем изображение
                    val offsetX = (canvasWidth - scaledWidth) / 2f
                    val offsetY = (canvasHeight - scaledHeight) / 2f
                    
                    // Сохраняем параметры масштабирования для обработки касаний
                    imageScale = scale
                    imageOffsetX = offsetX
                    imageOffsetY = offsetY
                    scaledImageWidth = scaledWidth
                    scaledImageHeight = scaledHeight
                    
                    drawImage(
                        image = imageBitmap,
                        dstOffset = androidx.compose.ui.unit.IntOffset(offsetX.toInt(), offsetY.toInt()),
                        dstSize = androidx.compose.ui.unit.IntSize(scaledWidth.toInt(), scaledHeight.toInt())
                    )
                    
                    // Рисуем текущий штрих с улучшенным визуальным эффектом
                    if (currentStroke.isNotEmpty() && (selectedColorClass != null || isEraserMode)) {
                        val strokeColor = if (isEraserMode) {
                            Red50.copy(alpha = 0.7f) // Красный цвет для ластика
                        } else {
                            selectedColorClass!!.color.copy(alpha = 0.7f)
                        }
                        
                        val paint = Paint().apply {
                            color = strokeColor.toArgb()
                            strokeWidth = brushSize * imageScale // Используем текущий размер кисти
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                            isAntiAlias = true
                        }
                        
                        // Рисуем плавную линию через все точки
                        if (currentStroke.size > 1) {
                            val path = Path()
                            path.moveTo(currentStroke[0].x, currentStroke[0].y)
                            
                            for (i in 1 until currentStroke.size) {
                                val prev = currentStroke[i - 1]
                                val current = currentStroke[i]
                                
                                // Используем квадратичные кривые Безье для плавности
                                if (i == 1) {
                                    path.lineTo(current.x, current.y)
                                } else {
                                    val midX = (prev.x + current.x) / 2f
                                    val midY = (prev.y + current.y) / 2f
                                    path.quadTo(prev.x, prev.y, midX, midY)
                                }
                            }
                            
                            drawContext.canvas.nativeCanvas.drawPath(path, paint)
                        }
                        
                        // Рисуем точки для лучшей визуализации
                        currentStroke.forEach { point ->
                            drawCircle(
                                color = strokeColor,
                                radius = brushSize * imageScale / 2f, // Используем текущий размер кисти
                                center = point
                            )
                        }
                    }
                }
                
                // Индикатор загрузки во время обновления маски
                if (isUpdatingMask) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Green50,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            
            // Color Classes Selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Base5)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Выберите класс для рисования:",
                    style = TextStyle(
                        fontFamily = montserratFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Base90
                    )
                )
                
                // Слайдер для толщины кисти
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Толщина кисти:",
                            style = TextStyle(
                                fontFamily = montserratFamily,
                                fontSize = 12.sp,
                                color = Base70
                            )
                        )
                        Text(
                            text = "${brushSize.toInt()}px",
                            style = TextStyle(
                                fontFamily = montserratFamily,
                                fontSize = 12.sp,
                                color = Green50,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    
                    Slider(
                        value = brushSize,
                        onValueChange = { brushSize = it },
                        valueRange = 10f..100f,
                        steps = 17, // 18 значений от 10 до 100 с шагом 5
                        colors = SliderDefaults.colors(
                            thumbColor = Green50,
                            activeTrackColor = Green50,
                            inactiveTrackColor = Base20
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isIntelligentMode) "Интеллектуальное рисование" else "Обычное рисование",
                        style = TextStyle(
                            fontFamily = montserratFamily,
                            fontSize = 12.sp,
                            color = if (isIntelligentMode) Green50 else Base70
                        )
                    )
                    
                    Switch(
                        checked = isIntelligentMode,
                        onCheckedChange = { isIntelligentMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Green50,
                            checkedTrackColor = Green50.copy(alpha = 0.3f)
                        )
                    )
                }
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(colorClasses) { colorClass ->
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    color = colorClass.color,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = if (selectedColorClass?.classId == colorClass.classId) 3.dp else 0.dp,
                                    color = Base90,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .combinedClickable(
                                    onClick = {
                                        selectedColorClass = colorClass
                                    },
                                    onLongClick = {
                                        contextMenuClass = colorClass
                                        showContextMenu = true
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = colorClass.className.take(3).uppercase(),
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }
                    
                    item {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    color = Base20,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Base30,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    isAddingNewClass = true
                                    editingColorClass = null
                                    showClassSelector = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Добавить новый класс",
                                tint = Base70,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                
                selectedColorClass?.let { colorClass ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isEraserMode) {
                                "Режим: Ластик"
                            } else {
                                "Выбран: ${colorClass.className}"
                            },
                            style = TextStyle(
                                fontFamily = montserratFamily,
                                fontSize = 12.sp,
                                color = if (isEraserMode) Red50 else Base70
                            )
                        )
                        Text(
                            text = "Удерживайте для редактирования или удаления класса",
                            style = TextStyle(
                                fontFamily = montserratFamily,
                                fontSize = 10.sp,
                                color = Green50
                            )
                        )
                    }
                } ?: run {
                    if (isEraserMode) {
                        Text(
                            text = "Режим: Ластик",
                            style = TextStyle(
                                fontFamily = montserratFamily,
                                fontSize = 12.sp,
                                color = Red50
                            )
                        )
                    }
                }
            }
            
            // Кнопка сохранения внизу (не поверх контента)
            Button(
                onClick = { 
                    // Создаем обновленный список лейблов с оригинальными английскими названиями
                    val updatedLabels = labels.toMutableList()
                    
                    // Обновляем лейблы для всех классов (стандартных и пользовательских)
                    colorClasses.forEach { colorClass ->
                        if (colorClass.classId < updatedLabels.size) {
                            updatedLabels[colorClass.classId] = colorClass.originalName
                        } else {
                            // Если класс новый, добавляем его в конец списка
                            while (updatedLabels.size <= colorClass.classId) {
                                updatedLabels.add("unknown")
                            }
                            updatedLabels[colorClass.classId] = colorClass.originalName
                        }
                    }
                    
                    // Создаем обновленную карту цветов на основе текущих colorClasses
                    val updatedLabelColors = colorClasses.associate { colorClass ->
                        colorClass.classId to colorClass.color
                    }
                    
                    // Отладочная информация
                    println("MaskEditingScreen: Сохранение изменений")
                    println("Оригинальные лейблы: $labels")
                    println("Обновленные лейблы (оригинальные английские): $updatedLabels")
                    println("Оригинальные цвета: $labelColors")
                    println("Обновленные цвета: $updatedLabelColors")
                    colorClasses.forEach { colorClass ->
                        println("Класс ${colorClass.classId}: ${colorClass.className} (original: ${colorClass.originalName}, isCustom: ${colorClass.isCustom})")
                    }
                    
                    onSave(currentMask, updatedLabels, updatedLabelColors)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green50,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Сохранить изменения",
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Сохранить изменения",
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
    
    // Контекстное меню для классов
    if (showContextMenu && contextMenuClass != null) {
        Dialog(
            onDismissRequest = {
                showContextMenu = false
                contextMenuClass = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                color = Base0
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Действия с классом",
                        style = TextStyle(
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                            color = Base90
                        )
                    )
                    
                    Text(
                        text = "Класс: ${contextMenuClass!!.className}",
                        style = TextStyle(
                            fontFamily = montserratFamily,
                            fontSize = 14.sp,
                            color = Base70
                        )
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Кнопка редактирования
                        OutlinedButton(
                            onClick = {
                                editingColorClass = contextMenuClass
                                showClassSelector = true
                                showContextMenu = false
                                contextMenuClass = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Green50
                            ),
                            border = BorderStroke(1.dp, Green50),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Изменить",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontSize = 12.sp
                                )
                            )
                        }
                        
                        // Кнопка удаления
                        Button(
                            onClick = {
                                classToDelete = contextMenuClass
                                showDeleteDialog = true
                                showContextMenu = false
                                contextMenuClass = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Red50,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Удалить",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                    
                    TextButton(
                        onClick = {
                            showContextMenu = false
                            contextMenuClass = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Отмена",
                            style = TextStyle(
                                fontFamily = montserratFamily,
                                fontSize = 14.sp,
                                color = Base70
                            )
                        )
                    }
                }
            }
        }
    }
    
    // Диалог подтверждения удаления класса
    if (showDeleteDialog && classToDelete != null) {
        Dialog(
            onDismissRequest = {
                showDeleteDialog = false
                classToDelete = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                color = Base0
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Удалить класс?",
                        style = TextStyle(
                            fontFamily = montserratFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                            color = Base90
                        )
                    )
                    
                    Text(
                        text = "Вы уверены, что хотите удалить класс \"${classToDelete!!.className}\"? Все области этого класса будут удалены с маски.",
                        style = TextStyle(
                            fontFamily = montserratFamily,
                            fontSize = 14.sp,
                            color = Base70
                        )
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                classToDelete = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Отмена",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontSize = 14.sp,
                                    color = Base70
                                )
                            )
                        }
                        
                        Button(
                            onClick = {
                                classToDelete?.let { classToRemove ->
                                    // Удаляем класс из списка
                                    colorClasses = colorClasses.filter { it.classId != classToRemove.classId }
                                    
                                    // Если удаляемый класс был выбран, сбрасываем выбор
                                    if (selectedColorClass?.classId == classToRemove.classId) {
                                        selectedColorClass = null
                                    }
                                    
                                    // Удаляем все штрихи этого класса
                                    drawingStrokes = drawingStrokes.filter { it.colorClass.classId != classToRemove.classId }
                                    
                                    // Очищаем маску от этого класса (заменяем на фон)
                                    for (y in currentMask.indices) {
                                        for (x in currentMask[y].indices) {
                                            if (currentMask[y][x] == classToRemove.classId) {
                                                currentMask[y][x] = 0 // Заменяем на фон
                                            }
                                        }
                                    }
                                    
                                    println("MaskEditingScreen: Удален класс ${classToRemove.classId}: ${classToRemove.className}")
                                    println("MaskEditingScreen: Оставшиеся классы: ${colorClasses.map { "${it.classId}: ${it.className}" }}")
                                    
                                    updateTrigger += 1 // Обновляем отображение
                                }
                                
                                showDeleteDialog = false
                                classToDelete = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Red50,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Удалить",
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Class Selector Dialog
    if (showClassSelector) {
        ClassSelectorDialog(
            labels = allAvailableLabels.filter { label ->
                label !in listOf("background", "food_containers", "dining_tools")
            },
            onClassSelected = { selectedLabel ->
                if (editingColorClass != null) {
                    // Редактируем существующий класс - сохраняем его тип (стандартный/пользовательский)
                    val originalName = allAvailableLabels.find { translateDishName(it) == selectedLabel } ?: selectedLabel
                    val updatedColorClass = editingColorClass!!.copy(
                        className = selectedLabel,
                        originalName = originalName
                    )
                    colorClasses = colorClasses.map { 
                        if (it.classId == editingColorClass!!.classId) updatedColorClass else it 
                    }
                    if (selectedColorClass?.classId == editingColorClass!!.classId) {
                        selectedColorClass = updatedColorClass
                    }
                } else {
                    // Добавляем новый класс
                    val newColor = generateRandomColor()
                    val newClassId = colorClasses.maxOfOrNull { it.classId }?.plus(1) ?: labels.size
                    // Находим оригинальное английское название для выбранного переведенного названия
                    val originalName = allAvailableLabels.find { translateDishName(it) == selectedLabel } ?: selectedLabel
                    val newColorClass = ColorClass(
                        classId = newClassId,
                        className = selectedLabel,
                        originalName = originalName,
                        color = newColor,
                        isCustom = true
                    )
                    colorClasses = colorClasses + newColorClass
                    selectedColorClass = newColorClass
                }
                showClassSelector = false
                isAddingNewClass = false
                editingColorClass = null
            },
            onDismiss = {
                showClassSelector = false
                isAddingNewClass = false
                editingColorClass = null
            }
        )
    }
}

@Composable
fun ClassSelectorDialog(
    labels: List<String>,
    onClassSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredLabels = remember(labels, searchQuery) {
        if (searchQuery.isBlank()) {
            labels
        } else {
            labels.filter { label ->
                val translatedName = translateDishName(label)
                label.contains(searchQuery, ignoreCase = true) ||
                label.replace("_", " ").contains(searchQuery, ignoreCase = true) ||
                translatedName.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(22.dp),
            color = Base0
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Выберите класс продукта",
                    style = TextStyle(
                        fontFamily = montserratFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = Base90
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Поле поиска
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Поиск продукта") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Green50,
                        focusedLabelColor = Green50
                    )
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredLabels) { label ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClassSelected(translateDishName(label)) },
                            colors = CardDefaults.cardColors(containerColor = Base5),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = translateDishName(label),
                                style = TextStyle(
                                    fontFamily = montserratFamily,
                                    fontSize = 14.sp,
                                    color = Base90
                                ),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    
                    if (filteredLabels.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Продукты не найдены",
                                    style = TextStyle(
                                        fontFamily = montserratFamily,
                                        fontSize = 14.sp,
                                        color = Base50
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