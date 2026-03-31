package ru.ksolowtools.client.day

import ru.ksolowtools.client.HttpClient
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Service
import retrofit2.converter.scalars.ScalarsConverterFactory

@Service
class DayClient : HttpClient(
    baseUrl = "https://isdayoff.ru/",
    converterFactories = listOf(ScalarsConverterFactory.create())
) {

    private val log = getLogger(DayClient::class.java)
    private val api = retrofit.create(DayApi::class.java)

    private fun getDay(action: () -> Int?) = kotlin.runCatching {
        action()
            ?.let(::toDayStatus)
    }
        .onSuccess { log.info("Статус дня получен:{}", it) }
        .onFailure { log.warn("Произошла ошибка при получении статуса:${it.message}", it) }
        .getOrNull()

    fun todayStatus() = getDay {
        api
            .today()
            .execute()
            .body()
    }

    fun tomorrowStatus() = getDay {
        api
            .tomorrow()
            .execute()
            .body()
    }

    fun today() = todayStatus()?.text

    fun tomorrow() = tomorrowStatus()?.text

    private fun toDayStatus(code: Int): DayStatus? = when (code) {
        0, 4 -> DayStatus(code = code, text = "Рабочий день")
        1 -> DayStatus(code = code, text = "Выходной день")
        2 -> DayStatus(code = code, text = "Сокращенный рабочий день")
        else -> null
    }

}

data class DayStatus(
    val code: Int,
    val text: String
)

