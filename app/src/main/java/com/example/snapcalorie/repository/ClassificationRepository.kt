package com.example.snapcalorie.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.snapcalorie.model.ClassificationResponse
import com.example.snapcalorie.network.ApiService
import com.example.snapcalorie.storage.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ClassificationRepository(
    private val api: ApiService,
    private val tokenStorage: TokenStorage
) {

    suspend fun classifyImage(context: Context, imageUri: Uri): ClassificationResponse =
        withContext(Dispatchers.IO) {
            try {
                val token = tokenStorage.token
                    ?: throw IllegalStateException("No token available - please login")

                // Читаем изображение из URI
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val imageBytes = inputStream?.readBytes()
                inputStream?.close()

                if (imageBytes == null) {
                    throw IllegalStateException("Failed to read image from URI")
                }

                // Создаем MultipartBody.Part для отправки файла
                val requestBody = imageBytes.toRequestBody("image/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", "image.jpg", requestBody)

                api.classifyImage("Bearer $token", filePart)
            } catch (e: Exception) {
                throw handleNetworkException(e)
            }
        }

    suspend fun classifyImageBitmap(bitmap: Bitmap): ClassificationResponse =
        withContext(Dispatchers.IO) {
            try {
                val token = tokenStorage.token
                    ?: throw IllegalStateException("No token available - please login")

                // Конвертируем bitmap в байты
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val imageBytes = stream.toByteArray()
                stream.close()

                // Создаем MultipartBody.Part для отправки файла
                val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", "region.jpg", requestBody)

                api.classifyImage("Bearer $token", filePart)
            } catch (e: Exception) {
                throw handleNetworkException(e)
            }
        }

    suspend fun classifyMultipleRegions(regions: List<Bitmap>): List<ClassificationResponse> =
        withContext(Dispatchers.IO) {
            try {
                regions.map { bitmap ->
                    classifyImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                throw handleNetworkException(e)
            }
        }

    private fun handleNetworkException(e: Exception): Exception {
        return when (e) {
            is HttpException -> {
                when (e.code()) {
                    400 -> IllegalStateException("Invalid image file. Please select a valid image.")
                    401 -> IllegalStateException("Authentication failed. Please login again.")
                    403 -> IllegalStateException("Access denied. You don't have permission.")
                    404 -> IllegalStateException("Classification service not found.")
                    500, 501, 502, 503 -> IllegalStateException("Server error. Please try again later.")
                    else -> IllegalStateException("Network error: ${e.message()}")
                }
            }
            is UnknownHostException -> IllegalStateException("Cannot connect to server. Please check your internet connection.")
            is ConnectException -> IllegalStateException("Connection failed. Server might be down.")
            is SocketTimeoutException -> IllegalStateException("Connection timed out. Please try again.")
            else -> e
        }
    }
} 