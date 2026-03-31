package ru.ksolowtools.telegram.client

import ru.ksolowtools.telegram.client.api.KsolowToolsApiClient
import ru.ksolowtools.telegram.client.api.StyledEveningMessage
import ru.ksolowtools.telegram.client.api.StyledSongText
import ru.ksolowtools.telegram.client.api.StyledSongTrack
import ru.ksolowtools.telegram.client.repository.DayMessageRepository
import ru.ksolowtools.telegram.client.style.KsolowToolsStyleService

class TelegramSongSupport(
    private val apiClient: KsolowToolsApiClient,
    private val styleService: KsolowToolsStyleService,
    private val dayMessageRepository: DayMessageRepository
) {

    fun songText(chatId: Long, sourceText: String): StyledSongText =
        apiClient.songText(
            style = styleService.requireStyle(chatId),
            sourceText = sourceText
        )

    fun songTrack(
        chatId: Long,
        prompt: String? = null,
        songText: String? = null
    ): StyledSongTrack = apiClient.songTrack(
        style = styleService.requireStyle(chatId),
        prompt = prompt,
        songText = songText
    )

    fun eveningSong(chatId: Long): StyledEveningSong {
        val style = styleService.requireStyle(chatId)
        val eveningMessage = apiClient.eveningMessage(
            style = style,
            messages = dayMessageRepository.getByChatId(chatId)
        )
        val songText = apiClient.songText(style = style, sourceText = eveningMessage.text)
        val track = apiClient.songTrack(style = style, songText = songText.text)

        return StyledEveningSong(
            eveningMessage = eveningMessage,
            songText = songText,
            track = track
        )
    }
}

data class StyledEveningSong(
    val eveningMessage: StyledEveningMessage,
    val songText: StyledSongText,
    val track: StyledSongTrack
)
