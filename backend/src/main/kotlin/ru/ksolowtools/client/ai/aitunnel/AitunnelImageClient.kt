package ru.ksolowtools.client.ai.aitunnel

import org.apache.logging.log4j.LogManager.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import ru.ksolowtools.client.HttpClient

@Service
class AitunnelImageClient(
    @Value("\${aitunnel.api-key}")
    private val apiKey: String,
    @Value("\${aitunnel.image-model:gpt-image-1-mini}")
    private val imageModel: String
) : HttpClient("https://api.aitunnel.ru/v1/") {

    private val log = getLogger(AitunnelImageClient::class.java)
    private val api = retrofit.create(AitunnelApi::class.java)

    fun generate(prompt: String, fallback: String? = null): String? {
        if (apiKey.isBlank()) {
            log.warn("Aitunnel api key is empty, image generation is unavailable")
            return fallback
        }

        return AitunnelImageGenerationRequest(
            model = imageModel,
            prompt = prompt.trim()
        ).send(fallback)
    }

    private fun AitunnelImageGenerationRequest.send(fallback: String?) = kotlin.runCatching {
        api.imageGenerations(
            authorization = "Bearer $apiKey",
            request = this
        )
            .execute()
            .body()
            ?.data
            ?.firstOrNull()
            ?.url
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: fallback
    }
        .onSuccess { log.info("Генерация изображения в aitunnel выполнена успешно") }
        .onFailure { log.warn("Ошибка при генерации изображения в aitunnel: ${it.message}", it) }
        .getOrElse { fallback }
}
