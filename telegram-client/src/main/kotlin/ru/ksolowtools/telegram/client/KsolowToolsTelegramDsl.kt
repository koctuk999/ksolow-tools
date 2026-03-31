package ru.ksolowtools.telegram.client

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot as telegramBot
import com.github.kotlintelegrambot.dispatch as telegramDispatch
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.handlers.MessageHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.message as telegramMessage

@DslMarker
annotation class KsolowToolsTelegramDsl

@KsolowToolsTelegramDsl
class KsolowToolsTelegramBotBuilder internal constructor() {

    var serviceUrl: String? = null
    var mongoUrl: String? = null
    var messagesEncryptionKey: String? = null
    var mongoDatabase: String = "bot"
    var dayZoneId: String = "Europe/Moscow"
    var allowedIds: Set<Long> = emptySet()
    var defaultStyle: String? = null
    var notAllowedMessage: String = "Этот чат не разрешен."
    var aiFallbackMessage: String = "Не удалось сгенерировать ответ."
    var explainNeedQuestionMessage: String = "Нужно написать вопрос после /explain."
    var weatherUnknownCityMessage: String = "Для этого города пока нет прогноза."
    var dayNoMessagesMessage: String = "Сегодня сообщений для саммари еще нет."
    var styleListTemplate: String = "Текущий стиль: {style}\nДоступные стили: {styles}"
    var styleUnknownTemplate: String = "Стиль '{style}' не найден. Доступные стили: {styles}"
    var styleSetSuccessTemplate: String = "Стиль переключен на '{style}'."
    var weatherLocationAliases: Map<String, String> = DEFAULT_WEATHER_LOCATION_ALIASES

    private var telegramBot: Bot? = null

    fun bot(body: Bot.Builder.() -> Unit) {
        telegramBot = telegramBot(body)
    }

    internal fun buildConfig(): KsolowToolsTelegramClientConfig =
        KsolowToolsTelegramClientConfig(
            serviceUrl = required("serviceUrl", serviceUrl),
            mongoUrl = required("mongoUrl", mongoUrl),
            messagesEncryptionKey = required("messagesEncryptionKey", messagesEncryptionKey),
            mongoDatabase = mongoDatabase,
            dayZoneId = dayZoneId,
            allowedIds = allowedIds,
            defaultStyle = defaultStyle,
            notAllowedMessage = notAllowedMessage,
            aiFallbackMessage = aiFallbackMessage,
            explainNeedQuestionMessage = explainNeedQuestionMessage,
            weatherUnknownCityMessage = weatherUnknownCityMessage,
            dayNoMessagesMessage = dayNoMessagesMessage,
            styleListTemplate = styleListTemplate,
            styleUnknownTemplate = styleUnknownTemplate,
            styleSetSuccessTemplate = styleSetSuccessTemplate,
            weatherLocationAliases = weatherLocationAliases
        )

    internal fun buildBot(): Bot = requireNotNull(telegramBot) {
        "Telegram bot is not configured. Define it via bot { ... } inside ksolowToolsTelegramBot { ... }."
    }
}

fun ksolowToolsTelegramBot(body: KsolowToolsTelegramBotBuilder.() -> Unit): Bot {
    val builder = KsolowToolsTelegramBotBuilder().apply(body)
    KsolowToolsTelegram.configure(builder.buildConfig())
    return builder.buildBot()
}

fun bot(body: Bot.Builder.() -> Unit): Bot = telegramBot(body)

fun Bot.Builder.dispatch(body: Dispatcher.() -> Unit) {
    telegramDispatch(body)
}

fun Dispatcher.message(body: MessageHandlerEnvironment.() -> Unit) {
    telegramMessage(body)
}

private fun required(name: String, value: String?): String = requireNotNull(value) {
    "$name must be set in ksolowToolsTelegramBot { ... }."
}
