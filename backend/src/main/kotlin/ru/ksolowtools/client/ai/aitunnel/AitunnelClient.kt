package ru.ksolowtools.client.ai.aitunnel

import org.apache.logging.log4j.LogManager.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import ru.ksolowtools.client.HttpClient
import ru.ksolowtools.client.ai.AIClient
import ru.ksolowtools.client.ai.AIRequestOptions

@Service
@ConditionalOnProperty(name = ["ai.provider"], havingValue = "aitunnel")
class AitunnelClient(
    @Value("\${aitunnel.api-key}")
    private val apiKey: String,
    @Value("\${aitunnel.model:grok-4.1-fast}")
    private val model: String
) : AIClient, HttpClient("https://api.aitunnel.ru/v1/") {

    private val log = getLogger(AitunnelClient::class.java)
    private val api = retrofit.create(AitunnelApi::class.java)

    private fun AitunnelRequest.send(fallback: String) = kotlin.runCatching {
        api.chatCompletions(
            authorization = "Bearer $apiKey",
            request = this
        )
            .execute()
            .body()
            ?.choices
            ?.firstOrNull()
            ?.message
            ?.content
            ?.trim()
            ?: fallback
    }
        .onSuccess { log.info("Запрос в aitunnel выполнен успешно") }
        .onFailure { log.warn("Ошибка при походе в aitunnel: ${it.message}", it) }
        .getOrElse { fallback }

    private fun systemMessage(text: String) = AitunnelMessage(role = "system", content = text)

    private fun userMessage(text: String) = AitunnelMessage(
        role = "user",
        content = text
    )

    override fun complete(
        systemPrompt: String,
        userPrompt: String,
        fallback: String,
        options: AIRequestOptions
    ): String = AitunnelRequest(
        messages = listOf(
            systemMessage(systemPrompt),
            userMessage(userPrompt)
        ),
        model = model,
        temperature = options.temperature,
        max_tokens = options.maxTokens
    ).send(fallback)
}
