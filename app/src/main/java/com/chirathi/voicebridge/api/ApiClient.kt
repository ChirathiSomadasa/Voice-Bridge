package com.chirathi.voicebridge.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * API Client Singleton
 * Provides Retrofit instance for Voice Bridge AI API
 */
object ApiClient {
    
    /**
     * Base URL Configuration:
     * - For Android Emulator: http://10.0.2.2:5002
     * - For Physical Device: http://<YOUR_MAC_IP>:5002 (e.g., http://192.168.1.137:5002)
     * 
     * To find your Mac IP: Open Terminal and run: ipconfig getifaddr en0
     */
//    private const val BASE_URL = "http://10.0.2.2:5002/"
//    private const val BASE_URL = "https://thamindud-voiceb-flask-api.hf.space"
    private const val BASE_URL = "https://thamindud-voiceb-flask-api-2.hf.space"
//    private const val BASE_URL = "http://192.168.8.113:5002/"
//    private const val BASE_URL = "http://172.28.120.163:5002/"   phone hotspot
    
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
    
    val api: VoiceBridgeApi = retrofit.create(VoiceBridgeApi::class.java)
}
