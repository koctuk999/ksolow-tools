package ru.ksolowtools.telegram.client

import ru.ksolowtools.telegram.client.api.KsolowToolsApiClient
import ru.ksolowtools.telegram.client.api.StyledEveningMessage
import ru.ksolowtools.telegram.client.api.StyledSongText
import ru.ksolowtools.telegram.client.api.StyledSongTrack
import ru.ksolowtools.telegram.client.api.StyledSongTrackStatus
import ru.ksolowtools.telegram.client.api.StyledSongTrackTask
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

    fun createSongTrack(
        chatId: Long,
        prompt: String? = null,
        songText: String? = null
    ): StyledSongTrackTask = apiClient.createSongTrack(
        style = styleService.requireStyle(chatId),
        prompt = prompt,
        songText = songText
    )

    fun songTrackStatus(taskId: String): StyledSongTrackStatus = apiClient.songTrackStatus(taskId)

    fun awaitSongTracks(task: StyledSongTrackTask): SongTrackGenerationResult {
        if (!task.success || task.taskId.isNullOrBlank()) {
            return SongTrackGenerationResult.failure(
                performer = task.performer,
                errorMessage = task.errorMessage ?: "Не удалось поставить задачу на генерацию."
            )
        }

        repeat(MAX_ATTEMPTS) { attempt ->
            val status = songTrackStatus(task.taskId)
            if (status.complete) {
                return if (status.success && status.tracks.isNotEmpty()) {
                    SongTrackGenerationResult.success(
                        performer = task.performer,
                        tracks = status.tracks
                    )
                } else {
                    SongTrackGenerationResult.failure(
                        performer = task.performer,
                        errorMessage = status.errorMessage ?: "Suno не вернул mp3."
                    )
                }
            }

            if (attempt < MAX_ATTEMPTS - 1) {
                Thread.sleep(POLL_INTERVAL_MS)
            }
        }

        val lastStatus = songTrackStatus(task.taskId)
        return if (lastStatus.tracks.isNotEmpty()) {
            SongTrackGenerationResult.success(
                performer = task.performer,
                tracks = lastStatus.tracks
            )
        } else {
            SongTrackGenerationResult.failure(
                performer = task.performer,
                errorMessage = "Suno не успел сгенерировать песню за отведенное время"
            )
        }
    }

    fun eveningSong(chatId: Long): StyledEveningSong {
        val style = styleService.requireStyle(chatId)
        val eveningMessage = apiClient.eveningMessage(
            style = style,
            messages = dayMessageRepository.getByChatId(chatId)
        )
        val songText = apiClient.songText(style = style, sourceText = eveningMessage.text)
        val trackTask = apiClient.createSongTrack(style = style, songText = songText.text)
        val track = awaitSongTracks(trackTask)

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
    val track: SongTrackGenerationResult
)

data class SongTrackGenerationResult(
    val success: Boolean,
    val performer: String,
    val tracks: List<StyledSongTrack> = emptyList(),
    val errorMessage: String? = null
) {
    companion object {
        fun success(
            performer: String,
            tracks: List<StyledSongTrack>
        ) = SongTrackGenerationResult(
            success = true,
            performer = performer,
            tracks = tracks
        )

        fun failure(
            performer: String,
            errorMessage: String
        ) = SongTrackGenerationResult(
            success = false,
            performer = performer,
            errorMessage = errorMessage
        )
    }
}

private const val MAX_ATTEMPTS = 40
private const val POLL_INTERVAL_MS = 15_000L
