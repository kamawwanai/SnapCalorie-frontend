package com.example.snapcalorie.util

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.tflite.java.TfLite
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.InterpreterApi.Options.TfLiteRuntime
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import androidx.core.graphics.scale
import kotlinx.coroutines.tasks.await

class SegmentationModel private constructor(
    private val interpreter: InterpreterApi,
    private val labels: List<String>,
    private val inputHeight: Int,
    private val inputWidth: Int,
    private val inputChannels: Int,
    private val numClasses: Int
) {

    companion object {
        suspend fun create(context: Context): SegmentationModel {
            // 1) Инициализация TfLite из Google Play Services (асинхронно)
            TfLite.initialize(context).await()
            
            // 2) Загрузка модели из assets/model.tflite
            val afd = context.assets.openFd("seg_model.tflite")
            val fileStream = FileInputStream(afd.fileDescriptor)
            val fileChannel: FileChannel = fileStream.channel
            val modelBuffer: ByteBuffer = fileChannel
                .map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)

            // 3) Настраиваем Interpreter с Google Play Services runtime
            val options = InterpreterApi.Options().apply {
                // Используем Google Play Services runtime
                setRuntime(TfLiteRuntime.FROM_SYSTEM_ONLY)
                
                // Проверяем поддержку GPU через Google Play Services (асинхронно)
                try {
                    val isGpuAvailable = TfLiteGpu.isGpuDelegateAvailable(context).await()
                    if (isGpuAvailable) {
                        // GPU поддерживается, но для простоты используем CPU
                        // GPU делегат требует более сложной асинхронной инициализации
                        setNumThreads(4)
                    } else {
                        // GPU не поддерживается, используем CPU с несколькими потоками
                        setNumThreads(4)
                    }
                } catch (e: Exception) {
                    // В случае ошибки используем CPU
                    setNumThreads(4)
                }
            }
            val interpreter = InterpreterApi.create(modelBuffer, options)
            interpreter.allocateTensors()

            // 4) Считываем форму входного тензора [1, H, W, C]
            val inShape = interpreter.getInputTensor(0).shape()
            val inputHeight = inShape[1]
            val inputWidth = inShape[2]
            val inputChannels = inShape[3]

            // 5) Считываем форму выходного тензора [1, H, W, numClasses]
            val outShape = interpreter.getOutputTensor(0).shape()
            val numClasses = outShape[3]

            // 6) Загружаем labels.txt — должно быть ровно numClasses строк
            val labels = context.assets.open("labels.txt")
                .bufferedReader()
                .useLines { it.toList() }

            require(labels.size == numClasses) {
                "labels.txt должна содержать $numClasses строк, а нашла ${labels.size}"
            }

            return SegmentationModel(
                interpreter = interpreter,
                labels = labels,
                inputHeight = inputHeight,
                inputWidth = inputWidth,
                inputChannels = inputChannels,
                numClasses = numClasses
            )
        }
    }

    /**
     * Прогон Bitmap через модель → 2D-маска [inputHeight][inputWidth].
     * Каждый элемент mask[y][x] ∈ 0..numClasses-1.
     */
    fun segmentToMask(bitmap: Bitmap): Array<IntArray> {
        // 1) Ресайз bitmap под вход модели
        val resized = bitmap.scale(inputWidth, inputHeight)

        // 2) Подготовка ByteBuffer для RGB UINT8
        val bytePerChannel = 1
        val inputBuffer = ByteBuffer
            .allocateDirect(inputHeight * inputWidth * inputChannels * bytePerChannel)
            .order(ByteOrder.nativeOrder())
        val px = IntArray(inputWidth * inputHeight)
        resized.getPixels(px, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        for (pixel in px) {
            // R, G, B
            inputBuffer.put(((pixel shr 16) and 0xFF).toByte())
            inputBuffer.put(((pixel shr 8)  and 0xFF).toByte())
            inputBuffer.put(((pixel       ) and 0xFF).toByte())
        }
        inputBuffer.rewind()

        // 3) Подготовка выходного буфера: [1][H][W][numClasses] - правильная форма
        val batch = 1
        val outputBuffer = Array(batch) {
            Array(inputHeight) {
                Array(inputWidth) {
                    FloatArray(numClasses)
                }
            }
        }

        // 4) Инференс
        val inputs = arrayOf<Any>(inputBuffer)
        val outputs = mapOf<Int, Any>(0 to outputBuffer)
        interpreter.runForMultipleInputsOutputs(inputs, outputs)

        // 5) ArgMax → mask[y][x]
        val mask = Array(inputHeight) { IntArray(inputWidth) }
        for (y in 0 until inputHeight) {
            for (x in 0 until inputWidth) {
                var best = 0
                var bestScore = outputBuffer[0][y][x][0]
                for (c in 1 until numClasses) {
                    val score = outputBuffer[0][y][x][c]
                    if (score > bestScore) {
                        bestScore = score
                        best = c
                    }
                }
                mask[y][x] = best
            }
        }
        return mask
    }

    /**
     * Получить список лейблов классов
     */
    fun getLabels(): List<String> {
        return labels
    }

    /** Освобождаем ресурсы делегатов */
    fun close() {
        interpreter.close()
    }
} 