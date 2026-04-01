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

    fun generate(prompt: String, fallback: GeneratedImage? = null): GeneratedImage? {
        if (apiKey.isBlank()) {
            log.warn("Aitunnel api key is empty, image generation is unavailable")
            return fallback
        }

        return AitunnelImageGenerationRequest(
            model = imageModel,
            prompt = prompt.trim()
        ).send(fallback)
    }

    private fun AitunnelImageGenerationRequest.send(fallback: GeneratedImage?) = kotlin.runCatching {
        api.imageGenerations(
            authorization = "Bearer $apiKey",
            request = this
        )
            .execute()
            .body()
            ?.data
            ?.firstOrNull()
            ?.toModel()
            ?: fallback
    }
        .onSuccess { log.info("Генерация изображения в aitunnel выполнена успешно") }
        .onFailure { log.warn("Ошибка при генерации изображения в aitunnel: ${it.message}", it) }
        .getOrElse { fallback }

    private fun AitunnelGeneratedImage.toModel(): GeneratedImage? {
        val normalizedUrl = url?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedBase64 = b64_json?.trim()?.takeIf { it.isNotEmpty() }
        if (normalizedUrl == null && normalizedBase64 == null) {
            return null
        }
        return GeneratedImage(
            url = normalizedUrl,
            base64 = normalizedBase64
        )
    }
}

data class GeneratedImage(
    val url: String? = null,
    val base64: String? = null
)
