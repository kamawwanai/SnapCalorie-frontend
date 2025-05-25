package com.example.snapcalorie.util

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.max

object MaskProcessor {
    
    /**
     * Улучшает маску сегментации для всех классов кроме фона
     */
    fun refineMultiClassMask(
        originalBitmap: Bitmap,
        mask: Array<IntArray>,
        numClasses: Int,
        backgroundClassId: Int = 0
    ): Array<IntArray> {
        // Конвертируем маску в OpenCV Mat
        val origMask = maskToMat(mask)
        
        val cleaned = mutableListOf<Mat>()
        
        for (k in 0 until numClasses) {
            if (k == backgroundClassId) {
                // Для фона просто извлекаем маску без обработки
                val backgroundMask = extractClassMask(origMask, k)
                cleaned.add(backgroundMask)
            } else {
                // 1) бинарная маска класса k
                val bin = extractClassMask(origMask, k)
                
                // 2) морфология
                val morph = cleanBinaryMask(bin, kernelSize = 5)
                bin.release()
                
                // 3) удаляем артефакты (<100 px)
                val noSmall = removeSmallComponents(morph, minArea = 10)
                morph.release()
                
                // 4) сглаживаем контуры (опционально)
                val smooth = smoothContours(noSmall, epsilon = 2.0)
                noSmall.release()
                
                cleaned.add(smooth)
            }
        }
        
        // 5) собираем воедино
        val merged = mergeClasses(cleaned)
        cleaned.forEach { it.release() }
        
        // Конвертируем обратно в Array<IntArray>
        val result = matToMask(merged)
        
        origMask.release()
        merged.release()
        
        return result
    }
    
    /**
     * Извлекает бинарную маску для конкретного класса
     */
    private fun extractClassMask(mask: Mat, classId: Int): Mat {
        val binary = Mat()
        Core.compare(mask, Scalar(classId.toDouble()), binary, Core.CMP_EQ)
        return binary
    }
    
    /**
     * Очищает бинарную маску с помощью морфологических операций
     */
    private fun cleanBinaryMask(mask: Mat, kernelSize: Int): Mat {
        val kernel = Imgproc.getStructuringElement(
            Imgproc.MORPH_ELLIPSE,
            Size(kernelSize.toDouble(), kernelSize.toDouble())
        )
        
        val opened = Mat()
        val closed = Mat()
        
        // Открытие (удаляет шум)
        Imgproc.morphologyEx(mask, opened, Imgproc.MORPH_OPEN, kernel)
        
        // Закрытие (заполняет дыры)
        Imgproc.morphologyEx(opened, closed, Imgproc.MORPH_CLOSE, kernel)
        
        opened.release()
        kernel.release()
        
        return closed
    }
    
    /**
     * Удаляет маленькие компоненты
     */
    private fun removeSmallComponents(mask: Mat, minArea: Int): Mat {
        val labels = Mat()
        val stats = Mat()
        val centroids = Mat()
        
        val numComponents = Imgproc.connectedComponentsWithStats(
            mask, labels, stats, centroids, 8, CvType.CV_32S
        )
        
        val result = Mat.zeros(mask.size(), CvType.CV_8UC1)
        
        for (i in 1 until numComponents) { // Пропускаем фон (компонент 0)
            val area = stats.get(i, Imgproc.CC_STAT_AREA)[0]
            if (area >= minArea) {
                // Копируем компонент в результат
                val componentMask = Mat()
                Core.compare(labels, Scalar(i.toDouble()), componentMask, Core.CMP_EQ)
                result.setTo(Scalar(255.0), componentMask)
                componentMask.release()
            }
        }
        
        labels.release()
        stats.release()
        centroids.release()
        
        return result
    }
    
    /**
     * Сглаживает контуры
     */
    private fun smoothContours(mask: Mat, epsilon: Double): Mat {
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        
        Imgproc.findContours(
            mask, contours, hierarchy, 
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
        )
        
        val result = Mat.zeros(mask.size(), CvType.CV_8UC1)
        
        for (contour in contours) {
            val approx = MatOfPoint2f()
            val contour2f = MatOfPoint2f()
            contour.convertTo(contour2f, CvType.CV_32FC2)
            
            val arcLength = Imgproc.arcLength(contour2f, true)
            Imgproc.approxPolyDP(contour2f, approx, epsilon * arcLength / 100.0, true)
            
            val smoothedContour = MatOfPoint()
            approx.convertTo(smoothedContour, CvType.CV_32S)
            
            Imgproc.fillPoly(result, listOf(smoothedContour), Scalar(255.0))
            
            contour2f.release()
            approx.release()
            smoothedContour.release()
        }
        
        contours.forEach { it.release() }
        hierarchy.release()
        
        return result
    }
    
    /**
     * Объединяет маски классов в одну многоклассовую маску
     */
    private fun mergeClasses(classMasks: List<Mat>): Mat {
        if (classMasks.isEmpty()) {
            throw IllegalArgumentException("Class masks list cannot be empty")
        }
        
        val result = Mat.zeros(classMasks[0].size(), CvType.CV_8UC1)
        
        classMasks.forEachIndexed { classId, mask ->
            result.setTo(Scalar(classId.toDouble()), mask)
        }
        
        return result
    }
    
    /**
     * Конвертирует Array<IntArray> в OpenCV Mat
     */
    private fun maskToMat(mask: Array<IntArray>): Mat {
        val height = mask.size
        val width = if (height > 0) mask[0].size else 0
        
        val mat = Mat(height, width, CvType.CV_8UC1)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                mat.put(y, x, mask[y][x].toDouble())
            }
        }
        
        return mat
    }
    
    /**
     * Конвертирует OpenCV Mat в Array<IntArray>
     */
    private fun matToMask(mat: Mat): Array<IntArray> {
        val height = mat.rows()
        val width = mat.cols()
        
        val mask = Array(height) { IntArray(width) }
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                mask[y][x] = mat.get(y, x)[0].toInt()
            }
        }
        
        return mask
    }
} 