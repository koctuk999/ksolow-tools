package ru.ksolowtools.telegram.client

import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.TelegramFile.ByUrl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.ksolowtools.telegram.client.api.KsolowToolsApiClient
import ru.ksolowtools.telegram.client.repository.DayMessageRepository
import ru.ksolowtools.telegram.client.style.KsolowToolsStyleService

private val log = LoggerFactory.getLogger("KsolowToolsTelegramCommands")

fun CommandHandlerEnvironment.forAllowedChats(
    config: KsolowToolsTelegramClientConfig = KsolowToolsTelegram.config,
    action: CommandHandlerEnvironment.() -> Unit
) {
    if (config.allowedIds.isEmpty() || message.chat.id in config.allowedIds) {
        action()
        return
    }

    bot.sendMessage(
        chatId = ChatId.fromId(message.chat.id),
        text = KsolowToolsTelegramMessages.NOT_ALLOWED
    ).also {
        it.logTelegramResult("Запрещенный чат", log)
    }
}

fun CommandHandlerEnvironment.handleHolidays(
    apiClient: KsolowToolsApiClient = KsolowToolsTelegram.apiClient,
    styleService: KsolowToolsStyleService = KsolowToolsTelegram.styleService,
    style: String? = styleService.resolveStyleName(message.chat.id),
    parseMode: ParseMode? = null,
    action: String = "Команда /holidays",
    log: Logger = ru.ksolowtools.telegram.client.log
) {
    val response = apiClient.holidaysToday(style)
    bot.sendMessageWithChunking(
        chatId = ChatId.fromId(message.chat.id),
        text = response,
        action = action,
        log = log,
        parseMode = parseMode
    )
}

fun CommandHandlerEnvironment.handleWeather(
    apiClient: KsolowToolsApiClient = KsolowToolsTelegram.apiClient,
    config: KsolowToolsTelegramClientConfig = KsolowToolsTelegram.config,
    styleService: KsolowToolsStyleService = KsolowToolsTelegram.styleService,
    style: String? = styleService.resolveStyleName(message.chat.id),
    parseMode: ParseMode? = null,
    action: String = "Команда /weather",
    log: Logger = ru.ksolowtools.telegram.client.log
) {
    val location = resolveWeatherLocationCode(rawCommandText(), config.weatherLocationAliases)
    val response = if (location == null) {
        KsolowToolsTelegramMessages.WEATHER_UNKNOWN_CITY
    } else {
        apiClient.currentWeather(location = location, style = style)
    }

    bot.sendMessageWithChunking(
        chatId = ChatId.fromId(message.chat.id),
        text = response,
        action = action,
        log = log,
        parseMode = parseMode
    )
}

fun CommandHandlerEnvironment.handleDay(
    apiClient: KsolowToolsApiClient = KsolowToolsTelegram.apiClient,
    dayMessageRepository: DayMessageRepository = KsolowToolsTelegram.dayMessageRepository,
    styleService: KsolowToolsStyleService = KsolowToolsTelegram.styleService,
    parseMode: ParseMode? = null,
    action: String = "Команда /day",
    log: Logger = ru.ksolowtools.telegram.client.log
) {
    val messages = dayMessageRepository.getByChatId(message.chat.id)
    val response = if (messages.isEmpty()) {
        KsolowToolsTelegramMessages.DAY_NO_MESSAGES
    } else {
        val style = styleService.resolveStyleName(message.chat.id)
        if (style.isNullOrBlank()) {
            KsolowToolsTelegramMessages.DAY_NO_MESSAGES
        } else {
            apiClient.daySummary(
                style = style,
                messages = messages
            )
        }
    }

    bot.sendMessageWithChunking(
        chatId = ChatId.fromId(message.chat.id),
        text = response,
        action = action,
        log = log,
        parseMode = parseMode
    )
}

fun CommandHandlerEnvironment.handleStyle(
    styleService: KsolowToolsStyleService = KsolowToolsTelegram.styleService,
    action: String = "Команда /style",
    log: Logger = ru.ksolowtools.telegram.client.log
) {
    val chatId = message.chat.id
    val styleArg = rawCommandText()
        ?.substringAfter(' ', "")
        ?.trim()
        .orEmpty()

    val availableStyles = styleService.availableStyles()
    val availableStylesText = availableStyles.joinToString(", ")
    val currentStyle = styleService.resolveStyleName(chatId).orEmpty()

    val response = if (styleArg.isBlank()) {
        styleService.format(
            template = KsolowToolsTelegramMessages.STYLE_LIST_TEMPLATE,
            variables = mapOf(
                "style" to currentStyle,
                "styles" to availableStylesText
            )
        )
    } else {
        val resolvedStyle = styleService.resolveRequestedStyle(styleArg)
        if (resolvedStyle == null) {
            styleService.format(
                template = KsolowToolsTelegramMessages.STYLE_UNKNOWN_TEMPLATE,
                variables = mapOf(
                    "style" to styleArg,
                    "styles" to availableStylesText
                )
            )
        } else {
            styleService.saveStyleForChat(chatId, chatDisplayName(), resolvedStyle)
            styleService.format(
                template = KsolowToolsTelegramMessages.STYLE_SET_SUCCESS_TEMPLATE,
                variables = mapOf("style" to resolvedStyle)
            )
        }
    }

    bot.sendMessageWithChunking(
        chatId = ChatId.fromId(chatId),
        text = response,
        action = action,
        log = log
    )
}

fun CommandHandlerEnvironment.handleExplain(
    apiClient: KsolowToolsApiClient = KsolowToolsTelegram.apiClient,
    styleService: KsolowToolsStyleService = KsolowToolsTelegram.styleService,
    parseMode: ParseMode? = null,
    action: String = "Команда /explain",
    log: Logger = ru.ksolowtools.telegram.client.log
) {
    val chatId = message.chat.id
    val question = rawCommandText()
        ?.substringAfter(' ', "")
        ?.trim()
        .orEmpty()

    val response = if (question.isBlank()) {
        KsolowToolsTelegramMessages.EXPLAIN_NEED_QUESTION
    } else {
        val style = styleService.requireStyle(chatId)
        apiClient.explain(
            style = style,
            question = question,
            fallback = KsolowToolsTelegramMessages.AI_FALLBACK
        )
    }

    bot.sendMessageWithChunking(
        chatId = ChatId.fromId(chatId),
        text = response,
        action = action,
        log = log,
        parseMode = parseMode
    )
}

fun CommandHandlerEnvironment.handleTranslate(
    apiClient: KsolowToolsApiClient = KsolowToolsTelegram.apiClient,
    styleService: KsolowToolsStyleService = KsolowToolsTelegram.styleService,
    parseMode: ParseMode? = null,
    action: String = "Команда /translate",
    log: Logger = ru.ksolowtools.telegram.client.log
) {
    val replyText = message.replyToMessage?.textOrCaption()
    val response = if (replyText.isNullOrBlank()) {
        KsolowToolsTelegramMessages.TRANSLATE_NEED_REPLY
    } else {
        apiClient.translate(
            style = styleService.requireStyle(message.chat.id),
            text = replyText,
            fallback = KsolowToolsTelegramMessages.AI_FALLBACK
        )
    }

    bot.sendMessageWithChunking(
        chatId = ChatId.fromId(message.chat.id),
        text = response,
        action = action,
        log = log,
        parseMode = parseMode
    )
}

fun CommandHandlerEnvironment.handleImage(
    apiClient: KsolowToolsApiClient = KsolowToolsTelegram.apiClient,
    action: String = "Команда /image",
    log: Logger = ru.ksolowtools.telegram.client.log
) {
    val chatId = ChatId.fromId(message.chat.id)
    val inlinePrompt = rawCommandText()
        ?.substringAfter(' ', "")
        ?.trim()
        .orEmpty()
    val replyPrompt = message.replyToMessage?.textOrCaption().orEmpty()
    val prompt = inlinePrompt.ifBlank { replyPrompt }.trim()

    if (prompt.isBlank()) {
        bot.sendMessage(
            chatId = chatId,
            text = KsolowToolsTelegramMessages.IMAGE_NEED_PROMPT
        ).also {
            it.logTelegramResult("$action (нет промпта)", log)
        }
        return
    }

    val imageUrl = apiClient.generateImage(prompt)
    if (imageUrl.isNullOrBlank()) {
        bot.sendMessage(
            chatId = chatId,
            text = KsolowToolsTelegramMessages.IMAGE_FALLBACK
        ).also {
            it.logTelegramResult("$action (ошибка генерации)", log)
        }
        return
    }

    bot.sendPhoto(
        chatId = chatId,
        photo = ByUrl(imageUrl)
    ).also {
        it.logTelegramCall(action, log)
    }
}

fun CommandHandlerEnvironment.handleToday(
    apiClient: KsolowToolsApiClient = KsolowToolsTelegram.apiClient,
    action: String = "Команда /today",
    log: Logger = ru.ksolowtools.telegram.client.log
) {
    bot.sendMessage(
        chatId = ChatId.fromId(message.chat.id),
        text = apiClient.today(KsolowToolsTelegramMessages.TODAY_UNAVAILABLE)
    ).also {
        it.logTelegramResult(action, log)
    }
}

fun CommandHandlerEnvironment.handleTomorrow(
    apiClient: KsolowToolsApiClient = KsolowToolsTelegram.apiClient,
    action: String = "Команда /tomorrow",
    log: Logger = ru.ksolowtools.telegram.client.log
) {
    bot.sendMessage(
        chatId = ChatId.fromId(message.chat.id),
        text = apiClient.tomorrow(KsolowToolsTelegramMessages.TOMORROW_UNAVAILABLE)
    ).also {
        it.logTelegramResult(action, log)
    }
}

fun CommandHandlerEnvironment.handleCat(
    apiClient: KsolowToolsApiClient = KsolowToolsTelegram.apiClient,
    action: String = "Команда /cat",
    log: Logger = ru.ksolowtools.telegram.client.log
) {
    bot.sendPhoto(
        chatId = ChatId.fromId(message.chat.id),
        photo = ByUrl(apiClient.randomCat())
    ).also {
        it.logTelegramCall(action, log)
    }
}

fun CommandHandlerEnvironment.handleSong(
    songSupport: TelegramSongSupport = KsolowToolsTelegram.songSupport,
    action: String = "Команда /song",
    log: Logger = ru.ksolowtools.telegram.client.log
) {
    val chatId = ChatId.fromId(message.chat.id)
    val replyText = message.replyToMessage?.textOrCaption()
    if (replyText.isNullOrBlank()) {
        bot.sendMessage(
            chatId = chatId,
            text = "Команду /song нужно вызывать реплаем на сообщение с текстом для песни."
        ).also {
            it.logTelegramResult("$action (нет реплая)", log)
        }
        return
    }

    val answer = songSupport.songText(message.chat.id, replyText).text

    bot.sendMessageWithChunking(
        chatId = chatId,
        text = answer,
        action = action,
        log = log
    )
}

fun CommandHandlerEnvironment.handleSuno(
    songSupport: TelegramSongSupport = KsolowToolsTelegram.songSupport,
    action: String = "Команда /suno",
    log: Logger = ru.ksolowtools.telegram.client.log
) {
    val chatId = ChatId.fromId(message.chat.id)
    val replyText = message.replyToMessage?.textOrCaption()
    if (replyText.isNullOrBlank()) {
        bot.sendMessage(
            chatId = chatId,
            text = "Команду /suno нужно вызывать реплаем на сообщение с промптом."
        ).also {
            it.logTelegramResult("$action (нет реплая)", log)
        }
        return
    }

    bot.sendMessage(
        chatId = chatId,
        text = "Отправил промпт в Suno, жду готовый mp3."
    ).also {
        it.logTelegramResult("$action (статус)", log)
    }

    val track = songSupport.songTrack(
        chatId = message.chat.id,
        prompt = replyText
    )

    if (!track.success || track.audioUrl.isNullOrBlank()) {
        bot.sendMessage(
            chatId = chatId,
            text = "Suno не вернул mp3: ${track.errorMessage ?: "Неизвестная ошибка"}"
        ).also {
            it.logTelegramResult("$action (ошибка Suno)", log)
        }
        return
    }

    bot.sendAudioFromUrl(
        chatId = chatId,
        audioUrl = track.audioUrl,
        performer = track.performer,
        title = track.title,
        duration = track.durationSeconds,
        replyToMessageId = message.replyToMessage?.messageId?.toLong(),
        allowSendingWithoutReply = true,
        action = action,
        log = log
    )
}

internal fun resolveWeatherLocationCode(
    commandText: String?,
    aliases: Map<String, String>
): String? {
    val cityArg = commandText
        ?.substringAfter(' ', "")
        ?.trim()
        ?.lowercase()
        .orEmpty()

    return when {
        cityArg.isBlank() -> aliases["spb"] ?: aliases.values.firstOrNull()
        else -> aliases[cityArg]
    }
}

private fun CommandHandlerEnvironment.rawCommandText(): String? = message.textOrCaption()

private fun CommandHandlerEnvironment.chatDisplayName(): String? =
    message.chat.title
        ?: message.chat.username
        ?: message.chat.firstName
        ?: message.chat.lastName
