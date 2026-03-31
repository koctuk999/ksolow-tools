package ru.ksolowtools.telegram.client

import com.github.kotlintelegrambot.dispatcher.handlers.MessageHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.ksolowtools.telegram.client.api.KsolowToolsApiClient
import ru.ksolowtools.telegram.client.repository.DayMessageRepository
import ru.ksolowtools.telegram.client.style.KsolowToolsStyleService

private val log = LoggerFactory.getLogger("KsolowToolsTelegramTextHandlers")

fun MessageHandlerEnvironment.forAllowedChats(
    config: KsolowToolsTelegramClientConfig = KsolowToolsTelegram.config,
    action: MessageHandlerEnvironment.() -> Unit
) {
    if (config.allowedIds.isEmpty() || message.chat.id in config.allowedIds) {
        action()
        return
    }

    bot.sendMessage(
        chatId = ChatId.fromId(message.chat.id),
        text = config.notAllowedMessage
    ).also {
        it.logTelegramResult("Запрещенный чат", log)
    }
}

fun MessageHandlerEnvironment.cacheMessageForDay(
    dayMessageRepository: DayMessageRepository = KsolowToolsTelegram.dayMessageRepository
) {
    if (message.from?.isBot == true) {
        return
    }

    val text = message.textOrCaption() ?: return
    val normalized = text.trim()
    if (normalized.length <= 2 || normalized.startsWith("/")) {
        return
    }

    dayMessageRepository.add(
        chatId = message.chat.id,
        message = normalized
    )
}

fun MessageHandlerEnvironment.handleDirectAddress(
    botUsername: String,
    apiClient: KsolowToolsApiClient = KsolowToolsTelegram.apiClient,
    styleService: KsolowToolsStyleService = KsolowToolsTelegram.styleService,
    config: KsolowToolsTelegramClientConfig = KsolowToolsTelegram.config,
    parseMode: ParseMode? = null,
    action: String = "Ответ на обращение",
    log: Logger = ru.ksolowtools.telegram.client.log
): Boolean {
    val content = message.textOrCaption() ?: return false
    if (message.from?.isBot == true) return false
    if (content.startsWith("/")) return false

    val chatId = message.chat.id
    val isPrivateChat = message.chat.type == "private"
    val normalizedBotUsername = botUsername.trim().removePrefix("@").lowercase()
    val mentionToken = normalizedBotUsername.takeIf { it.isNotBlank() }?.let { "@$it" }
    val lowerText = content.lowercase()
    val isMention = mentionToken?.let { it in lowerText } ?: false
    val repliedMessage = message.replyToMessage
    val isReplyToBot = repliedMessage?.from?.isBot == true &&
        (normalizedBotUsername.isBlank() ||
            repliedMessage.from?.username?.equals(normalizedBotUsername, ignoreCase = true) == true)

    if (!isPrivateChat && !isMention && !isReplyToBot) {
        return false
    }

    val userText = if (isMention && mentionToken != null) {
        content.replace(mentionToken, "", ignoreCase = true).trim()
    } else {
        content
    }
    if (userText.isBlank()) return true

    val style = styleService.resolveStyleName(chatId) ?: return false
    val response = apiClient.aiDirectResponse(
        style = style,
        text = userText,
        quotedText = repliedMessage?.textOrCaption(),
        fallback = config.aiFallbackMessage
    )

    bot.sendMessageWithChunking(
        chatId = ChatId.fromId(chatId),
        text = response,
        action = action,
        log = log,
        parseMode = parseMode
    )
    return true
}

private fun MessageHandlerEnvironment.textOrCaption(): String? = message.textOrCaption()
