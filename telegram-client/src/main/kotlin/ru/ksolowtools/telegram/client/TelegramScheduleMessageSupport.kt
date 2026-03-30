package ru.ksolowtools.telegram.client

import ru.ksolowtools.telegram.client.api.KsolowToolsApiClient
import ru.ksolowtools.telegram.client.api.StyledEveningMessage
import ru.ksolowtools.telegram.client.repository.DayMessageRepository
import ru.ksolowtools.telegram.client.style.KsolowToolsStyleService

class TelegramScheduleMessageSupport(
    private val apiClient: KsolowToolsApiClient,
    private val styleService: KsolowToolsStyleService,
    private val dayMessageRepository: DayMessageRepository
) {

    fun morningMessage(chatId: Long): String =
        apiClient.morningMessage(resolveStyle(chatId))

    fun eveningMessage(chatId: Long): StyledEveningMessage =
        apiClient.eveningMessage(
            style = resolveStyle(chatId),
            messages = dayMessageRepository.getByChatId(chatId)
        )

    fun eveningMessage(style: String, messages: List<String>): StyledEveningMessage =
        apiClient.eveningMessage(style, messages)

    fun messagesForToday(chatId: Long): List<String> = dayMessageRepository.getByChatId(chatId)

    private fun resolveStyle(chatId: Long): String = requireNotNull(styleService.resolveStyleName(chatId)) {
        "Не удалось определить стиль для чата $chatId"
    }
}
