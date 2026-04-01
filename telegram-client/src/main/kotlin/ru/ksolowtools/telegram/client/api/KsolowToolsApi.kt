package ru.ksolowtools.telegram.client.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

internal interface KsolowToolsApi {
    @GET("tools/styles")
    fun styles(): Call<List<String>>

    @GET("tools/cat")
    fun randomCat(): Call<CatResponse>

    @GET("day/holidays/today")
    fun holidaysToday(): Call<HolidaysResponse>

    @GET("day/today")
    fun today(): Call<DayStatusResponse>

    @GET("day/tomorrow")
    fun tomorrow(): Call<DayStatusResponse>

    @GET("day/holidays/today/styled")
    fun holidaysTodayStyled(
        @Query("style") style: String
    ): Call<StyledHolidaysResponse>

    @GET("weather/current")
    fun currentWeather(
        @Query("location") location: String
    ): Call<WeatherResponse>

    @GET("weather/current/styled")
    fun currentWeatherStyled(
        @Query("style") style: String,
        @Query("location") location: String
    ): Call<StyledWeatherResponse>

    @POST("day/summary/styled")
    fun daySummaryStyled(
        @Body request: StyledDaySummaryRequest
    ): Call<StyledDaySummaryResponse>

    @POST("day/morning-message/styled")
    fun morningMessageStyled(
        @Body request: StyledMorningMessageRequest
    ): Call<StyledMorningMessageResponse>

    @POST("day/evening-message/styled")
    fun eveningMessageStyled(
        @Body request: StyledEveningMessageRequest
    ): Call<StyledEveningMessageResponse>

    @POST("song/text/styled")
    fun songTextStyled(
        @Body request: StyledSongTextRequest
    ): Call<StyledSongTextResponse>

    @POST("song/track/styled")
    fun songTrackStyled(
        @Body request: StyledSongTrackRequest
    ): Call<StyledSongTrackResponse>

    @POST("ai/proxy/request/styled")
    fun aiStyledRequest(
        @Body request: StyledAiProxyRequest
    ): Call<StyledAiProxyResponse>

    @POST("ai/proxy/explain/styled")
    fun explainStyled(
        @Body request: StyledExplainRequest
    ): Call<StyledExplainResponse>

    @POST("ai/proxy/translate/styled")
    fun translateStyled(
        @Body request: StyledTranslateRequest
    ): Call<StyledTranslateResponse>

    @POST("ai/proxy/image")
    fun generateImage(
        @Body request: ImageGenerationRequest
    ): Call<ImageGenerationResponse>
}

data class DayStatusResponse(
    val date: String,
    val status: String?
)

data class HolidaysResponse(
    val date: String,
    val holidays: List<String>
)

data class StyledHolidaysResponse(
    val date: String,
    val style: String,
    val text: String
)

data class WeatherResponse(
    val text: String
)

data class StyledWeatherResponse(
    val location: String,
    val city: String,
    val localTime: String,
    val style: String,
    val text: String
)

data class StyledDaySummaryRequest(
    val style: String,
    val messages: List<String>
)

data class StyledDaySummaryResponse(
    val style: String,
    val messagesCount: Int,
    val text: String
)

data class StyledMorningMessageRequest(
    val style: String,
    val zoneId: String? = null
)

data class StyledMorningMessageResponse(
    val text: String
)

data class StyledEveningMessageRequest(
    val style: String,
    val messages: List<String>
)

data class StyledEveningMessageResponse(
    val text: String,
    val imageUrl: String
)

data class StyledSongTextRequest(
    val style: String,
    val sourceText: String
)

data class StyledSongTextResponse(
    val style: String,
    val text: String
)

data class StyledSongTrackRequest(
    val style: String,
    val prompt: String? = null,
    val songText: String? = null
)

data class StyledSongTrackResponse(
    val style: String,
    val success: Boolean,
    val performer: String,
    val title: String? = null,
    val audioUrl: String? = null,
    val durationSeconds: Int? = null,
    val lyrics: String? = null,
    val errorMessage: String? = null
)

data class StyledAiProxyRequest(
    val style: String,
    val text: String,
    val content: String? = null
)

data class StyledAiProxyResponse(
    val style: String,
    val text: String
)

data class StyledExplainRequest(
    val style: String,
    val question: String
)

data class StyledExplainResponse(
    val style: String,
    val text: String
)

data class StyledTranslateRequest(
    val style: String,
    val text: String
)

data class StyledTranslateResponse(
    val style: String,
    val text: String
)

data class ImageGenerationRequest(
    val prompt: String
)

data class ImageGenerationResponse(
    val prompt: String,
    val imageUrl: String? = null,
    val imageBase64: String? = null
)

data class CatResponse(
    val url: String
)
