package ru.ksolowtools.client.wheather

import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import ru.ksolowtools.client.HttpClient
import java.time.ZoneId

@Service
class WeatherClient(
    @Value("\${weather.api-key}")
    private val apiKey: String
) : HttpClient("https://api.weatherbit.io/") {

    private val log = getLogger(WeatherClient::class.java)

    private val api = retrofit.create(WeatherApi::class.java)

    fun currentWeather(location: WeatherLocation) = kotlin.runCatching {
        api.current(
            key = apiKey,
            lang = "ru",
            lat = location.lat,
            lon = location.lon
        )
            .execute()
            .body()
            ?.data
            ?.firstOrNull()

    }
        .onSuccess { log.info("Данные о погоде успешно получены:$it") }
        .onFailure { log.warn("Произошла ошибка при получении погоды:${it.message}", it) }
        .getOrNull()
}

enum class WeatherLocation(
    val code: String,
    val title: String,
    val lat: String,
    val lon: String,
    val zoneId: ZoneId
) {
    SPB(
        code = "spb",
        title = "Санкт-Петербург",
        lat = "59.9342802",
        lon = "30.3350986",
        zoneId = ZoneId.of("Europe/Moscow")
    );

    companion object {
        fun fromCode(code: String): WeatherLocation = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: throw IllegalArgumentException("Локация '$code' не найдена")
    }
}
