package ru.ksolowtools.telegram.client.repository

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.KMongo
import ru.ksolowtools.telegram.client.KsolowToolsTelegramClientConfig

class MongoSupport(config: KsolowToolsTelegramClientConfig) {
    val mongoClient: MongoClient = KMongo.createClient(config.mongoUrl)
    val database: MongoDatabase = mongoClient.getDatabase(config.mongoDatabase)
}
