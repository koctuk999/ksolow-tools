package ru.ksolowtools.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.ksolowtools.client.ai.AIClient
import ru.ksolowtools.client.ai.AIRequestOptions
import ru.ksolowtools.client.suno.SunoClient
import ru.ksolowtools.client.suno.SunoGenerateRequest
import ru.ksolowtools.client.suno.SunoTaskCreateResult
import ru.ksolowtools.client.suno.SunoTrack
import ru.ksolowtools.service.style.PromptService
import ru.ksolowtools.service.style.StyleService
import kotlin.math.roundToInt

@Service
class SongService(
    private val aiClient: AIClient,
    private val promptService: PromptService,
    private val styleService: StyleService,
    private val sunoClient: SunoClient
) {
    private val log = LoggerFactory.getLogger(SongService::class.java)

    fun songTextStyled(request: StyledSongTextRequest): StyledSongTextResponse {
        val trimmedText = request.sourceText.trim()
        if (trimmedText.isBlank()) {
            return StyledSongTextResponse(
                style = styleService.songProfile(request.style).styleName,
                text = NO_MESSAGES_TEXT
            )
        }

        val songProfile = styleService.songProfile(request.style)
        val systemPrompt = buildSongSystemPrompt(songProfile)

        return StyledSongTextResponse(
            style = songProfile.styleName,
            text = aiClient.complete(
                systemPrompt = systemPrompt,
                userPrompt = "Текст для песни:\n$trimmedText",
                fallback = AI_FALLBACK_TEXT,
                options = AIRequestOptions(maxTokens = 2500)
            )
        )
    }

    fun songTrackStyled(request: StyledSongTrackRequest): StyledSongTrackTaskResponse {
        val songProfile = styleService.songProfile(request.style)
        val inputText = request.songText?.trim().takeUnless { it.isNullOrBlank() }
            ?: request.prompt?.trim().takeUnless { it.isNullOrBlank() }
            ?: return StyledSongTrackTaskResponse.failure(
                style = songProfile.styleName,
                performer = performerName(songProfile),
                lyrics = null,
                reason = EMPTY_TRACK_INPUT_REASON
            )

        val songDraft = parseSongDraft(inputText)
        val trackRequest = SunoGenerateRequest(
            prompt = songDraft.lyrics,
            customMode = true,
            instrumental = false,
            model = DEFAULT_MODEL,
            callBackUrl = "",
            style = songProfile.songTrackStyle,
            title = songDraft.title
        )

        return when (val createResult = sunoClient.generate(trackRequest)) {
            is SunoTaskCreateResult.Success -> StyledSongTrackTaskResponse.success(
                style = songProfile.styleName,
                performer = performerName(songProfile),
                lyrics = songDraft.lyrics,
                taskId = createResult.taskId
            )

            is SunoTaskCreateResult.Failure -> {
                log.warn("Не удалось поставить задачу в Suno: {}", createResult.reason)
                StyledSongTrackTaskResponse.failure(
                    style = songProfile.styleName,
                    performer = performerName(songProfile),
                    lyrics = songDraft.lyrics,
                    reason = createResult.reason
                )
            }
        }
    }

    fun songTrackStatus(taskId: String): StyledSongTrackStatusResponse {
        val details = sunoClient.generationDetails(taskId)
            ?: return StyledSongTrackStatusResponse.failure(
                taskId = taskId,
                reason = "Не удалось получить статус задачи Suno"
            )
        val tracks = details.response
            ?.tracks()
            .orEmpty()
            .filter { !it.audioUrl.isNullOrBlank() }
            .map { it.toReadyTrack() }

        return when (details.status?.uppercase()) {
            "SUCCESS" -> {
                if (tracks.isEmpty()) {
                    StyledSongTrackStatusResponse.failure(
                        taskId = taskId,
                        reason = "Suno не вернул ссылку на mp3"
                    )
                } else {
                    StyledSongTrackStatusResponse.success(
                        taskId = taskId,
                        complete = true,
                        tracks = tracks
                    )
                }
            }

            "FIRST_SUCCESS" -> StyledSongTrackStatusResponse.success(
                taskId = taskId,
                complete = false,
                tracks = tracks
            )

            "CREATE_TASK_FAILED",
            "GENERATE_AUDIO_FAILED",
            "SENSITIVE_WORD_ERROR",
            "FAILED" ->
                StyledSongTrackStatusResponse.failure(
                    taskId = taskId,
                    reason = details.errorMessage ?: "Suno не смог сгенерировать песню"
                )

            "TEXT_SUCCESS", "PENDING", "GENERATING", null ->
                StyledSongTrackStatusResponse.pending(
                    taskId = taskId,
                    tracks = tracks
                )

            else -> {
                log.warn("Неожиданный статус Suno: {}", details.status)
                StyledSongTrackStatusResponse.pending(
                    taskId = taskId,
                    tracks = tracks
                )
            }
        }
    }

    private fun buildSongSystemPrompt(songProfile: SongStyleProfile): String = buildString {
        appendLine(promptService.getPrompt("songText").systemPrompt)
        appendLine()
        appendLine("Стиль песни:")
        appendLine(songProfile.songTextStyle)
    }.trim()

    private fun performerName(songProfile: SongStyleProfile): String = "Suno ${songProfile.styleName}"

    private fun SunoTrack.toReadyTrack() = StyledSongReadyTrack(
        audioUrl = audioUrl.orEmpty(),
        imageUrl = imageUrl,
        title = title?.trim().takeUnless { it.isNullOrBlank() } ?: "Suno track",
        durationSeconds = duration?.roundToInt()
    )

    private fun parseSongDraft(text: String): SongDraft {
        val lines = text.lineSequence().toList()
        val firstNonBlankIndex = lines.indexOfFirst { it.isNotBlank() }
        val firstNonBlankLine = firstNonBlankIndex
            .takeIf { it >= 0 }
            ?.let(lines::get)
            ?.trim()
            .orEmpty()

        val explicitTitle = TITLE_REGEX.matchEntire(firstNonBlankLine)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        val title = explicitTitle?.take(MAX_TITLE_LENGTH)
            ?: lines.firstOrNull { it.isNotBlank() }?.trim()?.take(MAX_TITLE_LENGTH)
            ?: "Песня"

        val lyrics = if (explicitTitle != null && firstNonBlankIndex >= 0) {
            lines
                .drop(firstNonBlankIndex + 1)
                .dropWhile { it.isBlank() }
                .joinToString("\n")
                .trim()
                .ifBlank { text.trim() }
        } else {
            text.trim()
        }

        return SongDraft(
            title = title,
            lyrics = lyrics
        )
    }

    companion object {
        private const val DEFAULT_MODEL = "V4_5ALL"
        private const val MAX_TITLE_LENGTH = 80
        private const val NO_MESSAGES_TEXT = "Сегодня нечего подводить в итогах."
        private const val AI_FALLBACK_TEXT = "Не удалось сгенерировать текст песни."
        private const val EMPTY_TRACK_INPUT_REASON = "Не передан текст для генерации трека."
        private val TITLE_REGEX = Regex("""Название:\s*(.+)""", RegexOption.IGNORE_CASE)
    }
}

data class StyledSongTextRequest(
    val style: String,
    val sourceText: String
)

data class StyledSongTextResponse(
    val style: String,
    val text: String
)

data class StyledSongTrackRequest(
    val style: String,
    val prompt: String? = null,
    val songText: String? = null
)

data class StyledSongTrackTaskResponse(
    val style: String,
    val success: Boolean,
    val performer: String,
    val taskId: String? = null,
    val lyrics: String? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun success(
            style: String,
            performer: String,
            lyrics: String,
            taskId: String
        ) = StyledSongTrackTaskResponse(
            style = style,
            success = true,
            performer = performer,
            taskId = taskId,
            lyrics = lyrics
        )

        fun failure(
            style: String,
            performer: String,
            lyrics: String?,
            reason: String
        ) = StyledSongTrackTaskResponse(
            style = style,
            success = false,
            performer = performer,
            lyrics = lyrics,
            errorMessage = reason
        )
    }
}

data class StyledSongTrackStatusResponse(
    val taskId: String,
    val complete: Boolean,
    val success: Boolean,
    val tracks: List<StyledSongReadyTrack> = emptyList(),
    val errorMessage: String? = null
) {
    companion object {
        fun pending(
            taskId: String,
            tracks: List<StyledSongReadyTrack> = emptyList()
        ) = StyledSongTrackStatusResponse(
            taskId = taskId,
            complete = false,
            success = false,
            tracks = tracks
        )

        fun success(
            taskId: String,
            complete: Boolean,
            tracks: List<StyledSongReadyTrack>
        ) = StyledSongTrackStatusResponse(
            taskId = taskId,
            complete = complete,
            success = true,
            tracks = tracks
        )

        fun failure(
            taskId: String,
            reason: String
        ) = StyledSongTrackStatusResponse(
            taskId = taskId,
            complete = true,
            success = false,
            errorMessage = reason
        )
    }
}

data class StyledSongReadyTrack(
    val audioUrl: String,
    val imageUrl: String? = null,
    val title: String,
    val durationSeconds: Int? = null
)

data class SongStyleProfile(
    val styleName: String,
    val songTextStyle: String,
    val songTrackStyle: String
)

private data class SongDraft(
    val title: String,
    val lyrics: String
)
