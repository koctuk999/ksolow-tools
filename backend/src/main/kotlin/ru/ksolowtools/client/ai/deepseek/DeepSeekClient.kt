package ru.ksolowtools.client.ai.deepseek

import ru.ksolowtools.client.HttpClient
import ru.ksolowtools.client.ai.AIClient
import ru.ksolowtools.client.ai.AIRequestOptions
import org.apache.logging.log4j.LogManager.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class DeepSeekClient(
    @Value("\${deepseek.api-key}")
    private val apiKey: String
) : AIClient, HttpClient("https://api.deepseek.com/v1/") {

    private val log = getLogger(DeepSeekClient::class.java)
    private val api = retrofit.create(DeepSeekApi::class.java)

    private fun DeepSeekRequest.send(fallback: String) = kotlin.runCatching {
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
        .onSuccess { log.info("Запрос в deepseek выполнен успешно") }
        .onFailure { log.warn("Ошибка при походе в deepseek: ${it.message}", it) }
        .getOrElse { fallback }

    private fun systemMessage(text: String) = DeepSeekMessage(role = "system", content = text)

    private fun userMessage(text: String) = DeepSeekMessage(
        role = "user",
        content = text
    )


    override fun complete(
        systemPrompt: String,
        userPrompt: String,
        fallback: String,
        options: AIRequestOptions
    ): String = DeepSeekRequest(
        messages = listOf(
            systemMessage(systemPrompt),
            userMessage(userPrompt)
        ),
        temperature = options.temperature,
        max_tokens = options.maxTokens
    ).send(fallback)
}
