package ru.ksolowtools.telegram.client

internal object KsolowToolsTelegramMessages {
    const val NOT_ALLOWED = "Этот чат не разрешен."
    const val AI_FALLBACK = "Не удалось сгенерировать ответ."
    const val TRANSLATE_NEED_REPLY = "Команду /translate нужно вызывать реплаем на сообщение."
    const val EXPLAIN_NEED_QUESTION = "Нужно написать вопрос после /explain."
    const val TODAY_UNAVAILABLE = "Не удалось получить статус дня."
    const val TOMORROW_UNAVAILABLE = "Не удалось получить статус дня."
    const val WEATHER_UNKNOWN_CITY = "Для этого города пока нет прогноза."
    const val DAY_NO_MESSAGES = "Сегодня сообщений для саммари еще нет."
    const val STYLE_LIST_TEMPLATE = "Текущий стиль: {style}\nДоступные стили: {styles}"
    const val STYLE_UNKNOWN_TEMPLATE = "Стиль '{style}' не найден. Доступные стили: {styles}"
    const val STYLE_SET_SUCCESS_TEMPLATE = "Стиль переключен на '{style}'."
}
