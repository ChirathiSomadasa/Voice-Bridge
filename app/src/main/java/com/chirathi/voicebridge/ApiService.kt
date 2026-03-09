package com.chirathi.voicebridge

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class CaptionResponse(
    val caption: String
)

interface ApiService {
    @Multipart
    @POST("caption")
    fun uploadImage(
        @Part image: MultipartBody.Part,
        @Part("hint") hint: okhttp3.RequestBody
    ): Call<CaptionResponse>
}
