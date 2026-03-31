package ru.ksolowtools.telegram.client

import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.Message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TelegramClientUtilsTest {

    @Test
    fun `splitIntoChunks returns one chunk for short text`() {
        assertEquals(listOf("abc"), splitIntoChunks("abc", 10))
    }

    @Test
    fun `splitIntoChunks splits long text`() {
        assertEquals(listOf("ab", "cd", "ef"), splitIntoChunks("abcdef", 2))
    }

    @Test
    fun `resolveWeatherLocationCode uses default spb alias when city is omitted`() {
        val result = resolveWeatherLocationCode("/weather", DEFAULT_WEATHER_LOCATION_ALIASES)

        assertEquals("spb", result)
    }

    @Test
    fun `resolveWeatherLocationCode resolves known city alias`() {
        val result = resolveWeatherLocationCode("/weather Питер", DEFAULT_WEATHER_LOCATION_ALIASES)

        assertEquals("spb", result)
    }

    @Test
    fun `resolveWeatherLocationCode returns null for unknown city`() {
        val result = resolveWeatherLocationCode("/weather msk", DEFAULT_WEATHER_LOCATION_ALIASES)

        assertNull(result)
    }

    @Test
    fun `textOrCaption returns trimmed text`() {
        val result = telegramMessage(text = "  hello  ", caption = "caption").textOrCaption()

        assertEquals("hello", result)
    }

    @Test
    fun `textOrCaption falls back to trimmed caption when text is blank`() {
        val result = telegramMessage(text = "   ", caption = "  caption  ").textOrCaption()

        assertEquals("caption", result)
    }

    @Test
    fun `textOrCaption returns null when text and caption are blank`() {
        val result = telegramMessage(text = " ", caption = "\n\t").textOrCaption()

        assertNull(result)
    }

    private fun telegramMessage(text: String? = null, caption: String? = null): Message = Message(
        messageId = 1,
        date = 0,
        chat = Chat(
            id = 1,
            type = "private"
        ),
        text = text,
        caption = caption
    )
}
