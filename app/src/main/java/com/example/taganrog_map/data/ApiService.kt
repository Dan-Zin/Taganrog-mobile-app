package com.example.taganrog_map.data

import com.example.taganrog_map.data.Config.API_BASE_URL
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {
    @GET("initiatives")
    suspend fun getInitiatives(
        @Query("status") status: String? = null,
        @Query("category") category: String? = null
    ): List<InitiativeResponse>

    @GET("initiatives/{id}")
    suspend fun getInitiative(@Path("id") id: String): InitiativeResponse

    @Multipart
    @POST("initiatives")
    suspend fun createInitiative(
        @Part("initiative") initiative: RequestBody,
        @Part files: List<MultipartBody.Part>
    ): InitiativeResponse

    @PUT("initiatives/{id}")
    suspend fun updateInitiative(
        @Path("id") id: String,
        @Body initiative: InitiativeCreateRequest
    ): InitiativeResponse

    @DELETE("initiatives/{id}")
    suspend fun deleteInitiative(@Path("id") id: String): Map<String, String>

    @GET("initiatives/geojson")
    suspend fun getInitiativesGeoJson(
        @Query("status") status: String? = null
    ): Map<String, Any>

}

object ApiClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = run {
        val normalizedBaseUrl = if (API_BASE_URL.endsWith("/")) API_BASE_URL else "${API_BASE_URL}/"
        Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: ApiService = retrofit.create(ApiService::class.java)
}
