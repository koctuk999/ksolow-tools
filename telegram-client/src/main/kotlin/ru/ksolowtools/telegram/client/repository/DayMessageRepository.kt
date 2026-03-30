package ru.ksolowtools.telegram.client.repository

import org.litote.kmongo.eq
import org.litote.kmongo.find
import org.litote.kmongo.getCollection
import org.slf4j.LoggerFactory
import ru.ksolowtools.telegram.client.KsolowToolsTelegramClientConfig
import ru.ksolowtools.telegram.client.security.MessageEncryptionService
import java.time.LocalDate
import java.time.ZoneId

class DayMessageRepository(
    private val mongoSupport: MongoSupport,
    private val config: KsolowToolsTelegramClientConfig,
    private val encryptionService: MessageEncryptionService
) {
    private val log = LoggerFactory.getLogger(DayMessageRepository::class.java)

    private val collection
        get() = mongoSupport.database.getCollection<TelegramDayMessage>("dayMessages")

    fun add(chatId: Long, message: String, dayKey: String = currentDayKey()) {
        collection.insertOne(
            TelegramDayMessage(
                chatId = chatId,
                dayKey = dayKey,
                message = encryptionService.encrypt(message)
            )
        )
    }

    fun getByChatId(chatId: Long, dayKey: String = currentDayKey()): List<String> =
        collection.find(
            TelegramDayMessage::chatId eq chatId,
            TelegramDayMessage::dayKey eq dayKey
        )
            .toList()
            .mapNotNull { encrypted ->
                runCatching { encryptionService.decrypt(encrypted.message) }
                    .onFailure { log.warn("Не удалось расшифровать сообщение для чата {}", chatId, it) }
                    .getOrNull()
            }

    fun currentDayKey(): String = LocalDate.now(ZoneId.of(config.dayZoneId)).toString()
}

data class TelegramDayMessage(
    val chatId: Long,
    val dayKey: String,
    val message: String
)
