package com.example.xtreamtvapp.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Xtream Codes API. We pass the full request URL via @Url so the server
 * gets exactly e.g. https://infinity.gives/player_api.php?username=...&password=...
 */
interface XtreamApi {

    @GET
    suspend fun getServerInfo(
        @retrofit2.http.Url url: String,
        @Query("username") username: String,
        @Query("password") password: String
    ): Response<XtreamServerInfo>

    @GET
    suspend fun getLiveStreams(
        @retrofit2.http.Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams"
    ): Response<List<LiveStream>>

    @GET
    suspend fun getLiveCategories(
        @retrofit2.http.Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): Response<List<LiveCategory>>

    /** EPG for one stream. Response may be raw array or { "epg_listings": [] }. */
    @GET
    suspend fun getShortEpg(
        @retrofit2.http.Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_short_epg",
        @Query("stream_id") streamId: String,
        @Query("limit") limit: Int = 50
    ): Response<EpgResponse>
}
