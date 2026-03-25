package com.chirathi.voicebridge.api

import com.chirathi.voicebridge.api.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Voice Bridge AI API Interface
 * Base URL: http://10.0.2.2:5001 (for Android Emulator)
 * For physical device: http://<YOUR_MAC_IP>:5001
 */
interface VoiceBridgeApi {
    
    /**
     * Chat with the NLP Chatbot
     * Endpoint: POST /api/chat
     */
    @POST("api/chat")
//    @POST("chat")  // becomes /+/api/chat
    suspend fun chat(@Body request: ChatRequest): Response<ChatResponse>


    /**
     * Get therapy task recommendations by age and disorder
     * Endpoint: POST /api/recommend/age
     */
    @POST("api/recommend/age")
//    @POST("recommend/age")  // becomes /+/api/recommend/age
    suspend fun recommendByAge(@Body request: RecommendByAgeRequest): Response<RecommendationsResponse>

    /**
     * Get therapy task recommendations by text description
     * Endpoint: POST /api/recommend/text
     */
    @POST("api/recommend/text")
//    @POST("recommend/text")  // becomes /+/api/recommend/text
    suspend fun recommendByText(@Body request: RecommendByTextRequest): Response<RecommendationsResponse>


    @POST("/api/recommend/therapy")
    suspend fun recommendTherapy(@Body request: RecommendTherapyRequest): Response<RecommendTherapyResponse>

}
