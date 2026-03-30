package ru.ksolowtools.telegram.client

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
}
