package ru.ksolowtools.client.ai.aitunnel

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@JsonIgnoreProperties(ignoreUnknown = true)
data class AitunnelResponse(
    val choices: List<AitunnelChoice> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AitunnelChoice(
    val message: AitunnelMessage
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AitunnelMessage(
    val role: String,
    val content: String
)

data class AitunnelRequest(
    val messages: List<AitunnelMessage>,
    val model: String,
    val temperature: Double = 0.7,
    val max_tokens: Int = 1000
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AitunnelImageGenerationResponse(
    val data: List<AitunnelGeneratedImage> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AitunnelGeneratedImage(
    val url: String? = null,
    val b64_json: String? = null
)

data class AitunnelImageGenerationRequest(
    val model: String,
    val prompt: String,
    val n: Int = 1,
    val size: String = "1024x1024",
    val quality: String = "low"
)

interface AitunnelApi {
    @POST("chat/completions")
    fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Body request: AitunnelRequest
    ): Call<AitunnelResponse>

    @POST("images/generations")
    fun imageGenerations(
        @Header("Authorization") authorization: String,
        @Body request: AitunnelImageGenerationRequest
    ): Call<AitunnelImageGenerationResponse>
}
