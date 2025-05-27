package com.example.snapcalorie.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.snapcalorie.ui.screens.*
import kotlin.math.*

class VolumeCalculator {
    
    companion object {
        private const val TAG = "VolumeCalculator"
        private const val VOXEL_SIZE = 0.002f // 2mm voxels
        private const val MIN_DENSITY_THRESHOLD = 0.3f // Minimum point density for voxel to be considered filled
    }
    
    suspend fun calculateVolume(context: Context, volumeData: VolumeCalculationData): VolumeResult {
        Log.d(TAG, "Starting volume calculation")
        
        // Step 1: Convert depth maps to 3D point clouds
        val pointCloudTop = convertDepthToPointCloud(
            volumeData.topViewResult.arCaptureData,
            volumeData.topViewResult.mask,
            volumeData.topViewResult.labels
        )
        
        val pointCloudSide = convertDepthToPointCloud(
            volumeData.sideViewResult.arCaptureData,
            volumeData.sideViewResult.mask,
            volumeData.sideViewResult.labels
        )
        
        Log.d(TAG, "Point clouds created: top=${pointCloudTop.size}, side=${pointCloudSide.size}")
        
        // Step 2: Combine and filter point clouds
        val combinedPointCloud = combineAndFilterPointClouds(pointCloudTop, pointCloudSide)
        Log.d(TAG, "Combined point cloud size: ${combinedPointCloud.size}")
        
        // Step 3: Determine dish type and base plane
        val dishType = determineDishType(
            volumeData.topViewResult.mask,
            volumeData.topViewResult.labels
        )
        
        val baseZ = calculateBaseZ(
            combinedPointCloud,
            volumeData.topViewResult.arCaptureData.planes,
            volumeData.sideViewResult.arCaptureData.planes,
            dishType
        )
        
        Log.d(TAG, "Dish type: $dishType, Base Z: $baseZ")
        
        // Step 4: Voxelize and calculate volume
        val componentVolumes = calculateComponentVolumes(
            combinedPointCloud,
            baseZ,
            volumeData.topViewResult.mask,
            volumeData.topViewResult.labels,
            volumeData.topViewResult.classificationResult
        )
        
        val totalVolume = componentVolumes.values.sum()
        Log.d(TAG, "Total volume calculated: ${totalVolume}ml")
        
        // Step 5: Calculate nutrition
        val nutritionResults = calculateNutrition(context, componentVolumes)
        
        return VolumeResult(
            totalVolume = totalVolume,
            componentVolumes = componentVolumes,
            nutritionResults = nutritionResults,
            pointCloudTop = pointCloudTop,
            pointCloudSide = pointCloudSide,
            pointCloudCombined = combinedPointCloud,
            baseZ = baseZ,
            dishType = dishType
        )
    }
    
    private fun convertDepthToPointCloud(
        arData: ARCaptureData,
        mask: Array<IntArray>,
        labels: List<String>
    ): List<Point3D> {
        val points = mutableListOf<Point3D>()
        val pose = arData.cameraPose
        
        val maskHeight = mask.size
        val maskWidth = if (maskHeight > 0) mask[0].size else 0
        
        Log.d(TAG, "Converting depth to point cloud:")
        Log.d(TAG, "Mask size: ${maskWidth}x${maskHeight}")
        Log.d(TAG, "Depth map size: ${arData.depthWidth}x${arData.depthHeight}")
        Log.d(TAG, "Depth map array size: ${arData.depthMap.size}")
        
        // Camera intrinsics for 512x512 image
        val fx = 256.0f // focal length x (half of image width)
        val fy = 256.0f // focal length y (half of image height)
        val cx = maskWidth / 2.0f // principal point x
        val cy = maskHeight / 2.0f // principal point y
        
        for (y in 0 until maskHeight) {
            for (x in 0 until maskWidth) {
                val labelId = mask[y][x]
                
                // Only process food pixels
                if (labelId > 0 && labelId < labels.size) {
                    val label = labels[labelId].lowercase()
                    if (!setOf("background", "food_containers", "dining_tools").contains(label)) {
                        
                        // Scale coordinates to depth map size
                        val depthX = (x * arData.depthWidth / maskWidth).coerceIn(0, arData.depthWidth - 1)
                        val depthY = (y * arData.depthHeight / maskHeight).coerceIn(0, arData.depthHeight - 1)
                        
                        // Get depth value with correct indexing
                        val depthIndex = depthY * arData.depthWidth + depthX
                        if (depthIndex >= 0 && depthIndex < arData.depthMap.size) {
                            val depth = arData.depthMap[depthIndex]
                            
                            Log.v(TAG, "Pixel ($x,$y) -> depth ($depthX,$depthY) = $depth")
                            
                            if (depth > 0.1f && depth < 1.0f) { // Valid depth range for 30cm distance
                                // Convert pixel coordinates to 3D
                                val worldX = (x - cx) * depth / fx
                                val worldY = (y - cy) * depth / fy
                                val worldZ = depth
                                
                                // Transform to world coordinates using camera pose
                                val worldPoint = transformPoint(worldX, worldY, worldZ, pose)
                                points.add(worldPoint)
                            }
                        }
                    }
                }
            }
        }
        
        Log.d(TAG, "Generated ${points.size} 3D points")
        return points
    }
    
    private fun transformPoint(x: Float, y: Float, z: Float, pose: SimplePose): Point3D {
        // Transform point from camera coordinates to world coordinates
        val translation = pose.translation
        val rotation = pose.rotation
        
        // Apply rotation (simplified - should use proper quaternion rotation)
        val transformedX = x + translation[0]
        val transformedY = y + translation[1]
        val transformedZ = z + translation[2]
        
        return Point3D(transformedX, transformedY, transformedZ)
    }
    
    private fun combineAndFilterPointClouds(
        pointCloudTop: List<Point3D>,
        pointCloudSide: List<Point3D>
    ): List<Point3D> {
        val combined = pointCloudTop + pointCloudSide
        
        // Apply median filtering to remove outliers
        return filterOutliers(combined)
    }
    
    private fun filterOutliers(points: List<Point3D>): List<Point3D> {
        if (points.size < 10) return points
        
        // Calculate median position
        val sortedX = points.map { it.x }.sorted()
        val sortedY = points.map { it.y }.sorted()
        val sortedZ = points.map { it.z }.sorted()
        
        val medianX = sortedX[sortedX.size / 2]
        val medianY = sortedY[sortedY.size / 2]
        val medianZ = sortedZ[sortedZ.size / 2]
        
        // Calculate standard deviation
        val meanX = points.map { it.x }.average().toFloat()
        val meanY = points.map { it.y }.average().toFloat()
        val meanZ = points.map { it.z }.average().toFloat()
        
        val stdX = sqrt(points.map { (it.x - meanX).pow(2) }.average()).toFloat()
        val stdY = sqrt(points.map { (it.y - meanY).pow(2) }.average()).toFloat()
        val stdZ = sqrt(points.map { (it.z - meanZ).pow(2) }.average()).toFloat()
        
        // Filter points within 2 standard deviations
        return points.filter { point ->
            abs(point.x - meanX) <= 2 * stdX &&
            abs(point.y - meanY) <= 2 * stdY &&
            abs(point.z - meanZ) <= 2 * stdZ
        }
    }
    
    private fun determineDishType(mask: Array<IntArray>, labels: List<String>): DishType {
        // Find food_containers class
        val containerClassId = labels.indexOfFirst { it.lowercase() == "food_containers" }
        if (containerClassId == -1) return DishType.FLAT_PLATE
        
        // Analyze the shape of the container mask
        var containerPixels = 0
        var boundingBox = IntArray(4) { if (it < 2) Int.MAX_VALUE else Int.MIN_VALUE } // minX, minY, maxX, maxY
        
        for (y in mask.indices) {
            for (x in mask[y].indices) {
                if (mask[y][x] == containerClassId) {
                    containerPixels++
                    boundingBox[0] = minOf(boundingBox[0], x) // minX
                    boundingBox[1] = minOf(boundingBox[1], y) // minY
                    boundingBox[2] = maxOf(boundingBox[2], x) // maxX
                    boundingBox[3] = maxOf(boundingBox[3], y) // maxY
                }
            }
        }
        
        if (containerPixels == 0) return DishType.FLAT_PLATE
        
        // Calculate aspect ratio and area ratio
        val width = boundingBox[2] - boundingBox[0]
        val height = boundingBox[3] - boundingBox[1]
        val boundingArea = width * height
        val fillRatio = containerPixels.toFloat() / boundingArea
        
        // If the container has a hole in the middle (low fill ratio), it's likely a deep plate
        return if (fillRatio < 0.7f) DishType.DEEP_PLATE else DishType.FLAT_PLATE
    }
    
    private fun calculateBaseZ(
        points: List<Point3D>,
        topPlanes: List<SimplePlane>,
        sidePlanes: List<SimplePlane>,
        dishType: DishType
    ): Float {
        if (points.isEmpty()) return 0f
        
        // Find the lowest Z value (table surface)
        val minZ = points.minOf { it.z }
        
        // For deep plates, add some offset for the inner bottom
        return when (dishType) {
            DishType.DEEP_PLATE -> minZ + 0.02f // 2cm offset for deep plate bottom
            DishType.FLAT_PLATE -> minZ + 0.005f // 5mm offset for flat plate
        }
    }
    
    private fun calculateComponentVolumes(
        points: List<Point3D>,
        baseZ: Float,
        mask: Array<IntArray>,
        labels: List<String>,
        classificationResult: String?
    ): Map<String, Float> {
        if (points.isEmpty()) {
            Log.w(TAG, "No points for volume calculation")
            return emptyMap()
        }
        
        Log.d(TAG, "Calculating volume from ${points.size} points")
        Log.d(TAG, "Base Z: $baseZ")
        
        // Find bounding box
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        val maxZ = points.maxOf { it.z }
        
        Log.d(TAG, "Bounding box: X[$minX, $maxX], Y[$minY, $maxY], Z[$baseZ, $maxZ]")
        
        // Create voxel grid
        val voxelsX = ((maxX - minX) / VOXEL_SIZE).toInt() + 1
        val voxelsY = ((maxY - minY) / VOXEL_SIZE).toInt() + 1
        val voxelsZ = ((maxZ - baseZ) / VOXEL_SIZE).toInt() + 1
        
        Log.d(TAG, "Voxel grid size: ${voxelsX}x${voxelsY}x${voxelsZ}")
        
        if (voxelsX <= 0 || voxelsY <= 0 || voxelsZ <= 0) {
            Log.w(TAG, "Invalid voxel grid dimensions")
            return emptyMap()
        }
        
        // Count points in each voxel
        val voxelCounts = Array(voxelsX) { Array(voxelsY) { IntArray(voxelsZ) } }
        var pointsAboveBase = 0
        
        points.forEach { point ->
            if (point.z >= baseZ) {
                pointsAboveBase++
                val voxelX = ((point.x - minX) / VOXEL_SIZE).toInt().coerceIn(0, voxelsX - 1)
                val voxelY = ((point.y - minY) / VOXEL_SIZE).toInt().coerceIn(0, voxelsY - 1)
                val voxelZ = ((point.z - baseZ) / VOXEL_SIZE).toInt().coerceIn(0, voxelsZ - 1)
                
                voxelCounts[voxelX][voxelY][voxelZ]++
            }
        }
        
        Log.d(TAG, "Points above base: $pointsAboveBase")
        
        // Calculate expected points per voxel for density threshold
        val totalVoxels = voxelsX * voxelsY * voxelsZ
        val avgPointsPerVoxel = if (totalVoxels > 0) pointsAboveBase.toFloat() / totalVoxels else 0f
        val minPointsThreshold = maxOf(1, (avgPointsPerVoxel * MIN_DENSITY_THRESHOLD).toInt())
        
        Log.d(TAG, "Average points per voxel: $avgPointsPerVoxel, threshold: $minPointsThreshold")
        
        // Count filled voxels
        var filledVoxels = 0
        var maxPointsInVoxel = 0
        
        for (x in 0 until voxelsX) {
            for (y in 0 until voxelsY) {
                for (z in 0 until voxelsZ) {
                    val pointCount = voxelCounts[x][y][z]
                    maxPointsInVoxel = maxOf(maxPointsInVoxel, pointCount)
                    
                    if (pointCount >= minPointsThreshold) {
                        filledVoxels++
                    }
                }
            }
        }
        
        Log.d(TAG, "Filled voxels: $filledVoxels, max points in voxel: $maxPointsInVoxel")
        
        // Calculate volume
        val voxelVolume = VOXEL_SIZE * VOXEL_SIZE * VOXEL_SIZE * 1000000 // Convert to ml (m³ to ml)
        val totalVolume = filledVoxels * voxelVolume
        
        Log.d(TAG, "Voxel volume: ${voxelVolume}ml, total volume: ${totalVolume}ml")
        
        Log.d(TAG, "All labels: ${labels.joinToString()}")
        Log.d(TAG, "Classification result: $classificationResult")
        
        // Prioritize classification result from server
        val selectedClass = if (classificationResult != null && classificationResult != "unknown") {
            Log.d(TAG, "Using classification result: $classificationResult")
            classificationResult
        } else {
            // Fallback to first detected food class
            val foodClasses = labels.withIndex()
                .filter { (_, label) -> 
                    !setOf("background", "food_containers", "dining_tools").contains(label.lowercase())
                }
                .map { (_, label) -> label }
            
            Log.d(TAG, "Food classes found: $foodClasses")
            
            if (foodClasses.isNotEmpty()) {
                val fallbackClass = foodClasses.first()
                Log.d(TAG, "Using fallback food class: $fallbackClass")
                fallbackClass
            } else {
                Log.d(TAG, "No food classes found, using unknown_food")
                "unknown_food"
            }
        }
        
        Log.d(TAG, "Final selected class: $selectedClass")
        return mapOf(selectedClass to totalVolume)
    }
    
    private suspend fun calculateNutrition(context: Context, componentVolumes: Map<String, Float>): Map<String, NutritionCalculationResult> {
        val nutritionLoader = NutritionDataLoader()
        val categories = nutritionLoader.loadCategories(context)
        val results = mutableMapOf<String, NutritionCalculationResult>()
        
        Log.d(TAG, "Calculating nutrition for components: ${componentVolumes.keys.joinToString()}")
        
        componentVolumes.forEach { (component, volume) ->
            Log.d(TAG, "Processing component: $component, volume: ${volume}ml")
            
            // Find nutrition data for this component
            val category = categories.find { it.category.equals(component, ignoreCase = true) }
            
            if (category != null) {
                Log.d(TAG, "Found category data for $component: density=${category.density}, calories=${category.calories}")
                
                // Get density and convert volume to weight
                val density = category.density.toFloat() // density in g/ml
                val weight = volume * density // Convert volume to weight in grams
                
                // Calculate nutrition based on weight (categories.json data is per 100g)
                val factor = weight / 100f
                
                results[component] = NutritionCalculationResult(
                    weight = weight,
                    calories = category.calories * factor,
                    proteins = category.protein.toFloat() * factor,
                    fats = category.fat.toFloat() * factor,
                    carbohydrates = category.carbs.toFloat() * factor
                )
                
                Log.d(TAG, "Calculated nutrition for $component: weight=${weight}g, calories=${category.calories * factor}")
            } else {
                Log.w(TAG, "No category data found for $component, using defaults")
                
                // Default values if nutrition data not found
                val defaultDensity = 0.8f // Default density
                val weight = volume * defaultDensity
                
                results[component] = NutritionCalculationResult(
                    weight = weight,
                    calories = weight * 2.0f, // 2 kcal/g default
                    proteins = weight * 0.1f,
                    fats = weight * 0.05f,
                    carbohydrates = weight * 0.3f
                )
            }
        }
        
        return results
    }
} 