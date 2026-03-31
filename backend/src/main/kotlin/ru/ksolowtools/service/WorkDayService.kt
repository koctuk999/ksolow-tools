package ru.ksolowtools.service

import com.github.benmanes.caffeine.cache.Caffeine.newBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.ksolowtools.client.day.DayClient
import ru.ksolowtools.client.day.DayStatus
import java.time.ZoneId
import java.time.ZonedDateTime.now
import java.time.format.DateTimeFormatter.ofPattern

@Service
class WorkDayService(
    private val dayClient: DayClient
) {

    private val log = LoggerFactory.getLogger(WorkDayService::class.java)
    private val statusCache = newBuilder()
        .build<TargetDay, WorkDayStatus?> { day ->
            val dayLabel = day?.value?.lowercase() ?: "неизвестный день"
            runCatching {
                when (day) {
                    TargetDay.TODAY -> buildStatus(dayClient.todayStatus(), day)
                    TargetDay.TOMORROW -> buildStatus(dayClient.tomorrowStatus(), day)
                    null -> null
                }
            }
                .onSuccess { log.info("Статус {} успешно получен", dayLabel) }
                .onFailure { log.warn("Не удалось получить статус {}", dayLabel, it) }
                .getOrNull()
        }

    fun today() = statusCache[TargetDay.TODAY]

    fun tomorrow() = statusCache[TargetDay.TOMORROW]

    fun clearCache() {
        statusCache.invalidateAll()
        log.info("Кэш статусов рабочего дня очищен")
    }

    private fun buildStatus(status: DayStatus?, targetDay: TargetDay): WorkDayStatus? =
        status?.let {
            WorkDayStatus(
                date = dateFor(targetDay),
                targetDay = targetDay,
                status = it.text,
                code = it.code
            )
        }

    private fun dateFor(targetDay: TargetDay, zoneId: ZoneId = ZoneId.of(DEFAULT_ZONE_ID)): String {
        val date = now(zoneId).let { current ->
            when (targetDay) {
                TargetDay.TODAY -> current
                TargetDay.TOMORROW -> current.plusDays(1)
            }
        }

        return date.format(DATE_FORMATTER)
    }

    private companion object {
        const val DEFAULT_ZONE_ID = "Europe/Moscow"
        val DATE_FORMATTER = ofPattern("dd.MM.yyyy")
    }
}

data class WorkDayStatus(
    val date: String,
    val targetDay: TargetDay,
    val status: String,
    val code: Int
)
