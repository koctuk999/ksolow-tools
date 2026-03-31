package ru.ksolowtools.telegram.client.style

import org.slf4j.LoggerFactory
import ru.ksolowtools.telegram.client.KsolowToolsTelegramClientConfig
import ru.ksolowtools.telegram.client.api.KsolowToolsApiClient
import ru.ksolowtools.telegram.client.repository.ChatStyleRepository
import java.time.Duration
import java.time.Instant

class KsolowToolsStyleService(
    private val config: KsolowToolsTelegramClientConfig,
    private val apiClient: KsolowToolsApiClient,
    private val chatStyleRepository: ChatStyleRepository
) {
    private val log = LoggerFactory.getLogger(KsolowToolsStyleService::class.java)

    @Volatile
    private var cachedStyles: CachedStyles? = null

    fun availableStyles(): List<String> {
        val current = cachedStyles
        if (current != null && current.expiresAt.isAfter(Instant.now())) {
            return current.values
        }

        val styles = apiClient.styles()
        cachedStyles = CachedStyles(
            values = styles,
            expiresAt = Instant.now().plus(CACHE_TTL)
        )
        return styles
    }

    fun resolveStyleName(chatId: Long): String? {
        val availableStyles = availableStyles()
        val storedStyle = chatStyleRepository.find(chatId)?.style
        if (!storedStyle.isNullOrBlank() && storedStyle in availableStyles) {
            return storedStyle
        }

        val defaultStyle = config.defaultStyle
        if (!defaultStyle.isNullOrBlank() && defaultStyle in availableStyles) {
            return defaultStyle
        }

        return availableStyles.firstOrNull()
    }

    fun requireStyle(chatId: Long): String = requireNotNull(resolveStyleName(chatId)) {
        "Не удалось определить стиль для чата $chatId"
    }

    fun resolveRequestedStyle(requestedStyle: String): String? {
        val normalized = requestedStyle.trim().lowercase().replace(" ", "")
        return normalized.takeIf { it in availableStyles() }
    }

    fun saveStyleForChat(chatId: Long, chatName: String?, style: String) {
        chatStyleRepository.save(chatId, chatName, style)
        log.info("Сохранен стиль {} для чата {}", style, chatId)
    }

    fun format(template: String, variables: Map<String, String>): String =
        variables.entries.fold(template) { acc, (key, value) ->
            acc.replace("{$key}", value)
        }

    private data class CachedStyles(
        val values: List<String>,
        val expiresAt: Instant
    )

    private companion object {
        val CACHE_TTL: Duration = Duration.ofMinutes(5)
    }
}
