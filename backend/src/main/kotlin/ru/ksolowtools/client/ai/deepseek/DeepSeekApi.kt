package ru.ksolowtools.client.ai.deepseek

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@JsonIgnoreProperties(ignoreUnknown = true)
data class DeepSeekResponse(
    val choices: List<DeepSeekChoice> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DeepSeekChoice(
    val message: DeepSeekMessage
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DeepSeekMessage(
    val role: String,
    val content: String
)

data class DeepSeekRequest(
    val messages: List<DeepSeekMessage>,
    val model: String = "deepseek-chat",
    val temperature: Double = 0.7,
    val max_tokens: Int = 1000
)

interface DeepSeekApi {
    @POST("chat/completions")
    fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Body request: DeepSeekRequest
    ): Call<DeepSeekResponse>
}
