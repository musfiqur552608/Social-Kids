package com.example.socialbaby.data.remote

import com.example.socialbaby.data.remote.dto.OEmbedResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface OEmbedApi {

    @GET
    suspend fun getYoutubeOEmbed(
        @Url url: String = "https://www.youtube.com/oembed",
        @Query("url") videoUrl: String,
        @Query("format") format: String = "json"
    ): OEmbedResponse
}
