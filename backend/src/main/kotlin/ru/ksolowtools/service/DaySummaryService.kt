package ru.ksolowtools.service

import org.springframework.stereotype.Service
import ru.ksolowtools.client.ai.AIClient
import ru.ksolowtools.client.ai.AIRequestOptions
import ru.ksolowtools.service.style.PromptService

@Service
class DaySummaryService(
    private val aiClient: AIClient,
    private val promptService: PromptService
) {

    fun summarizeStyled(style: String, messages: List<String>): StyledDaySummary =
        StyledDaySummary(
            style = style,
            messagesCount = messages.size,
            text = aiClient.complete(
                systemPrompt = promptService.buildSystemPrompt(style, "daySummary"),
                userPrompt = buildUserPrompt(messages),
                fallback = fallbackSummary(messages),
                options = AIRequestOptions()
            )
        )

    private fun buildUserPrompt(messages: List<String>): String = buildString {
        appendLine("Сообщения за день:")
        messages.forEachIndexed { index, message ->
            appendLine("${index + 1}. $message")
        }
    }.trim()

    private fun fallbackSummary(messages: List<String>): String = buildString {
        appendLine("Итоги дня:")
        messages.take(10).forEach { appendLine("— $it") }
    }.trim()
}

data class StyledDaySummary(
    val style: String,
    val messagesCount: Int,
    val text: String
)

data class StyledDaySummaryRequest(
    val style: String,
    val messages: List<String>
)
