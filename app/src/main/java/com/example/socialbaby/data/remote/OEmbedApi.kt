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

    @GET
    suspend fun getTikTokOEmbed(
        @Url url: String = "https://www.tiktok.com/oembed",
        @Query("url") videoUrl: String
    ): OEmbedResponse

    @GET
    suspend fun getFacebookOEmbed(
        @Url url: String = "https://graph.facebook.com/v18.0/oembed_video",
        @Query("url") videoUrl: String
    ): OEmbedResponse
}
