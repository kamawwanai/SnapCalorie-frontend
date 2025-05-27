package com.example.snapcalorie.util

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

data class NutritionCategory(
    val category: String,
    val density: Double,
    val calories: Int,
    val protein: Double,
    val fat: Double,
    val carbs: Double
)

data class NutritionInfo(
    val calories: Int,
    val protein: Double,
    val fat: Double,
    val carbs: Double
)

data class NutritionCalculationResult(
    val weight: Float,
    val calories: Float,
    val proteins: Float,
    val fats: Float,
    val carbohydrates: Float
)

data class NutritionData(
    val calories: Float,
    val proteins: Float,
    val fats: Float,
    val carbohydrates: Float
)

class NutritionDataLoader {
    private var categoriesCache: List<NutritionCategory>? = null
    
    suspend fun getNutritionData(component: String): NutritionData? {
        // Simple mapping for common foods - in real app this would come from a database
        val nutritionMap = mapOf(
            "rice" to NutritionData(130f, 2.7f, 0.3f, 28f),
            "pasta" to NutritionData(131f, 5f, 1.1f, 25f),
            "meat" to NutritionData(250f, 26f, 15f, 0f),
            "chicken" to NutritionData(165f, 31f, 3.6f, 0f),
            "beef" to NutritionData(250f, 26f, 15f, 0f),
            "pork" to NutritionData(242f, 27f, 14f, 0f),
            "fish" to NutritionData(206f, 22f, 12f, 0f),
            "vegetables" to NutritionData(25f, 1f, 0.2f, 5f),
            "potato" to NutritionData(77f, 2f, 0.1f, 17f),
            "bread" to NutritionData(265f, 9f, 3.2f, 49f),
            "soup" to NutritionData(50f, 2f, 1f, 8f),
            "sauce" to NutritionData(80f, 1f, 5f, 8f),
            "salad" to NutritionData(15f, 1f, 0.1f, 3f),
            "fruit" to NutritionData(52f, 0.3f, 0.2f, 14f),
            "cheese" to NutritionData(402f, 25f, 33f, 1.3f),
            "egg" to NutritionData(155f, 13f, 11f, 1.1f)
        )
        
        return nutritionMap[component.lowercase()]
    }

    fun loadCategories(context: Context): List<NutritionCategory> {
        if (categoriesCache != null) {
            return categoriesCache!!
        }
        
        return try {
            val jsonString = context.assets.open("categories.json").bufferedReader().use { it.readText() }
            val gson = Gson()
            val listType = object : TypeToken<List<NutritionCategory>>() {}.type
            val categories = gson.fromJson<List<NutritionCategory>>(jsonString, listType)
            categoriesCache = categories
            categories
        } catch (e: IOException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getNutritionInfo(context: Context, categoryName: String): NutritionInfo? {
        val categories = loadCategories(context)
        val category = categories.find { it.category.equals(categoryName, ignoreCase = true) }
        return category?.let {
            NutritionInfo(
                calories = it.calories,
                protein = it.protein,
                fat = it.fat,
                carbs = it.carbs
            )
        }
    }
    
    fun calculateNutritionFromMask(
        context: Context,
        mask: Array<IntArray>,
        labels: List<String>,
        labelColors: Map<Int, Color>,
        volumeMl: Double,
        excludedClasses: Set<String> = setOf("background", "food_containers", "dining_tools")
    ): NutritionCalculationResult {
        val categories = loadCategories(context)
        
        // Подсчитываем площадь каждого класса
        val classPixels = mutableMapOf<Int, Int>()
        for (row in mask) {
            for (classId in row) {
                if (classId > 0) { // Исключаем фон (класс 0)
                    classPixels[classId] = classPixels.getOrDefault(classId, 0) + 1
                }
            }
        }
        
        // Фильтруем исключенные классы и классы без цветов
        val validClassPixels = classPixels.filter { (classId, pixels) ->
            pixels > 0 && 
            classId < labels.size && 
            !excludedClasses.contains(labels[classId].lowercase()) &&
            labelColors.containsKey(classId)
        }
        
        // Общая площадь всех валидных классов
        val totalPixels = validClassPixels.values.sum()
        if (totalPixels == 0) {
            return NutritionCalculationResult(0f, 0f, 0f, 0f, 0f)
        }
        
        var totalCalories = 0.0
        var totalProtein = 0.0
        var totalFat = 0.0
        var totalCarbs = 0.0
        
        // Расчет для каждого класса
        for ((classId, pixels) in validClassPixels) {
            val className = labels[classId]
            val category = categories.find { it.category.equals(className, ignoreCase = true) }
            
            if (category != null) {
                // Доля по площади
                val alpha = pixels.toDouble() / totalPixels.toDouble()
                
                // Плотность г/мл
                val density = category.density
                
                // Масса в граммах
                val massGrams = volumeMl * alpha * density
                
                // Добавляем к общим значениям
                totalCalories += massGrams * category.calories / 100.0
                totalProtein += massGrams * category.protein / 100.0
                totalFat += massGrams * category.fat / 100.0
                totalCarbs += massGrams * category.carbs / 100.0
            }
        }
        
        return NutritionCalculationResult(
            weight = 0f, // Will be calculated elsewhere
            calories = totalCalories.toFloat(),
            proteins = totalProtein.toFloat(),
            fats = totalFat.toFloat(),
            carbohydrates = totalCarbs.toFloat()
        )
    }
} 