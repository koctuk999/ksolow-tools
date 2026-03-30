package ru.ksolowtools.service

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import ru.ksolowtools.client.CommonClient.sendRequest
import ru.ksolowtools.service.TargetDay.TODAY
import ru.ksolowtools.service.TargetDay.TOMORROW

fun getHolidays(day: TargetDay) = parseHolidaysTodayHtml(
    html = requireNotNull(
        sendRequest {
            url(
                when (day) {
                    TODAY -> "https://my-calend.ru/holidays"
                    TOMORROW -> "https://my-calend.ru/holidays/tomorrow"
                }
            )
            get()
        }.body?.string()
    ) { "Не получилось получить список сегодняшних событий" }
)

private fun parseHolidaysTodayHtml(
    html: String
): List<String> {

    val doc = Jsoup.parse(html)

    // Ищем заголовок, который начинается с "Праздники"
    val header = doc.select("h2")
        .firstOrNull { it.text().trim().startsWith("Праздники") }
        ?: return emptyList()

    // Ищем первый UL после этого заголовка
    val list = generateSequence(header.nextElementSibling()) { it.nextElementSibling() }
        .firstOrNull { it.tagName().equals("ul", ignoreCase = true) }
        ?: return emptyList()

    return list.select("li")
        .asSequence()
        .mapNotNull { li -> li.extractHolidayName() }
        .distinct()
        .toList()
}

/**
 * Достаём чистое название праздника:
 * - если есть <a> — берём текст ссылки
 * - иначе весь текст li
 * - удаляем хвостовые цифры (счётчики)
 */
private fun Element.extractHolidayName(): String? {
    val raw = selectFirst("a")?.text()?.takeIf { it.isNotBlank() }
        ?: text()

    return raw
        .replace(Regex("\\s+\\d+$"), "")   // убрать счётчик типа " 15"
        .trim()
        .takeIf { it.isNotBlank() }
}
