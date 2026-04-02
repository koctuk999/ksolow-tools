package ru.ksolowtools.client.suno

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

data class SunoEnvelope<T>(
    val code: Int? = null,
    val msg: String? = null,
    val data: T? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SunoGenerateRequest(
    val prompt: String,
    val customMode: Boolean,
    val instrumental: Boolean,
    val model: String,
    val callBackUrl: String,
    val style: String? = null,
    val title: String? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SunoTaskData(
    @JsonAlias("taskId", "task_id", "id", "requestId", "request_id")
    val taskId: String? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SunoGenerationDetails(
    val taskId: String? = null,
    val status: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val response: SunoGenerationResponse? = null
)

data class SunoGenerationResponse(
    @JsonAlias("data")
    val data: List<SunoTrack>? = null,
    @JsonAlias("sunoData")
    val sunoData: List<SunoTrack>? = null
) {
    fun tracks(): List<SunoTrack> = data.orEmpty().ifEmpty { sunoData.orEmpty() }
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SunoTrack(
    val id: String? = null,
    @JsonAlias("audioUrl", "audio_url")
    val audioUrl: String? = null,
    @JsonAlias("imageUrl", "image_url")
    val imageUrl: String? = null,
    val title: String? = null,
    val duration: Double? = null
)

interface SunoApi {

    @POST("api/v1/generate")
    fun generate(
        @Header("Authorization") authorization: String,
        @Body request: SunoGenerateRequest
    ): Call<SunoEnvelope<SunoTaskData>>

    @GET("api/v1/generate/record-info")
    fun generationDetails(
        @Header("Authorization") authorization: String,
        @Query("taskId") taskId: String
    ): Call<SunoEnvelope<SunoGenerationDetails>>
}
