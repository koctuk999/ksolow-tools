package ru.ksolowtools.service

import com.github.benmanes.caffeine.cache.Caffeine.newBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.ksolowtools.client.ai.AIClient
import ru.ksolowtools.client.ai.AIRequestOptions
import ru.ksolowtools.client.day.DayClient
import ru.ksolowtools.service.TargetDay.TODAY
import ru.ksolowtools.service.TargetDay.TOMORROW
import ru.ksolowtools.service.style.PromptService
import java.time.ZoneId
import java.time.ZoneId.of
import java.time.ZonedDateTime.now
import java.time.format.DateTimeFormatter.ofPattern

@Service
class DayService(
    private val aiClient: AIClient,
    private val dayClient: DayClient,
    private val promptService: PromptService
) {

    private val log = LoggerFactory.getLogger(DayService::class.java)
    private val holidaysCache = newBuilder()
        .build<TargetDay, List<String>> { day ->
            runCatching {
                getHolidays(day)
            }
                .onSuccess { log.info("Список праздников на ${day.value} успешно получен") }
                .onFailure { log.error("Не получилось спарсить праздники", it) }
                .getOrNull()
        }
    private val styledHolidaysCache = newBuilder()
        .build<String, String> { style ->
            runCatching {
                generateStyledHolidays(style)
            }
                .onSuccess { log.info("Стилизованные праздники для стиля {} успешно сгенерированы", style) }
                .onFailure { log.error("Не получилось сгенерировать стилизованные праздники для стиля {}", style, it) }
                .getOrElse { fallbackHolidaysText(holidaysCache[TODAY] ?: emptyList()) }
        }

    fun todayDate(zoneId: ZoneId = of("Europe/Moscow")) = now(zoneId)
        .format(ofPattern("dd.MM.yyyy"))

    fun tomorrowDate(zoneId: ZoneId = of("Europe/Moscow")) = now(zoneId)
        .plusDays(1)
        .format(ofPattern("dd.MM.yyyy"))

    fun holidaysTodayList() = Holidays(
        date = todayDate(),
        holidays = holidaysCache[TODAY] ?: emptyList()
    )

    fun holidaysTomorrowList() = Holidays(
        date = tomorrowDate(),
        holidays = holidaysCache[TOMORROW] ?: emptyList()
    )

    fun holidaysTodayStyled(style: String) = StyledHolidays(
        date = todayDate(),
        style = style,
        text = styledHolidaysCache[style] ?: fallbackHolidaysText(holidaysCache[TODAY] ?: emptyList())
    )

    fun clearHolidayCache() {
        holidaysCache.invalidateAll()
        log.info("Кэш праздников очищен")
    }

    fun clearStyledHolidayCache() {
        styledHolidaysCache.invalidateAll()
        log.info("Кэш стилизованных праздников очищен")
    }

    private fun generateStyledHolidays(style: String): String {
        val holidays = holidaysCache[TODAY] ?: emptyList()
        val fallback = fallbackHolidaysText(holidays)
        val userPrompt = buildString {
            appendLine("Дата (МСК): ${todayDate()}")
            appendLine("Список праздников:")
            holidays.forEach { appendLine("— $it") }
        }.trim()

        return aiClient.complete(
            systemPrompt = promptService.buildSystemPrompt(style, "holidays"),
            userPrompt = userPrompt,
            fallback = fallback,
            options = AIRequestOptions()
        )
    }

    private fun fallbackHolidaysText(holidays: List<String>): String = buildString {
        appendLine("Сегодня празднуют:")
        holidays.forEach { appendLine("— $it") }
    }.trim()
}

data class Holidays(
    val date: String,
    val holidays: List<String>
)

data class StyledHolidays(
    val date: String,
    val style: String,
    val text: String
)

enum class TargetDay(val value: String) {
    TODAY("Сегодня"),
    TOMORROW("Завтра")
}
