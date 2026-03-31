package ru.ksolowtools.telegram.client

data class KsolowToolsTelegramClientConfig(
    val serviceUrl: String,
    val mongoUrl: String,
    val messagesEncryptionKey: String,
    val mongoDatabase: String = "bot",
    val dayZoneId: String = "Europe/Moscow",
    val allowedIds: Set<Long> = emptySet(),
    val defaultStyle: String? = null,
    val notAllowedMessage: String = "Этот чат не разрешен.",
    val aiFallbackMessage: String = "Не удалось сгенерировать ответ.",
    val explainNeedQuestionMessage: String = "Нужно написать вопрос после /explain.",
    val weatherUnknownCityMessage: String = "Для этого города пока нет прогноза.",
    val dayNoMessagesMessage: String = "Сегодня сообщений для саммари еще нет.",
    val styleListTemplate: String = "Текущий стиль: {style}\nДоступные стили: {styles}",
    val styleUnknownTemplate: String = "Стиль '{style}' не найден. Доступные стили: {styles}",
    val styleSetSuccessTemplate: String = "Стиль переключен на '{style}'.",
    val weatherLocationAliases: Map<String, String> = DEFAULT_WEATHER_LOCATION_ALIASES
)

val DEFAULT_WEATHER_LOCATION_ALIASES = mapOf(
    "spb" to "spb",
    "спб" to "spb",
    "питер" to "spb",
    "санкт-петербург" to "spb",
    "санктпетербург" to "spb"
)
