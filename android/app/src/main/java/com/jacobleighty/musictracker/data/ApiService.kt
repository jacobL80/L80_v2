package com.jacobleighty.musictracker.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import com.jacobleighty.musictracker.Constants

interface ApiService {

    // ── Music ─────────────────────────────────────────────────────────────────

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

    // ── All ───────────────────────────────────────────────────────────────────

    @GET("api/all.php")
    suspend fun getAllItems(): List<AllItem>

    // ── Concerts ──────────────────────────────────────────────────────────────

    @GET("api/concerts.php")
    suspend fun getConcerts(): List<Concert>

    @POST("api/concerts.php")
    suspend fun createConcert(
        @Header("X-Edit-Token") token: String,
        @Body concert: Concert,
    ): Response<Concert>

    @PUT("api/concerts.php")
    suspend fun updateConcert(
        @Query("id") id: Int,
        @Header("X-Edit-Token") token: String,
        @Body concert: Concert,
    ): Response<Concert>

    @DELETE("api/concerts.php")
    suspend fun deleteConcert(
        @Query("id") id: Int,
        @Header("X-Edit-Token") token: String,
    ): Response<Unit>

    // ── TV / Movies ───────────────────────────────────────────────────────────

    @GET("api/tvmovies.php")
    suspend fun getTvShows(): List<TvShow>

    @POST("api/tvmovies.php")
    suspend fun createTvShow(
        @Header("X-Edit-Token") token: String,
        @Body show: TvShow,
    ): Response<TvShow>

    @PUT("api/tvmovies.php")
    suspend fun updateTvShow(
        @Query("id") id: Int,
        @Header("X-Edit-Token") token: String,
        @Body show: TvShow,
    ): Response<TvShow>

    @DELETE("api/tvmovies.php")
    suspend fun deleteTvShow(
        @Query("id") id: Int,
        @Header("X-Edit-Token") token: String,
    ): Response<Unit>

    // ── Running ───────────────────────────────────────────────────────────────

    @GET("api/running.php")
    suspend fun getRunningWeeks(): List<RunningWeek>

    @GET("api/running.php")
    suspend fun getRunningWeeksByYear(@Query("year") year: Int): List<RunningWeek>

    @POST("api/running.php")
    suspend fun createRunEntry(
        @Header("X-Edit-Token") token: String,
        @Body entry: Map<String, String>,
    ): Response<RunningDayEntry>

    @PUT("api/running.php")
    suspend fun updateRunEntry(
        @Query("id") id: Int,
        @Header("X-Edit-Token") token: String,
        @Body entry: Map<String, String>,
    ): Response<RunningDayEntry>

    @DELETE("api/running.php")
    suspend fun deleteRunEntry(
        @Query("id") id: Int,
        @Header("X-Edit-Token") token: String,
    ): Response<Unit>

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
