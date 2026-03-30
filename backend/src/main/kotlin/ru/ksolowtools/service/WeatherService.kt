package ru.ksolowtools.service

import com.github.benmanes.caffeine.cache.Caffeine.newBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.ksolowtools.client.ai.AIClient
import ru.ksolowtools.client.ai.AIRequestOptions
import ru.ksolowtools.client.wheather.WeatherClient
import ru.ksolowtools.client.wheather.WeatherData
import ru.ksolowtools.client.wheather.WeatherLocation
import ru.ksolowtools.service.style.PromptService
import java.time.Duration.ofHours
import java.time.ZonedDateTime.now
import java.time.format.DateTimeFormatter.ofPattern

@Service
class WeatherService(
    private val aiClient: AIClient,
    private val weatherClient: WeatherClient,
    private val promptService: PromptService
) {

    private val log = LoggerFactory.getLogger(WeatherService::class.java)

    private val weatherCache = newBuilder()
        .expireAfterWrite(ofHours(1))
        .build<WeatherLocation, WeatherData?> { location ->
            runCatching {
                weatherClient.currentWeather(location)
            }
                .onSuccess { log.info("Погода для {} успешно получена", location.title) }
                .onFailure { log.error("Не получилось получить погоду для {}", location.title, it) }
                .getOrNull()
        }

    private val styledWeatherCache = newBuilder()
        .expireAfterWrite(ofHours(1))
        .build<StyledWeatherCacheKey, StyledWeatherForecast> { key ->
            generateStyledForecast(
                style = key.style,
                location = key.location
            )
        }

    fun currentStyled(style: String, location: WeatherLocation) =
        styledWeatherCache[StyledWeatherCacheKey(style, location)]


    fun current(location: WeatherLocation) = WeatherForecast(
        text = prettyForecastText(weatherCache[location])
    )

    private fun generateStyledForecast(style: String, location: WeatherLocation): StyledWeatherForecast {
        val localTime = now(location.zoneId).format(localTimeFormatter)
        val weather = weatherCache[location]

        val fallback = prettyForecastText(weather)
        val userPrompt = buildUserPrompt(weather, localTime)

        return StyledWeatherForecast(
            location = location.code,
            city = weather.cityName,
            localTime = localTime,
            style = style,
            text = aiClient.complete(
                systemPrompt = promptService.buildSystemPrompt(style, "weather"),
                userPrompt = userPrompt,
                fallback = fallback,
                options = AIRequestOptions()
            )
        )
    }

    private fun buildUserPrompt(weather: WeatherData, localTime: String): String = buildString {
        appendLine("city_name: ${weather.cityName}")
        appendLine("local_time: $localTime")
        appendLine("app_temp: ${weather.appTemp.format(1)}")
        appendLine("temp: ${weather.temp.format(1)}")
        appendLine("clouds: ${weather.clouds}")
        appendLine("wind_spd: ${weather.windSpd.format(1)}")
        appendLine("wind_cdir_full: ${weather.windCdirFull}")
        appendLine("pres: ${weather.pres.format(1)}")
        appendLine("rh: ${weather.rh}")
        appendLine("snow: ${weather.snow.format(1)}")
        appendLine("precip: ${weather.precip.format(1)}")
        appendLine("weather.description: ${weather.weather.description}")
        appendLine("aqi: ${weather.aqi}")
        appendLine("uv: ${weather.uv.format(1)}")
    }.trim()

    private fun prettyForecastText(weather: WeatherData) = buildString {
        appendLine("Приветствую, ${weather.cityName}.")
        appendLine(
            "Сейчас ${weather.weather.description.lowercase()}, температура ${weather.temp.format(1)}°C, ощущается как ${
                weather.appTemp.format(
                    1
                )
            }°C."
        )
        appendLine(
            "Облачность ${weather.clouds}%, влажность ${weather.rh}%, ветер ${weather.windCdirFull} ${
                weather.windSpd.format(
                    1
                )
            } м/с."
        )
        append(
            "Давление ${weather.pres.format(1)} mb, осадки ${weather.precip.format(1)} мм/ч, УФ-индекс ${
                weather.uv.format(
                    1
                )
            }, качество воздуха ${weather.aqi}."
        )
    }.trim()

    private fun Double.format(scale: Int) = "%.${scale}f".format(this)

    companion object {
        private val localTimeFormatter = ofPattern("yyyy-MM-dd HH:mm")
    }
}

data class StyledWeatherForecast(
    val location: String,
    val city: String,
    val localTime: String,
    val style: String,
    val text: String
)

data class WeatherForecast(
    val text: String
)

private data class StyledWeatherCacheKey(
    val style: String,
    val location: WeatherLocation
)
