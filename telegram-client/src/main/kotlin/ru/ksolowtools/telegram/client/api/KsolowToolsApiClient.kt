package ru.ksolowtools.telegram.client.api

import org.slf4j.LoggerFactory
import ru.ksolowtools.telegram.client.KsolowToolsTelegramClientConfig
import ru.ksolowtools.telegram.client.http.RetrofitSupport

class KsolowToolsApiClient(
    private val config: KsolowToolsTelegramClientConfig
) {
    private val log = LoggerFactory.getLogger(KsolowToolsApiClient::class.java)
    private val api = RetrofitSupport.create(config.serviceUrl).create(KsolowToolsApi::class.java)

    fun holidaysToday(style: String? = null): String = runCatching {
        if (style.isNullOrBlank()) {
            val response = api.holidaysToday().execute().body().requireBody("holidaysToday")
            formatHolidays(response)
        } else {
            api.holidaysTodayStyled(style).execute().body().requireBody("holidaysTodayStyled").text
        }
    }
        .onFailure { log.warn("Не удалось получить праздники из сервиса {}", config.serviceUrl, it) }
        .getOrElse { "Не удалось получить список праздников." }

    fun styles(): List<String> = runCatching {
        api.styles().execute().body().requireBody("styles").sorted()
    }
        .onFailure { log.warn("Не удалось получить список стилей из сервиса {}", config.serviceUrl, it) }
        .getOrElse { emptyList() }

    fun randomCat(): String = runCatching {
        api.randomCat().execute().body().requireBody("randomCat").url
    }
        .onFailure { log.warn("Не удалось получить кота из сервиса {}", config.serviceUrl, it) }
        .getOrElse { "https://cdn2.thecatapi.com/images/MTY3ODIyMQ.jpg" }

    fun currentWeather(location: String, style: String? = null): String = runCatching {
        if (style.isNullOrBlank()) {
            api.currentWeather(location).execute().body().requireBody("currentWeather").text
        } else {
            api.currentWeatherStyled(style, location).execute().body().requireBody("currentWeatherStyled").text
        }
    }
        .onFailure { log.warn("Не удалось получить погоду из сервиса {}", config.serviceUrl, it) }
        .getOrElse { "Не удалось получить прогноз погоды." }

    fun daySummary(style: String, messages: List<String>): String = runCatching {
        api.daySummaryStyled(
            StyledDaySummaryRequest(
                style = style,
                messages = messages
            )
        ).execute().body().requireBody("daySummaryStyled").text
    }
        .onFailure { log.warn("Не удалось получить саммари дня из сервиса {}", config.serviceUrl, it) }
        .getOrElse { fallbackDaySummary(messages) }

    fun morningMessage(style: String): String = runCatching {
        api.morningMessageStyled(
            StyledMorningMessageRequest(
                style = style,
                zoneId = config.dayZoneId
            )
        ).execute().body().requireBody("morningMessageStyled").text
    }
        .onFailure { log.warn("Не удалось получить утреннее сообщение из сервиса {}", config.serviceUrl, it) }
        .getOrElse { "Не удалось получить утреннее сообщение." }

    fun eveningMessage(style: String, messages: List<String>): StyledEveningMessage = runCatching {
        api.eveningMessageStyled(
            StyledEveningMessageRequest(
                style = style,
                messages = messages
            )
        ).execute().body().requireBody("eveningMessageStyled").toModel()
    }
        .onFailure { log.warn("Не удалось получить вечернее сообщение из сервиса {}", config.serviceUrl, it) }
        .getOrElse {
            StyledEveningMessage(
                text = fallbackDaySummary(messages),
                imageUrl = DEFAULT_CAT_URL
            )
        }

    private fun formatHolidays(response: HolidaysResponse): String = buildString {
        appendLine("Праздники на ${response.date}:")
        response.holidays.forEach { appendLine("— $it") }
    }.trim()

    fun aiDirectResponse(
        style: String,
        text: String,
        quotedText: String? = null,
        fallback: String = config.aiFallbackMessage
    ): String = runCatching {
        api.aiStyledRequest(
            StyledAiProxyRequest(
                style = style,
                text = text,
                content = quotedText
            )
        ).execute().body().requireBody("aiStyledRequest").text
    }
        .onFailure { log.warn("Не удалось получить AI-ответ из сервиса {}", config.serviceUrl, it) }
        .getOrElse { fallback }

    private fun fallbackDaySummary(messages: List<String>): String = buildString {
        appendLine("Итоги дня:")
        if (messages.isEmpty()) {
            appendLine("— Сегодня в чате было тихо, но это тоже результат.")
        } else {
            messages.take(10).forEach { appendLine("— $it") }
        }
    }.trim()

    private fun StyledEveningMessageResponse.toModel(): StyledEveningMessage = StyledEveningMessage(
        text = text,
        imageUrl = imageUrl
    )

    companion object {
        const val DEFAULT_CAT_URL: String = "https://cdn2.thecatapi.com/images/MTY3ODIyMQ.jpg"
    }
}

private fun <T> T?.requireBody(action: String): T = requireNotNull(this) {
    "Empty response body for $action"
}

data class StyledEveningMessage(
    val text: String,
    val imageUrl: String
)
