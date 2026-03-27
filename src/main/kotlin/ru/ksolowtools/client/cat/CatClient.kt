package ru.ksolowtools.client.cat

import ru.ksolowtools.client.HttpClient
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

private const val DEFAULT_CAT = "https://cdn2.thecatapi.com/images/MTY3ODIyMQ.jpg"

@Service
class CatClient(
    @Value("\${cat.api-key}")
    private val catApiKey: String
) : HttpClient("https://api.thecatapi.com/") {

    private val log = getLogger(CatClient::class.java)
    private val api = retrofit.create(CatApi::class.java)

    fun randomCat() = kotlin.runCatching {
        api.getRandomImage(catApiKey)
            .execute()
            .body()
            ?.firstOrNull()
            ?.url ?: DEFAULT_CAT

    }
        .onSuccess { log.info("Картинка успешно получена:$it") }
        .onFailure { log.warn("Произошла ошибка при получении картинки:${it.message}", it) }
        .getOrDefault(DEFAULT_CAT)
}


