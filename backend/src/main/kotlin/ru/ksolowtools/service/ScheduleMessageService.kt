package ru.ksolowtools.service

import org.springframework.stereotype.Service
import ru.ksolowtools.client.ai.AIClient
import ru.ksolowtools.client.ai.AIRequestOptions
import ru.ksolowtools.client.cat.CatClient
import ru.ksolowtools.service.style.PromptService
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val DEFAULT_ZONE_ID = "Europe/Moscow"

@Service
class ScheduleMessageService(
    private val aiClient: AIClient,
    private val dayService: DayService,
    private val workDayService: WorkDayService,
    private val catClient: CatClient,
    private val promptService: PromptService
) {

    fun morningMessageStyled(request: StyledMorningMessageRequest): StyledMorningMessageResponse {
        val zoneId = request.zoneId.toZoneId()
        val now = ZonedDateTime.now(zoneId)
        val holidays = dayService.holidaysTodayList().holidays
        val workDayStatus = workDayService.today()
        val fallback = fallbackMorningMessage(now, holidays, workDayStatus)

        return StyledMorningMessageResponse(
            text = aiClient.complete(
                systemPrompt = promptService.buildSystemPrompt(request.style, "morning"),
                userPrompt = buildMorningPrompt(now, holidays, workDayStatus, zoneId),
                fallback = fallback,
                options = AIRequestOptions()
            )
        )
    }

    fun eveningMessageStyled(request: StyledEveningMessageRequest): StyledEveningMessageResponse {
        val tomorrowStatus = workDayService.tomorrow()
        val fallback = fallbackEveningMessage(request.messages, tomorrowStatus)

        return StyledEveningMessageResponse(
            text = aiClient.complete(
                systemPrompt = promptService.buildSystemPrompt(request.style, "evening"),
                userPrompt = buildEveningPrompt(request.messages, tomorrowStatus),
                fallback = fallback,
                options = AIRequestOptions()
            ),
            imageUrl = catClient.randomCat()
        )
    }

    private fun buildMorningPrompt(
        now: ZonedDateTime,
        holidays: List<String>,
        workDayStatus: WorkDayStatus?,
        zoneId: ZoneId
    ): String = buildString {
        appendLine("Дата/время (${zoneId.id}): ${now.format(MORNING_FORMATTER)}")
        appendLine("Приветствие: ${morningGreeting(now.hour)}")
        appendLine("Статус дня: ${workDayStatus?.status ?: "Не удалось определить"}")
        appendLine("Пожелание: Хорошего дня!")
        appendLine("Список праздников:")
        if (holidays.isEmpty()) {
            appendLine("— Сегодня праздников не найдено")
        } else {
            holidays.forEach { appendLine("— $it") }
        }
    }.trim()

    private fun buildEveningPrompt(messages: List<String>, tomorrowStatus: WorkDayStatus?): String = buildString {
        appendLine("Статус завтрашнего дня: ${tomorrowStatus?.status ?: "Не удалось определить"}")
        appendLine("Сообщения за день:")
        if (messages.isEmpty()) {
            appendLine("— Сегодня в чате не было сохраненных сообщений")
        } else {
            messages.forEachIndexed { index, message ->
                appendLine("${index + 1}. $message")
            }
        }
        appendLine()
        appendLine("Нужен готовый текст вечернего сообщения по этим данным.")
    }.trim()

    private fun fallbackMorningMessage(
        now: ZonedDateTime,
        holidays: List<String>,
        workDayStatus: WorkDayStatus?
    ): String = buildString {
        appendLine(morningGreeting(now.hour))
        appendLine("Дата/время (${now.zone.id}): ${now.format(MORNING_FORMATTER)}")
        appendLine("Статус дня: ${workDayStatus?.status ?: "Не удалось определить"}")
        appendLine("Хорошего дня!")
        appendLine("Сегодня празднуют:")
        if (holidays.isEmpty()) {
            appendLine("— Сегодня праздников не найдено")
        } else {
            holidays.forEach { appendLine("— $it") }
        }
    }.trim()

    private fun fallbackEveningMessage(messages: List<String>, tomorrowStatus: WorkDayStatus?): String = buildString {
        appendLine("Итоги дня:")
        appendLine("Статус завтрашнего дня: ${tomorrowStatus?.status ?: "Не удалось определить"}")
        if (messages.isEmpty()) {
            appendLine("— Сегодня в чате было тихо, но это тоже результат.")
        } else {
            messages.take(10).forEach { appendLine("— $it") }
        }
    }.trim()

    private fun morningGreeting(hour: Int): String = when (hour) {
        in 5..11 -> "Доброе утро!"
        in 12..17 -> "Добрый день!"
        in 18..23 -> "Добрый вечер!"
        else -> "Доброй ночи!"
    }

    private fun String?.toZoneId(): ZoneId = runCatching {
        ZoneId.of(this ?: DEFAULT_ZONE_ID)
    }.getOrDefault(ZoneId.of(DEFAULT_ZONE_ID))

    private companion object {
        val MORNING_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}

data class StyledMorningMessageRequest(
    val style: String,
    val zoneId: String? = null
)

data class StyledMorningMessageResponse(
    val text: String
)

data class StyledEveningMessageRequest(
    val style: String,
    val messages: List<String> = emptyList()
)

data class StyledEveningMessageResponse(
    val text: String,
    val imageUrl: String
)
