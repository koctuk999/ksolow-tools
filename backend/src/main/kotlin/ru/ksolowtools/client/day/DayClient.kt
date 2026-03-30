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
        when (action()) {
            0, 4 -> "Рабочий день"
            1 -> "Выходной день"
            2 -> "Сокращенный рабочий день"
            else -> null
        }

    }
        .onSuccess { log.info("Статус дня получен:$it") }
        .onFailure { log.warn("Произошла ошибка при получении статуса:${it.message}", it) }
        .getOrNull()

    fun today() = getDay {
        api
            .today()
            .execute()
            .body()
    }

    fun tomorrow() = getDay {
        api
            .tomorrow()
            .execute()
            .body()
    }

}


