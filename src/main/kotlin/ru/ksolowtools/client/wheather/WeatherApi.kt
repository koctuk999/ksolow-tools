package ru.ksolowtools.client.wheather

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


data class WeatherResponse(
    val data: List<WeatherData>
)

@JsonNaming(SnakeCaseStrategy::class)
data class WeatherData(
    val cityName: String,
    val appTemp: Double,
    val temp: Double,
    val clouds: Int,
    val windSpd: Double,
    val windCdirFull: String,
    val pres: Int,
    val rh: Int,
    val snow: Int,
    val precip:Int,
    val weather: Weather,
    val uv:Int,
    val aqi:Int
)

data class Weather(
    val description: String
)

interface WeatherApi {

    @GET("v2.0/current")
    fun current(
        @Query("key") key: String,
        @Query("lang") lang: String,
        @Query("lat") lat: String,
        @Query("lon") lon: String
    ): Call<WeatherResponse>
}
