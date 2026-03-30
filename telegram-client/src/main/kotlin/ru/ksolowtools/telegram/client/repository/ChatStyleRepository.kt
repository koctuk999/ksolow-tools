package ru.ksolowtools.telegram.client.repository

import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.setValue
import org.litote.kmongo.updateOne

class ChatStyleRepository(
    private val mongoSupport: MongoSupport
) {
    private val collection
        get() = mongoSupport.database.getCollection<TelegramChat>("chats")

    fun find(chatId: Long): TelegramChat? = collection.findOne(TelegramChat::chatId eq chatId)

    fun save(chatId: Long, chatName: String?, style: String) {
        val existing = find(chatId)
        if (existing == null) {
            collection.insertOne(
                TelegramChat(
                    chatId = chatId,
                    name = chatName,
                    style = style
                )
            )
        } else {
            collection.updateOne(
                TelegramChat::chatId eq chatId,
                setValue(TelegramChat::style, style)
            )
        }
    }
}

data class TelegramChat(
    val chatId: Long,
    val name: String? = null,
    val style: String? = null
)
