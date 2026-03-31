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

    fun songTrackStyled(request: StyledSongTrackRequest): StyledSongTrackResponse {
        val songProfile = styleService.songProfile(request.style)
        val inputText = request.songText?.trim().takeUnless { it.isNullOrBlank() }
            ?: request.prompt?.trim().takeUnless { it.isNullOrBlank() }
            ?: return StyledSongTrackResponse.failure(
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

        return when (val result = generateAndAwait(trackRequest)) {
            is SongGenerationResult.Success -> StyledSongTrackResponse.success(
                style = songProfile.styleName,
                performer = performerName(songProfile),
                lyrics = songDraft.lyrics,
                title = result.track.title,
                audioUrl = result.track.audioUrl,
                durationSeconds = result.track.durationSeconds
            )

            is SongGenerationResult.Failure -> StyledSongTrackResponse.failure(
                style = songProfile.styleName,
                performer = performerName(songProfile),
                lyrics = songDraft.lyrics,
                reason = result.reason
            )
        }
    }

    private fun generateAndAwait(request: SunoGenerateRequest): SongGenerationResult {
        val taskId = when (val createResult = sunoClient.generate(request)) {
            is SunoTaskCreateResult.Success -> createResult.taskId
            is SunoTaskCreateResult.Failure -> {
                log.warn("Не удалось поставить задачу в Suno: {}", createResult.reason)
                return SongGenerationResult.Failure(createResult.reason)
            }
        }

        repeat(MAX_ATTEMPTS) { attempt ->
            val details = sunoClient.generationDetails(taskId)
                ?: return SongGenerationResult.Failure("Не удалось получить статус задачи Suno")
            val track = details.response?.tracks()?.firstOrNull { !it.audioUrl.isNullOrBlank() }
            when (details.status?.uppercase()) {
                "SUCCESS", "FIRST_SUCCESS" -> {
                    if (track == null) {
                        return SongGenerationResult.Failure("Suno не вернул ссылку на mp3")
                    }
                    return SongGenerationResult.Success(track.toReadyTrack())
                }

                "CREATE_TASK_FAILED",
                "GENERATE_AUDIO_FAILED",
                "SENSITIVE_WORD_ERROR",
                "FAILED" ->
                    return SongGenerationResult.Failure(
                        details.errorMessage ?: "Suno не смог сгенерировать песню"
                    )

                "TEXT_SUCCESS", "PENDING", "GENERATING", null -> {
                    if (attempt < MAX_ATTEMPTS - 1) {
                        Thread.sleep(POLL_INTERVAL_MS)
                    }
                }

                else -> {
                    log.warn("Неожиданный статус Suno: {}", details.status)
                    if (attempt < MAX_ATTEMPTS - 1) {
                        Thread.sleep(POLL_INTERVAL_MS)
                    }
                }
            }
        }

        return SongGenerationResult.Failure("Suno не успел сгенерировать песню за отведенное время")
    }

    private fun buildSongSystemPrompt(songProfile: SongStyleProfile): String = buildString {
        appendLine(promptService.getPrompt("songText").systemPrompt)
        appendLine()
        appendLine("Стиль песни:")
        appendLine(songProfile.songTextStyle)
    }.trim()

    private fun performerName(songProfile: SongStyleProfile): String = "Suno ${songProfile.styleName}"

    private fun SunoTrack.toReadyTrack() = SongReadyTrack(
        audioUrl = audioUrl.orEmpty(),
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
        private const val MAX_ATTEMPTS = 40
        private const val POLL_INTERVAL_MS = 15_000L
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

data class StyledSongTrackResponse(
    val style: String,
    val success: Boolean,
    val performer: String,
    val title: String? = null,
    val audioUrl: String? = null,
    val durationSeconds: Int? = null,
    val lyrics: String? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun success(
            style: String,
            performer: String,
            lyrics: String,
            title: String,
            audioUrl: String,
            durationSeconds: Int?
        ) = StyledSongTrackResponse(
            style = style,
            success = true,
            performer = performer,
            title = title,
            audioUrl = audioUrl,
            durationSeconds = durationSeconds,
            lyrics = lyrics
        )

        fun failure(
            style: String,
            performer: String,
            lyrics: String?,
            reason: String
        ) = StyledSongTrackResponse(
            style = style,
            success = false,
            performer = performer,
            lyrics = lyrics,
            errorMessage = reason
        )
    }
}

data class SongStyleProfile(
    val styleName: String,
    val songTextStyle: String,
    val songTrackStyle: String
)

private data class SongDraft(
    val title: String,
    val lyrics: String
)

private data class SongReadyTrack(
    val audioUrl: String,
    val title: String,
    val durationSeconds: Int?
)

private sealed interface SongGenerationResult {
    data class Success(val track: SongReadyTrack) : SongGenerationResult
    data class Failure(val reason: String) : SongGenerationResult
}
