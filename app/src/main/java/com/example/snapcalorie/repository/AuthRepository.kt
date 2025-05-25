package com.example.snapcalorie.repository

import com.example.snapcalorie.model.UserCreate
import com.example.snapcalorie.model.UserRead
import com.example.snapcalorie.network.ApiService
import com.example.snapcalorie.storage.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AuthRepository(
    private val api: ApiService,
    private val tokenStorage: TokenStorage
) {

    suspend fun register(email: String, password: String): UserRead =
        withContext(Dispatchers.IO) {
            try {
                api.register(UserCreate(email, password))
            } catch (e: Exception) {
                throw handleNetworkException(e)
            }
        }

    suspend fun login(email: String, password: String): UserRead =
        withContext(Dispatchers.IO) {
            try {
                // получаем токен
                val tokenResponse = api.login(email, password)
                tokenStorage.token = tokenResponse.accessToken
                // сразу запрашиваем и возвращаем профиль
                api.me("Bearer ${tokenResponse.accessToken}")
            } catch (e: Exception) {
                throw handleNetworkException(e)
            }
        }

    suspend fun getProfile(): UserRead =
        withContext(Dispatchers.IO) {
            try {
                val token = tokenStorage.token
                    ?: throw IllegalStateException("No token available - please login")
                api.me("Bearer $token")
            } catch (e: Exception) {
                throw handleNetworkException(e)
            }
        }

    fun logout() {
        tokenStorage.clear()
    }
    
    private fun handleNetworkException(e: Exception): Exception {
        return when (e) {
            is HttpException -> {
                when (e.code()) {
                    401 -> IllegalStateException("Authentication failed. Please check your credentials.")
                    403 -> IllegalStateException("Access denied. You don't have permission.")
                    404 -> IllegalStateException("Resource not found.")
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
