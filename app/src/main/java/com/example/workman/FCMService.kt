package com.example.workman

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface FCMService {
    @Headers("Content-Type:application/json")
    @POST("fcm/send")
    suspend fun sendNotification(
        @Body payload: Map<String, Any>,
        @Header("Authorization") apiKey: String
    ): Response<ResponseBody>
}
