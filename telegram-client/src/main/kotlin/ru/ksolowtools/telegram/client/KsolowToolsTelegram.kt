package ru.ksolowtools.telegram.client

import ru.ksolowtools.telegram.client.api.KsolowToolsApiClient
import ru.ksolowtools.telegram.client.repository.ChatStyleRepository
import ru.ksolowtools.telegram.client.repository.DayMessageRepository
import ru.ksolowtools.telegram.client.repository.MongoSupport
import ru.ksolowtools.telegram.client.security.MessageEncryptionService
import ru.ksolowtools.telegram.client.style.KsolowToolsStyleService

object KsolowToolsTelegram {

    private var state: State? = null

    fun configure(config: KsolowToolsTelegramClientConfig) {
        val apiClient = KsolowToolsApiClient(config)
        val mongoSupport = MongoSupport(config)
        val encryptionService = MessageEncryptionService(config.messagesEncryptionKey)
        val chatStyleRepository = ChatStyleRepository(mongoSupport)
        val dayMessageRepository = DayMessageRepository(
            mongoSupport = mongoSupport,
            config = config,
            encryptionService = encryptionService
        )
        state = State(
            config = config,
            apiClient = apiClient,
            chatStyleRepository = chatStyleRepository,
            dayMessageRepository = dayMessageRepository,
            styleService = KsolowToolsStyleService(
                config = config,
                apiClient = apiClient,
                chatStyleRepository = chatStyleRepository
            )
        )
    }

    val config: KsolowToolsTelegramClientConfig
        get() = requireState().config

    val apiClient: KsolowToolsApiClient
        get() = requireState().apiClient

    val styleService: KsolowToolsStyleService
        get() = requireState().styleService

    val dayMessageRepository: DayMessageRepository
        get() = requireState().dayMessageRepository

    private fun requireState(): State = requireNotNull(state) {
        "KsolowToolsTelegram is not configured. Call KsolowToolsTelegram.configure(...) first."
    }

    private data class State(
        val config: KsolowToolsTelegramClientConfig,
        val apiClient: KsolowToolsApiClient,
        val chatStyleRepository: ChatStyleRepository,
        val dayMessageRepository: DayMessageRepository,
        val styleService: KsolowToolsStyleService
    )
}
