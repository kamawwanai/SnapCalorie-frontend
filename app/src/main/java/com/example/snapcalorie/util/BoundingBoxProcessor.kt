package com.example.snapcalorie.util

import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class ClassBoundingBox(
    val classId: Int,
    val className: String,
    val boundingBox: Rect,
    val area: Int
)

data class ClassificationRegion(
    val classIds: Set<Int>,
    val classNames: Set<String>,
    val boundingBox: Rect,
    val croppedBitmap: Bitmap
)

data class DishTypeGroup(
    val dishType: String,
    val boundingBoxes: List<ClassBoundingBox>,
    val requiredClasses: Set<String>
)

data class DishTypeClassificationRegion(
    val dishType: String,
    val classIds: Set<Int>,
    val classNames: Set<String>,
    val boundingBox: Rect,
    val croppedBitmap: Bitmap,
    val requiredClasses: Set<String>
)

data class DishTypeClassificationResult(
    val region: DishTypeClassificationRegion,
    val classificationResult: String
)

object BoundingBoxProcessor {
    
    // Классы, которые нужно объединять если они близко
    private val mergeableClasses = setOf("beverages", "soups_stews", "dairy", "fats_oils_sauces")
    private const val MERGE_DISTANCE_THRESHOLD = 10
    
    // Группировка классов по типу блюда для классификации
    private val mergeGroupsByDishType = mapOf(
        // Салат — всё «зелёное» и хрустящее
        "salad" to setOf(
            "leafy_greens",
            "herbs_spices",
            "other_vegetables",
            "stem_vegetables",
            "non-starchy_roots"
        ),

        // Суп — бульон + овощи + бобовые + крахмал + масла/соусы
        "soup" to setOf(
            "soups_stews",
            "non-starchy_roots",
            "other_vegetables",
            "beans_nuts",
            "starchy_vegetables",
            "other_starches",
            "herbs_spices",
            "fats_oils_sauces"
        ),

        // Рагу/stew — мясо/рыба + овощи + крахмал
        "stew" to setOf(
            "soups_stews",
            "meats",
            "poultry",
            "seafood",
            "non-starchy_roots",
            "other_vegetables",
            "beans_nuts",
            "starchy_vegetables",
            "other_starches"
        ),

        // Смузи/коктейль — фрукты + молочка + специи/травы
        "smoothie" to setOf(
            "fruits",
            "dairy",
            "beverages",
            "herbs_spices"
        ),

        // Зерновая миска — крупы + бобовые + овощи/фрукты + молочка
        "grain_bowl" to setOf(
            "rice_grains_cereals",
            "beans_nuts",
            "other_vegetables",
            "fruits",
            "dairy"
        ),

        // Паста — макароны + соус + (опционально) белок и овощи
        "pasta" to setOf(
            "noodles_pasta",
            "fats_oils_sauces",
            "other_vegetables",
            "beans_nuts",
            "meats",
            "seafood",
            "dairy"
        ),

        // Сэндвич/бургер — выпечка + белок + овощи/соус
        "sandwich" to setOf(
            "baked_goods",
            "meats",
            "poultry",
            "other_vegetables",
            "dairy",
            "fats_oils_sauces"
        ),

        // Пицца — тесто + топпинги + сыр + соус
        "pizza" to setOf(
            "baked_goods",
            "meats",
            "poultry",
            "seafood",
            "other_vegetables",
            "dairy",
            "fats_oils_sauces"
        ),

        // Десерт — сладкое + выпечка + фрукты/молочка
        "dessert" to setOf(
            "sweets_desserts",
            "baked_goods",
            "fruits",
            "dairy"
        ),

        // Напиток — жидкости + молочка + фрукты/специи
        "beverage" to setOf(
            "beverages",
            "dairy",
            "fruits",
            "herbs_spices"
        ),

        // Лёгкий перекус — «снэки» и прочее
        "snack" to setOf(
            "snacks",
            "other_food"
        )
    )
    
    /**
     * Находит bounding box для каждого класса в маске
     */
    fun findClassBoundingBoxes(
        mask: Array<IntArray>,
        labels: List<String>,
        excludedClasses: Set<String>
    ): List<ClassBoundingBox> {
        val height = mask.size
        val width = if (height > 0) mask[0].size else 0
        
        if (height == 0 || width == 0) return emptyList()
        
        val classBounds = mutableMapOf<Int, Rect>()
        val classAreas = mutableMapOf<Int, Int>()
        
        // Находим границы для каждого класса
        for (y in 0 until height) {
            for (x in 0 until width) {
                val classId = mask[y][x]
                
                // Пропускаем исключенные классы
                if (classId >= labels.size || excludedClasses.contains(labels[classId].lowercase())) {
                    continue
                }
                
                // Обновляем bounding box
                val currentBounds = classBounds[classId]
                if (currentBounds == null) {
                    classBounds[classId] = Rect(x, y, x + 1, y + 1)
                } else {
                    currentBounds.left = min(currentBounds.left, x)
                    currentBounds.top = min(currentBounds.top, y)
                    currentBounds.right = max(currentBounds.right, x + 1)
                    currentBounds.bottom = max(currentBounds.bottom, y + 1)
                }
                
                // Подсчитываем площадь
                classAreas[classId] = classAreas.getOrDefault(classId, 0) + 1
            }
        }
        
        return classBounds.map { (classId, bounds) ->
            ClassBoundingBox(
                classId = classId,
                className = if (classId < labels.size) labels[classId] else "unknown",
                boundingBox = bounds,
                area = classAreas[classId] ?: 0
            )
        }.filter { it.area > 0 } // Убираем пустые классы
    }
    
    /**
     * Объединяет близкие классы, которые должны классифицироваться вместе
     */
    fun mergeCloseClasses(
        boundingBoxes: List<ClassBoundingBox>
    ): List<Set<ClassBoundingBox>> {
        val mergeableBoxes = boundingBoxes.filter { 
            mergeableClasses.contains(it.className.lowercase()) 
        }
        val nonMergeableBoxes = boundingBoxes.filter { 
            !mergeableClasses.contains(it.className.lowercase()) 
        }
        
        if (mergeableBoxes.isEmpty()) {
            return boundingBoxes.map { setOf(it) }
        }
        
        // Группируем близкие объединяемые классы
        val groups = mutableListOf<MutableSet<ClassBoundingBox>>()
        
        for (box in mergeableBoxes) {
            var addedToGroup = false
            
            for (group in groups) {
                // Проверяем, близок ли этот box к любому в группе
                if (group.any { isClose(it.boundingBox, box.boundingBox) }) {
                    group.add(box)
                    addedToGroup = true
                    break
                }
            }
            
            if (!addedToGroup) {
                groups.add(mutableSetOf(box))
            }
        }
        
        // Объединяем группы, которые стали близкими после добавления новых элементов
        var merged = true
        while (merged) {
            merged = false
            for (i in groups.indices) {
                for (j in i + 1 until groups.size) {
                    if (groupsAreClose(groups[i], groups[j])) {
                        groups[i].addAll(groups[j])
                        groups.removeAt(j)
                        merged = true
                        break
                    }
                }
                if (merged) break
            }
        }
        
        // Добавляем необъединяемые классы как отдельные группы
        val result = mutableListOf<Set<ClassBoundingBox>>()
        result.addAll(groups.map { it.toSet() })
        result.addAll(nonMergeableBoxes.map { setOf(it) })
        
        return result
    }
    
    /**
     * Создает регионы для классификации
     */
    fun createClassificationRegions(
        originalBitmap: Bitmap,
        groupedBoxes: List<Set<ClassBoundingBox>>
    ): List<ClassificationRegion> {
        return groupedBoxes.map { group ->
            val combinedBounds = combineBoundingBoxes(group.map { it.boundingBox })
            val croppedBitmap = cropBitmap(originalBitmap, combinedBounds)
            
            ClassificationRegion(
                classIds = group.map { it.classId }.toSet(),
                classNames = group.map { it.className }.toSet(),
                boundingBox = combinedBounds,
                croppedBitmap = croppedBitmap
            )
        }
    }
    
    /**
     * Проверяет, находятся ли два bounding box близко друг к другу
     */
    private fun isClose(rect1: Rect, rect2: Rect): Boolean {
        // Вычисляем минимальное расстояние между прямоугольниками
        val dx = max(0, max(rect1.left - rect2.right, rect2.left - rect1.right))
        val dy = max(0, max(rect1.top - rect2.bottom, rect2.top - rect1.bottom))
        val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())
        
        return distance <= MERGE_DISTANCE_THRESHOLD
    }
    
    /**
     * Проверяет, близки ли две группы bounding box
     */
    private fun groupsAreClose(group1: Set<ClassBoundingBox>, group2: Set<ClassBoundingBox>): Boolean {
        for (box1 in group1) {
            for (box2 in group2) {
                if (isClose(box1.boundingBox, box2.boundingBox)) {
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * Объединяет несколько bounding box в один
     */
    private fun combineBoundingBoxes(boxes: List<Rect>): Rect {
        if (boxes.isEmpty()) {
            return Rect(0, 0, 0, 0)
        }
        
        var left = boxes[0].left
        var top = boxes[0].top
        var right = boxes[0].right
        var bottom = boxes[0].bottom
        
        for (box in boxes.drop(1)) {
            left = min(left, box.left)
            top = min(top, box.top)
            right = max(right, box.right)
            bottom = max(bottom, box.bottom)
        }
        
        return Rect(left, top, right, bottom)
    }
    
    /**
     * Обрезает bitmap по заданному прямоугольнику
     */
    private fun cropBitmap(bitmap: Bitmap, rect: Rect): Bitmap {
        // Убеждаемся, что координаты в пределах изображения
        val left = max(0, rect.left)
        val top = max(0, rect.top)
        val right = min(bitmap.width, rect.right)
        val bottom = min(bitmap.height, rect.bottom)
        
        val width = right - left
        val height = bottom - top
        
        if (width <= 0 || height <= 0) {
            // Возвращаем минимальный bitmap если область пустая
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
        
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }
    
    /**
     * Объединяет все близкие классы (в пределах заданного расстояния) для высокоуверенной классификации
     */
    fun mergeAllCloseClasses(
        boundingBoxes: List<ClassBoundingBox>,
        distanceThreshold: Int = 3
    ): List<Set<ClassBoundingBox>> {
        if (boundingBoxes.isEmpty()) {
            return emptyList()
        }
        
        // Группируем все близкие классы
        val groups = mutableListOf<MutableSet<ClassBoundingBox>>()
        
        for (box in boundingBoxes) {
            var addedToGroup = false
            
            for (group in groups) {
                // Проверяем, близок ли этот box к любому в группе
                if (group.any { isCloseWithThreshold(it.boundingBox, box.boundingBox, distanceThreshold) }) {
                    group.add(box)
                    addedToGroup = true
                    break
                }
            }
            
            if (!addedToGroup) {
                groups.add(mutableSetOf(box))
            }
        }
        
        // Объединяем группы, которые стали близкими после добавления новых элементов
        var merged = true
        while (merged) {
            merged = false
            for (i in groups.indices) {
                for (j in i + 1 until groups.size) {
                    if (groupsAreCloseWithThreshold(groups[i], groups[j], distanceThreshold)) {
                        groups[i].addAll(groups[j])
                        groups.removeAt(j)
                        merged = true
                        break
                    }
                }
                if (merged) break
            }
        }
        
        return groups.map { it.toSet() }
    }
    
    /**
     * Проверяет, находятся ли два bounding box близко друг к другу с заданным порогом
     */
    private fun isCloseWithThreshold(rect1: Rect, rect2: Rect, threshold: Int): Boolean {
        // Вычисляем минимальное расстояние между прямоугольниками
        val dx = max(0, max(rect1.left - rect2.right, rect2.left - rect1.right))
        val dy = max(0, max(rect1.top - rect2.bottom, rect2.top - rect1.bottom))
        val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())
        
        return distance <= threshold
    }
    
    /**
     * Проверяет, близки ли две группы bounding box с заданным порогом
     */
    private fun groupsAreCloseWithThreshold(
        group1: Set<ClassBoundingBox>, 
        group2: Set<ClassBoundingBox>, 
        threshold: Int
    ): Boolean {
        for (box1 in group1) {
            for (box2 in group2) {
                if (isCloseWithThreshold(box1.boundingBox, box2.boundingBox, threshold)) {
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * Физически объединяет классы на маске - заменяет пиксели всех классов в группе на один общий класс
     */
    fun mergeClassesOnMask(
        mask: Array<IntArray>,
        mergedGroups: List<Set<ClassBoundingBox>>
    ): Array<IntArray> {
        val height = mask.size
        val width = if (height > 0) mask[0].size else 0
        
        if (height == 0 || width == 0) return mask
        
        // Создаем копию маски
        val newMask = Array(height) { y -> IntArray(width) { x -> mask[y][x] } }
        
        // Для каждой группы объединенных классов
        mergedGroups.forEach { group ->
            if (group.size > 1) { // Только если группа содержит несколько классов
                // Берем первый класс в группе как целевой
                val targetClassId = group.first().classId
                val allClassIds = group.map { it.classId }.toSet()
                
                // Заменяем все пиксели других классов в группе на целевой класс
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val currentClassId = newMask[y][x]
                        if (allClassIds.contains(currentClassId) && currentClassId != targetClassId) {
                            newMask[y][x] = targetClassId
                        }
                    }
                }
            }
        }
        
        return newMask
    }
    
    /**
     * Группирует классы по типу блюда для более точной классификации
     */
    fun groupClassesByDishType(
        boundingBoxes: List<ClassBoundingBox>
    ): List<DishTypeGroup> {
        val dishGroups = mutableListOf<DishTypeGroup>()
        val usedClassIds = mutableSetOf<Int>()
        
        // Для каждого типа блюда проверяем, есть ли подходящие классы
        mergeGroupsByDishType.forEach { (dishType, requiredClasses) ->
            val matchingBoxes = boundingBoxes.filter { box ->
                !usedClassIds.contains(box.classId) && 
                requiredClasses.contains(box.className.lowercase())
            }
            
            // Если найдено достаточно классов для этого типа блюда (минимум 2)
            if (matchingBoxes.size >= 2) {
                dishGroups.add(
                    DishTypeGroup(
                        dishType = dishType,
                        boundingBoxes = matchingBoxes,
                        requiredClasses = requiredClasses
                    )
                )
                // Помечаем использованные классы
                matchingBoxes.forEach { usedClassIds.add(it.classId) }
            }
        }
        
        // Добавляем оставшиеся классы как отдельные группы
        val remainingBoxes = boundingBoxes.filter { !usedClassIds.contains(it.classId) }
        remainingBoxes.forEach { box ->
            dishGroups.add(
                DishTypeGroup(
                    dishType = "individual",
                    boundingBoxes = listOf(box),
                    requiredClasses = setOf(box.className.lowercase())
                )
            )
        }
        
        return dishGroups
    }
    
    /**
     * Создает регионы для классификации на основе групп типов блюд
     */
    fun createDishTypeClassificationRegions(
        originalBitmap: Bitmap,
        dishGroups: List<DishTypeGroup>
    ): List<DishTypeClassificationRegion> {
        return dishGroups.map { group ->
            val combinedBounds = combineBoundingBoxes(group.boundingBoxes.map { it.boundingBox })
            val croppedBitmap = cropBitmap(originalBitmap, combinedBounds)
            
            DishTypeClassificationRegion(
                dishType = group.dishType,
                classIds = group.boundingBoxes.map { it.classId }.toSet(),
                classNames = group.boundingBoxes.map { it.className }.toSet(),
                boundingBox = combinedBounds,
                croppedBitmap = croppedBitmap,
                requiredClasses = group.requiredClasses
            )
        }
    }
    
    /**
     * Объединяет классы на маске на основе результатов классификации типов блюд
     */
    fun mergeClassesOnMaskByDishType(
        mask: Array<IntArray>,
        dishTypeResults: List<DishTypeClassificationResult>
    ): Array<IntArray> {
        val height = mask.size
        val width = if (height > 0) mask[0].size else 0
        
        if (height == 0 || width == 0) return mask
        
        // Создаем копию маски
        val newMask = Array(height) { y -> IntArray(width) { x -> mask[y][x] } }
        
        // Для каждого успешно классифицированного типа блюда
        dishTypeResults.forEach { result ->
            if (result.classificationResult != "unknown" && result.region.classIds.size > 1) {
                // Берем первый класс в группе как целевой
                val targetClassId = result.region.classIds.first()
                val allClassIds = result.region.classIds
                
                // Заменяем все пиксели других классов в группе на целевой класс
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val currentClassId = newMask[y][x]
                        if (allClassIds.contains(currentClassId) && currentClassId != targetClassId) {
                            newMask[y][x] = targetClassId
                        }
                    }
                }
            }
        }
        
        return newMask
    }
} 