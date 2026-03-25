package com.chirathi.voicebridge.repository

import android.util.Log
import com.chirathi.voicebridge.api.ApiClient
import com.chirathi.voicebridge.api.models.*

/**
 * AI Repository
 * Handles all AI-related operations (Chatbot and Task Recommender)
 */
class AIRepository {

    private val api = ApiClient.api
    private val TAG = "AIRepository"

    /**
     * Send message to chatbot and get response
     * @param message User's message
     * @return ChatResponse or null if error
     */
    suspend fun sendChatMessage(message: String): ChatResponse? {
        return try {
            val request = ChatRequest(message)
            val response = api.chat(request)

            if (response.isSuccessful) {
                Log.d(TAG, "Chat successful: ${response.body()}")
                response.body()
            } else {
                Log.e(TAG, "Chat error: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Chat exception: ${e.message}", e)
            null
        }
    }

    /**
     * Get therapy task recommendations by age and disorder
     * @param age Child's age
     * @param disorder Child's disorder type
     * @return List of recommended tasks or empty list if error
     */
    suspend fun getRecommendationsByAge(age: Int, disorder: String): List<TherapyTask> {
        return try {
            val request = RecommendByAgeRequest(age, disorder)
            val response = api.recommendByAge(request)

            if (response.isSuccessful) {
                val recommendations = response.body()?.recommendations ?: emptyList()
                Log.d(TAG, "Got ${recommendations.size} recommendations for age=$age, disorder=$disorder")
                recommendations
            } else {
                Log.e(TAG, "Recommend error: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recommend exception: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get therapy task recommendations by text description
     * @param description Text description of desired activity
     * @param topN Number of recommendations to return (default: 5)
     * @return List of recommended tasks or empty list if error
     */
    suspend fun getRecommendationsByText(description: String, topN: Int = 5): List<TherapyTask> {
        return try {
            val request = RecommendByTextRequest(description, topN)
            val response = api.recommendByText(request)

            if (response.isSuccessful) {
                val recommendations = response.body()?.recommendations ?: emptyList()
                Log.d(TAG, "Got ${recommendations.size} text-based recommendations")
                recommendations
            } else {
                Log.e(TAG, "Text recommend error: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Text recommend exception: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getTherapyRecommendations(request: RecommendTherapyRequest): List<RecommendTherapyItem> {
        return try {
            val response = api.recommendTherapy(request)

            if (response.isSuccessful) {
                val activities = response.body()?.activities ?: emptyList()
                Log.d(TAG, "Got ${activities.size} therapy recommendations")
                activities
            } else {
                Log.e(TAG, "Therapy recommend error: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Therapy recommend exception: ${e.message}", e)
            emptyList()
        }
    }
}
