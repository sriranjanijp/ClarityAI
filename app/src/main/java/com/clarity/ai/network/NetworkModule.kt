// app/src/main/java/com/clarity/ai/network/NetworkModule.kt
package com.clarity.ai.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    // CHANGE THIS TO YOUR COMPUTER'S IP ADDRESS
    // For Android Studio Emulator:
    private const val BASE_URL = "http://10.0.2.2:8000/"

    // For physical device (replace XXX with your actual IP):
    // private const val BASE_URL = "http://192.168.1.XXX:8000/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ClarityApiService = retrofit.create(ClarityApiService::class.java)
}