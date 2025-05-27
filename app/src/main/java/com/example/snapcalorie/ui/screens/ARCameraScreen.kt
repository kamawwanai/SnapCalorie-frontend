package com.example.snapcalorie.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.snapcalorie.ui.components.Screen
import com.example.snapcalorie.ui.theme.*
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.IOException
import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ARCaptureData(
    val rgbFrame: Bitmap,
    val depthMap: FloatArray,
    val cameraPose: SimplePose,
    val planes: List<SimplePlane>,
    val anchors: List<SimpleAnchor>,
    val depthWidth: Int,
    val depthHeight: Int
)

// Simple data classes to replace ARCore dependencies
data class SimplePose(
    val translation: FloatArray,
    val rotation: FloatArray
) {
    fun tx() = translation[0]
    fun ty() = translation[1] 
    fun tz() = translation[2]
}

data class SimplePlane(
    val centerPose: SimplePose,
    val extentX: Float,
    val extentZ: Float
)

data class SimpleAnchor(
    val pose: SimplePose
)

enum class CaptureStage {
    TOP_VIEW, SIDE_VIEW
}

@Composable
fun ARCameraScreen(
    captureStage: CaptureStage,
    onCaptureComplete: (ARCaptureData) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var midasInterpreter by remember { mutableStateOf<Interpreter?>(null) }
    var cameraState by remember { mutableStateOf("Initializing...") }
    var isCameraReady by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<androidx.camera.core.ImageCapture?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
            hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        }
    )

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (!hasCameraPermission) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        if (!hasLocationPermission) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // Initialize MiDaS depth estimation model
    LaunchedEffect(hasCameraPermission, hasLocationPermission) {
        if (hasCameraPermission && hasLocationPermission) {
            cameraState = "Загрузка модели MiDaS..."
            
            initializeMiDaS(context) { interpreter ->
                midasInterpreter = interpreter
                isCameraReady = true
                cameraState = when (captureStage) {
                    CaptureStage.TOP_VIEW -> "Наведите камеру сверху на блюдо\nГлубина: MiDaS нейросеть"
                    CaptureStage.SIDE_VIEW -> "Снимите блюдо сбоку (угол 30–60°)\nГлубина: MiDaS нейросеть"
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            midasInterpreter?.let { interpreter ->
                try {
                    interpreter.close()
                    Log.d("MiDaS", "MiDaS interpreter closed")
                } catch (e: Exception) {
                    Log.e("MiDaS", "Error closing MiDaS interpreter", e)
                }
            }
        }
    }

    fun captureARData() {
        val interpreter = midasInterpreter ?: run {
            cameraState = "Ошибка: MiDaS модель не готова"
            Log.e("MiDaS", "MiDaS interpreter is null")
            return
        }
        
        if (!isCameraReady) {
            cameraState = "MiDaS ещё не готов, подождите..."
            Log.w("MiDaS", "MiDaS not ready for capture")
            return
        }
        
        val imageCaptureUseCase = imageCapture ?: run {
            cameraState = "Ошибка: Камера не готова к съемке"
            Log.e("MiDaS", "ImageCapture is null")
            return
        }
        
        isCapturing = true
        cameraState = "Захват изображения..."
        Log.d("MiDaS", "Starting capture process with MiDaS depth estimation...")
        
        // Create temp file for capture
        val tempFile = try {
            java.io.File.createTempFile("capture", ".jpg", context.cacheDir)
        } catch (e: Exception) {
            Log.e("MiDaS", "Failed to create temp file", e)
            cameraState = "Ошибка создания файла: ${e.message}"
            isCapturing = false
            return
        }
        
        // Capture real image from camera
        imageCaptureUseCase.takePicture(
            androidx.camera.core.ImageCapture.OutputFileOptions.Builder(tempFile).build(),
            ContextCompat.getMainExecutor(context),
            object : androidx.camera.core.ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: androidx.camera.core.ImageCapture.OutputFileResults) {
                    try {
                        Log.d("MiDaS", "Image saved successfully, processing...")
                        
                        // Load the captured image from temp file
                        val originalBitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                        if (originalBitmap == null) {
                            Log.e("MiDaS", "Failed to decode captured image from: ${tempFile.absolutePath}")
                            cameraState = "Ошибка: не удалось загрузить изображение"
                            isCapturing = false
                            return
                        }
                        
                        Log.d("MiDaS", "Captured original image: ${originalBitmap.width}x${originalBitmap.height}")
                        
                        // Crop to square 512x512 from center
                        val rgbBitmap = cropToSquare(originalBitmap, 512)
                        Log.d("MiDaS", "Cropped to square: ${rgbBitmap.width}x${rgbBitmap.height}")
                        Log.d("MiDaS", "Starting MiDaS processing...")
                        
                        // Process with MiDaS to get depth map
                        val depthData = try {
                            val rawDepthData = processImageWithMiDaS(rgbBitmap, interpreter)
                            
                            // Ensure depth map matches image dimensions
                            val expectedSize = rgbBitmap.width * rgbBitmap.height
                            if (rawDepthData.size != expectedSize) {
                                Log.w("MiDaS", "Depth map size mismatch: ${rawDepthData.size} vs expected $expectedSize")
                                // Resize depth map to match image dimensions
                                resizeDepthMap(rawDepthData, rgbBitmap.width, rgbBitmap.height)
                            } else {
                                rawDepthData
                            }
                        } catch (e: Exception) {
                            Log.e("MiDaS", "Error in MiDaS processing", e)
                            cameraState = "Ошибка MiDaS: ${e.message}"
                            isCapturing = false
                            return
                        }
                        
                        Log.d("MiDaS", "MiDaS processing completed, creating pose...")
                        
                        // Create simple pose based on capture stage
                        val cameraPose = when (captureStage) {
                            CaptureStage.TOP_VIEW -> SimplePose(
                                translation = floatArrayOf(0f, 0f, -0.30f), // 30cm away, looking down
                                rotation = floatArrayOf(0f, 0f, 0f, 1f) // No rotation
                            )
                            CaptureStage.SIDE_VIEW -> SimplePose(
                                translation = floatArrayOf(0.30f, 0f, -0.15f), // 30cm to the side, 15cm back
                                rotation = floatArrayOf(0f, 0.707f, 0f, 0.707f) // 90 degree rotation around Y axis
                            )
                        }
                        
                        // Create empty planes and anchors for compatibility
                        val planes = emptyList<SimplePlane>()
                        val anchors = emptyList<SimpleAnchor>()
                        
                        Log.d("MiDaS", "Creating ARCaptureData...")
                        
                        val captureData = ARCaptureData(
                            rgbFrame = rgbBitmap,
                            depthMap = depthData,
                            cameraPose = cameraPose,
                            planes = planes,
                            anchors = anchors,
                            depthWidth = rgbBitmap.width,
                            depthHeight = rgbBitmap.height
                        )
                        
                        Log.d("MiDaS", "Successfully processed real image with MiDaS - RGB: ${rgbBitmap.width}x${rgbBitmap.height}, Depth: ${depthData.size} values")
                        Log.d("MiDaS", "Bitmap config: ${rgbBitmap.config}, isRecycled: ${rgbBitmap.isRecycled}")
                        
                        // Clean up temp file
                        try {
                            tempFile.delete()
                            Log.d("MiDaS", "Temp file deleted")
                        } catch (e: Exception) {
                            Log.w("MiDaS", "Failed to delete temp file", e)
                        }
                        
                        isCapturing = false
                        Log.d("MiDaS", "Calling onCaptureComplete with data")
                        
                        try {
                            onCaptureComplete(captureData)
                            Log.d("MiDaS", "onCaptureComplete called successfully")
                        } catch (e: Exception) {
                            Log.e("MiDaS", "Error in onCaptureComplete callback", e)
                            cameraState = "Ошибка навигации: ${e.message}"
                        }
                        
                    } catch (e: Exception) {
                        Log.e("MiDaS", "Error processing captured image", e)
                        cameraState = "Ошибка обработки: ${e.message}"
                        isCapturing = false
                        
                        // Clean up temp file on error
                        try {
                            tempFile.delete()
                        } catch (deleteError: Exception) {
                            Log.w("MiDaS", "Failed to delete temp file on error", deleteError)
                        }
                    }
                }
                
                override fun onError(exception: androidx.camera.core.ImageCaptureException) {
                    Log.e("MiDaS", "Image capture failed", exception)
                    when (exception.imageCaptureError) {
                        androidx.camera.core.ImageCapture.ERROR_INVALID_CAMERA -> {
                            cameraState = "Ошибка камеры. Попробуйте перезапустить экран"
                        }
                        androidx.camera.core.ImageCapture.ERROR_CAMERA_CLOSED -> {
                            cameraState = "Камера закрыта. Попробуйте снова"
                        }
                        else -> {
                            cameraState = "Ошибка захвата: ${exception.message}"
                        }
                    }
                    isCapturing = false
                }
            }
        )
    }



    Box(modifier = Modifier.fillMaxSize()) {
        // Simple camera preview using CameraX for display only
        if (hasCameraPermission && hasLocationPermission) {
            AndroidView(
                factory = { context ->
                    androidx.camera.view.PreviewView(context).apply {
                        implementationMode = androidx.camera.view.PreviewView.ImplementationMode.COMPATIBLE
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        val preview = androidx.camera.core.Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        // Create ImageCapture use case here with 512x512 resolution
                        val imageCaptureUseCase = androidx.camera.core.ImageCapture.Builder()
                            .setCaptureMode(androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .setTargetResolution(android.util.Size(512, 512))
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            
                            // Bind both preview and imageCapture
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCaptureUseCase
                            )
                            
                            // Set imageCapture only after successful binding
                            imageCapture = imageCaptureUseCase
                            
                            Log.d("MiDaS", "Camera bound successfully with preview and imageCapture")
                            
                            // Update camera state to indicate readiness
                            if (isCameraReady) {
                                cameraState = when (captureStage) {
                                    CaptureStage.TOP_VIEW -> "Готов к съемке сверху\nГлубина: MiDaS нейросеть"
                                    CaptureStage.SIDE_VIEW -> "Готов к съемке сбоку\nГлубина: MiDaS нейросеть"
                                }
                            }
                            
                        } catch (e: Exception) {
                            Log.e("MiDaS", "Failed to bind camera", e)
                            cameraState = "Ошибка привязки камеры: ${e.message}"
                            imageCapture = null
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )
        } else {
            // Show placeholder while permissions are being requested
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (!hasCameraPermission || !hasLocationPermission) {
                        "Ожидание разрешений камеры..."
                    } else {
                        "Инициализация MiDaS..."
                    },
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }

        // Square viewfinder overlay
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Square frame overlay
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .background(Color.Transparent)
                    .border(2.dp, Color.White, RoundedCornerShape(8.dp))
            )
        }

        // HUD overlay
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top HUD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = cameraState,
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Capture button
                Button(
                    onClick = { captureARData() },
                    enabled = isCameraReady && !isCapturing && imageCapture != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green50,
                        contentColor = Base0
                    )
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Base0
                        )
                    } else {
                        Text(
                            text = when (captureStage) {
                                CaptureStage.TOP_VIEW -> "Снять верхний вид"
                                CaptureStage.SIDE_VIEW -> "Снять боковой вид"
                            },
                            fontFamily = montserratFamily,
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Back button
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Green50
                    )
                ) {
                    Text(
                        text = "Назад",
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

private fun cropToSquare(bitmap: Bitmap, targetSize: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    
    // Determine the size of the square (minimum of width and height)
    val squareSize = minOf(width, height)
    
    // Calculate the starting coordinates for cropping (center crop)
    val x = (width - squareSize) / 2
    val y = (height - squareSize) / 2
    
    // Crop to square
    val squareBitmap = Bitmap.createBitmap(bitmap, x, y, squareSize, squareSize)
    
    // Scale to target size
    val scaledBitmap = Bitmap.createScaledBitmap(squareBitmap, targetSize, targetSize, true)
    
    // Clean up intermediate bitmap if it's different from the original
    if (squareBitmap != bitmap) {
        squareBitmap.recycle()
    }
    
    return scaledBitmap
}

private suspend fun initializeMiDaS(context: android.content.Context, onSuccess: (Interpreter) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            Log.d("MiDaS", "Loading MiDaS model from assets")
            
            // Load MiDaS model from assets
            val modelBuffer = try {
                FileUtil.loadMappedFile(context, "midas.tflite")
            } catch (e: IOException) {
                Log.e("MiDaS", "Failed to load MiDaS model", e)
                return@withContext
            }
            
            // Create interpreter with optimizations
            val options = Interpreter.Options().apply {
                setNumThreads(4) // Use 4 threads for better performance
                setUseNNAPI(true) // Use Android Neural Networks API if available
            }
            
            val interpreter = Interpreter(modelBuffer, options)
            
            // Get model input/output info
            val inputShape = interpreter.getInputTensor(0).shape()
            val outputShape = interpreter.getOutputTensor(0).shape()
            
            Log.d("MiDaS", "Model loaded successfully")
            Log.d("MiDaS", "Input shape: [${inputShape.joinToString(", ")}]")
            Log.d("MiDaS", "Output shape: [${outputShape.joinToString(", ")}]")
            
            withContext(Dispatchers.Main) {
                onSuccess(interpreter)
                Log.d("MiDaS", "MiDaS initialization completed successfully")
            }
            
        } catch (e: Exception) {
            Log.e("MiDaS", "Error initializing MiDaS", e)
        }
    }
}

private fun resizeDepthMapInternal(depthMap: FloatArray, sourceWidth: Int, sourceHeight: Int, targetWidth: Int, targetHeight: Int): FloatArray {
    val targetSize = targetWidth * targetHeight
    val resizedDepthMap = FloatArray(targetSize)
    
    for (y in 0 until targetHeight) {
        for (x in 0 until targetWidth) {
            // Map target coordinates to source coordinates
            val sourceX = (x * sourceWidth / targetWidth).coerceIn(0, sourceWidth - 1)
            val sourceY = (y * sourceHeight / targetHeight).coerceIn(0, sourceHeight - 1)
            
            val sourceIndex = sourceY * sourceWidth + sourceX
            val targetIndex = y * targetWidth + x
            
            if (sourceIndex < depthMap.size) {
                resizedDepthMap[targetIndex] = depthMap[sourceIndex]
            } else {
                resizedDepthMap[targetIndex] = 0.15f // Default depth
            }
        }
    }
    
    return resizedDepthMap
}

private fun resizeDepthMap(depthMap: FloatArray, targetWidth: Int, targetHeight: Int): FloatArray {
    val targetSize = targetWidth * targetHeight
    
    // Calculate source dimensions (assuming square output from MiDaS)
    val sourceSize = depthMap.size
    val sourceWidth = kotlin.math.sqrt(sourceSize.toDouble()).toInt()
    val sourceHeight = sourceSize / sourceWidth
    
    Log.d("MiDaS", "Resizing depth map from ${sourceWidth}x${sourceHeight} to ${targetWidth}x${targetHeight}")
    
    val resizedDepthMap = FloatArray(targetSize)
    
    for (y in 0 until targetHeight) {
        for (x in 0 until targetWidth) {
            // Map target coordinates to source coordinates
            val sourceX = (x * sourceWidth / targetWidth).coerceIn(0, sourceWidth - 1)
            val sourceY = (y * sourceHeight / targetHeight).coerceIn(0, sourceHeight - 1)
            
            val sourceIndex = sourceY * sourceWidth + sourceX
            val targetIndex = y * targetWidth + x
            
            if (sourceIndex < depthMap.size) {
                resizedDepthMap[targetIndex] = depthMap[sourceIndex]
            } else {
                resizedDepthMap[targetIndex] = 0.15f // Default depth
            }
        }
    }
    
    return resizedDepthMap
}

private fun processImageWithMiDaS(bitmap: Bitmap, interpreter: Interpreter): FloatArray {
    try {
        Log.d("MiDaS", "Starting MiDaS depth estimation...")
        Log.d("MiDaS", "Input bitmap: ${bitmap.width}x${bitmap.height}, config: ${bitmap.config}")
        
        // Get model input shape
        val inputShape = try {
            interpreter.getInputTensor(0).shape()
        } catch (e: Exception) {
            Log.e("MiDaS", "Error getting input tensor shape", e)
            throw e
        }
        
        val inputHeight = inputShape[1]
        val inputWidth = inputShape[2]
        val inputChannels = inputShape[3]
        
        Log.d("MiDaS", "Model input shape: [${inputShape.joinToString(", ")}]")
        
        // Resize bitmap to model input size
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        
        // Convert bitmap to float array (normalized to [0, 1])
        val inputBuffer = ByteBuffer.allocateDirect(4 * inputHeight * inputWidth * inputChannels)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        val pixels = IntArray(inputWidth * inputHeight)
        resizedBitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        
        // Convert to normalized RGB values
        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            
            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }
        
        // Prepare output buffer
        val outputShape = interpreter.getOutputTensor(0).shape()
        val outputHeight = outputShape[1]
        val outputWidth = outputShape[2]
        val outputSize = outputHeight * outputWidth
        
        Log.d("MiDaS", "Model output shape: [${outputShape.joinToString(", ")}]")
        
        val outputBuffer = ByteBuffer.allocateDirect(4 * outputSize)
        outputBuffer.order(ByteOrder.nativeOrder())
        
        // Run inference
        Log.d("MiDaS", "Running MiDaS inference...")
        interpreter.run(inputBuffer, outputBuffer)
        
        // Convert output to float array
        outputBuffer.rewind()
        val rawDepthData = FloatArray(outputSize)
        for (i in 0 until outputSize) {
            rawDepthData[i] = outputBuffer.float
        }
        
        // Normalize depth values to reasonable range (0.05m to 0.5m for close-up food photography)
        val minDepth = rawDepthData.minOrNull() ?: 0f
        val maxDepth = rawDepthData.maxOrNull() ?: 1f
        val depthRange = maxDepth - minDepth
        
        for (i in rawDepthData.indices) {
            // Normalize to [0, 1] then scale to [0.1, 1.0] meters (10cm to 100cm range)
            val normalized = if (depthRange > 0) (rawDepthData[i] - minDepth) / depthRange else 0f
            rawDepthData[i] = 0.1f + normalized * 0.9f // 0.1m to 1.0m range
        }
        
        Log.d("MiDaS", "MiDaS inference completed - raw output size: ${rawDepthData.size}, range: ${rawDepthData.minOrNull()} - ${rawDepthData.maxOrNull()}")
        
        // Resize to match input bitmap dimensions (512x512)
        val targetSize = bitmap.width * bitmap.height
        val finalDepthData = if (rawDepthData.size != targetSize) {
            Log.d("MiDaS", "Resizing depth map from ${outputWidth}x${outputHeight} to ${bitmap.width}x${bitmap.height}")
            resizeDepthMapInternal(rawDepthData, outputWidth, outputHeight, bitmap.width, bitmap.height)
        } else {
            rawDepthData
        }
        
        Log.d("MiDaS", "Final depth data size: ${finalDepthData.size} for ${bitmap.width}x${bitmap.height} image")
        
        return finalDepthData
        
    } catch (e: Exception) {
        Log.e("MiDaS", "Error in MiDaS processing", e)
        // Return fallback depth data
        val fallbackSize = 640 * 480
        return FloatArray(fallbackSize) { 0.15f } // 15cm default
    }
}

private fun convertYUV420ToBitmap(image: Image): Bitmap? {
    return try {
        Log.d("MiDaS", "Converting YUV420 image: ${image.width}x${image.height}, planes: ${image.planes.size}")
        
        val planes = image.planes
        if (planes.size < 3) {
            Log.e("MiDaS", "Image has insufficient planes: ${planes.size}")
            return null
        }
        
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        
        Log.d("MiDaS", "Buffer sizes - Y: $ySize, U: $uSize, V: $vSize")

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copy Y plane
        yBuffer.get(nv21, 0, ySize)
        
        // For NV21 format, we need V then U
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        val success = yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 90, out)
        
        if (!success) {
            Log.e("MiDaS", "Failed to compress YUV to JPEG")
            return null
        }
        
        val imageBytes = out.toByteArray()
        Log.d("MiDaS", "JPEG compressed to ${imageBytes.size} bytes")
        
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        if (bitmap == null) {
            Log.e("MiDaS", "Failed to decode bitmap from image bytes")
            return null
        }
        
        Log.d("MiDaS", "Bitmap decoded: ${bitmap.width}x${bitmap.height}")
        
        // Don't rotate for MiDaS - it handles orientation internally
        Log.d("MiDaS", "Final bitmap: ${bitmap.width}x${bitmap.height}")
        bitmap
    } catch (e: Exception) {
        Log.e("MiDaS", "Error converting YUV420 to bitmap", e)
        null
    }
}

private fun convertDepthImageToFloatArray(depthImage: Image): FloatArray {
    return try {
        val buffer = depthImage.planes[0].buffer
        val pixelCount = depthImage.width * depthImage.height
        val depthData = FloatArray(pixelCount)
        
        when (depthImage.format) {
            ImageFormat.DEPTH16 -> {
                // 16-bit depth format - values in millimeters
                val shortBuffer = buffer.asShortBuffer()
                for (i in 0 until minOf(pixelCount, shortBuffer.remaining())) {
                    val depthValueMm = shortBuffer.get(i).toInt() and 0xFFFF
                    // Convert millimeters to meters, handle invalid values (0 = no depth data)
                    depthData[i] = if (depthValueMm == 0) {
                        Float.POSITIVE_INFINITY // Invalid/no depth
                    } else {
                        depthValueMm / 1000.0f // Convert mm to meters
                    }
                }
                // Fill remaining pixels if buffer is smaller
                for (i in shortBuffer.remaining() until pixelCount) {
                    depthData[i] = Float.POSITIVE_INFINITY
                }
            }
            else -> {
                Log.w("MiDaS", "Unsupported depth image format: ${depthImage.format}")
                // Fill with estimated depth
                for (i in 0 until pixelCount) {
                    depthData[i] = 0.15f // 15cm default
                }
            }
        }
        
        val validDepths = depthData.filter { it.isFinite() }
        Log.d("MiDaS", "Converted depth data: ${depthData.size} pixels, valid: ${validDepths.size}, range: ${validDepths.minOrNull()} - ${validDepths.maxOrNull()} meters")
        depthData
    } catch (e: Exception) {
        Log.e("MiDaS", "Error converting depth image to float array", e)
        // Return estimated depth data if conversion fails
        FloatArray(depthImage.width * depthImage.height) { 0.15f } // 15cm default
    }
}

private fun createDefaultPose(): SimplePose {
    // Create a default pose representing a camera looking down at food from ~15cm
    val translation = floatArrayOf(0f, 0f, -0.15f) // 15cm away
    val rotation = floatArrayOf(0f, 0f, 0f, 1f) // No rotation (identity quaternion)
    return SimplePose(translation, rotation)
}

private fun createEstimatedDepthData(width: Int, height: Int, cameraPose: SimplePose?): FloatArray {
    // Create estimated depth data based on close-up food photography distance
    // Close-up food photos are taken from 10-25cm distance
    val centerX = width / 2
    val centerY = height / 2
    val estimatedDistance = 0.15f // 15cm typical distance
    
    return FloatArray(width * height) { index ->
        val x = index % width
        val y = index / width
        
        // Create slight depth variation - closer in center, farther at edges
        val distanceFromCenter = kotlin.math.sqrt(
            ((x - centerX).toFloat() / centerX).let { it * it } +
            ((y - centerY).toFloat() / centerY).let { it * it }
        )
        
        // Vary depth from 10cm in center to 20cm at edges
        estimatedDistance + (distanceFromCenter * 0.05f)
    }
}

 