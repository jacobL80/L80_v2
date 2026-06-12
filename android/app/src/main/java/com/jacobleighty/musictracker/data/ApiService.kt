package com.jacobleighty.musictracker.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import com.jacobleighty.musictracker.Constants

interface ApiService {

    @GET("api/artists.php")
    suspend fun getArtists(): List<Artist>

    @POST("api/artists.php")
    suspend fun createArtist(
        @Header("X-Edit-Token") token: String,
        @Body artist: Artist,
    ): Response<Artist>

    @PUT("api/artists.php")
    suspend fun updateArtist(
        @Query("id") id: Int,
        @Header("X-Edit-Token") token: String,
        @Body artist: Artist,
    ): Response<Artist>

    @DELETE("api/artists.php")
    suspend fun deleteArtist(
        @Query("id") id: Int,
        @Header("X-Edit-Token") token: String,
    ): Response<Unit>

    @GET("api/history.php")
    suspend fun getHistory(): List<HistoryEntry>

    @POST("api/history.php")
    suspend fun createHistoryEntry(
        @Header("X-Edit-Token") token: String,
        @Body entry: Map<String, String>,
    ): Response<HistoryEntry>

    companion object {
        fun create(): ApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()
            return Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
