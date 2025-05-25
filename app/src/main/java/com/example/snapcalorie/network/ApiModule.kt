package com.example.snapcalorie.network

import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiModule {

    // Using localhost with adb reverse for USB debugging
    private const val BASE_URL = "http://127.0.0.1:8000/"
    
    // Timeout constants
    private const val CONNECT_TIMEOUT = 15L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 15L

    // Интерцептор, который подставит токен в каждый запрос
    class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request().newBuilder().apply {
                // если токен есть, добавляем заголовок
                tokenProvider()?.let { header("Authorization", "Bearer $it") }
            }.build()
            return chain.proceed(request)
        }
    }

    // Клубок OkHttpClient: логирование + наш AuthInterceptor
    fun provideOkHttpClient(tokenProvider: () -> String?): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor(AuthInterceptor(tokenProvider))
            .build()

    // Сам Retrofit
    fun provideApiService(tokenProvider: () -> String?): ApiService {
        val gson = GsonBuilder()
            .setLenient()
            .create()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(provideOkHttpClient(tokenProvider))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
