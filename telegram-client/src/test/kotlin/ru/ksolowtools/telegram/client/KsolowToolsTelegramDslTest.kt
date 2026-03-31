package ru.ksolowtools.telegram.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KsolowToolsTelegramDslTest {

    @Test
    fun `ksolow tools telegram bot configures shared state and returns bot`() {
        val bot = ksolowToolsTelegramBot {
            serviceUrl = "http://localhost:8080"
            mongoUrl = "mongodb://localhost:27017"
            mongoDatabase = "kidala-bot"
            messagesEncryptionKey = "1234567890abcdef"
            dayZoneId = "Europe/Moscow"
            allowedIds = setOf(1L, 2L)
            defaultStyle = "swear"

            bot {
                token = "token"
            }
        }

        assertNotNull(bot)
        assertEquals("http://localhost:8080", KsolowToolsTelegram.config.serviceUrl)
        assertEquals("mongodb://localhost:27017", KsolowToolsTelegram.config.mongoUrl)
        assertEquals("kidala-bot", KsolowToolsTelegram.config.mongoDatabase)
        assertEquals(setOf(1L, 2L), KsolowToolsTelegram.config.allowedIds)
        assertEquals("swear", KsolowToolsTelegram.config.defaultStyle)
    }
}
