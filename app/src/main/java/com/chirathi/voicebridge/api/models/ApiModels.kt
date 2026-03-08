package com.chirathi.voicebridge.api.models

import com.google.gson.annotations.SerializedName


// CHATBOT API MODELS
data class ChatRequest(
    val message: String
)

data class ChatResponse(
    val response: String,
    val intent: String? = null
)


// RECOMMENDER API MODELS
data class RecommendByAgeRequest(
    val age: Int,
    val disorder: String
)

data class RecommendByTextRequest(
//    val text: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("top_n")
    val topN: Int = 5
)

data class TherapyTask(
    val activity: String,  // Main activity description from Flask
    @SerializedName("age_group")
    val ageGroup: String,
    @SerializedName("disorder_category")
    val disorder: String,
    val goal: String,  // Smart goal from Flask
    val score: Double? = null,  // Similarity score
    // Optional fields for backward compatibility
    val title: String? = null,
    val description: String? = null,
    val materials: String? = null,
    val duration: String? = null,
    val tips: String? = null,
    val similarity: Double? = null
)

data class RecommendationsResponse(
    val recommendations: List<TherapyTask>
)
