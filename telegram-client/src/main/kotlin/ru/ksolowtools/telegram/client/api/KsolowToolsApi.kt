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

    @POST("ai/proxy/request/styled")
    fun aiStyledRequest(
        @Body request: StyledAiProxyRequest
    ): Call<StyledAiProxyResponse>
}

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

data class StyledAiProxyRequest(
    val style: String,
    val text: String,
    val content: String? = null
)

data class StyledAiProxyResponse(
    val style: String,
    val text: String
)

data class CatResponse(
    val url: String
)
