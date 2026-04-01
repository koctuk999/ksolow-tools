package ru.ksolowtools.service

import org.springframework.stereotype.Service
import ru.ksolowtools.client.ai.AIClient
import ru.ksolowtools.client.ai.AIRequestOptions
import ru.ksolowtools.client.ai.aitunnel.AitunnelImageClient
import ru.ksolowtools.service.style.PromptService

@Service
class AIGatewayService(
    private val aiClient: AIClient,
    private val promptService: PromptService,
    private val aitunnelImageClient: AitunnelImageClient
) {

    fun sendRequest(request: AIProxyRequest) = AIProxyResponse(
        text = aiClient.complete(
            systemPrompt = "",
            userPrompt = request.prompt.trim(),
            fallback = request.prompt.trim(),
            options = AIRequestOptions()
        )
    )

    fun sendStyledRequest(request: StyledAIProxyRequest) = StyledAIProxyResponse(
        style = request.style,
        text = aiClient.complete(
            systemPrompt = promptService.buildSystemPrompt(request.style, "chat"),
            userPrompt = buildStyledUserPrompt(request),
            fallback = request.text.trim(),
            options = AIRequestOptions()
        )
    )

    fun explain(request: ExplainRequest) = ExplainResponse(
        style = request.style,
        text = aiClient.complete(
            systemPrompt = promptService.buildSystemPrompt(request.style, "explain"),
            userPrompt = request.question.trim(),
            fallback = request.question.trim(),
            options = AIRequestOptions()
        )
    )

    fun translate(request: StyledTranslateRequest) = StyledTranslateResponse(
        style = request.style,
        text = aiClient.complete(
            systemPrompt = promptService.buildSystemPrompt(request.style, "translate"),
            userPrompt = request.text.trim(),
            fallback = request.text.trim(),
            options = AIRequestOptions()
        )
    )

    fun generateImage(request: ImageGenerationRequest): ImageGenerationResponse {
        val prompt = request.prompt.trim()
        val image = aitunnelImageClient.generate(
            prompt = prompt,
            fallback = null
        )
        return ImageGenerationResponse(
            prompt = prompt,
            imageUrl = image?.url,
            imageBase64 = image?.base64
        )
    }

    private fun buildStyledUserPrompt(request: StyledAIProxyRequest): String = buildString {
        appendLine("Текст сообщения:")
        appendLine(request.text.trim())

        request.content
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { content ->
                appendLine()
                appendLine("Цитируемое сообщение:")
                appendLine(content)
            }
    }.trim()
}

data class AIProxyRequest(
    val prompt: String
)

data class StyledAIProxyRequest(
    val style: String,
    val text: String,
    val content: String? = null
)

data class ExplainRequest(
    val style: String,
    val question: String
)

data class StyledTranslateRequest(
    val style: String,
    val text: String
)

data class ImageGenerationRequest(
    val prompt: String
)

data class AIProxyResponse(
    val text: String
)

data class StyledAIProxyResponse(
    val style: String,
    val text: String
)

data class ExplainResponse(
    val style: String,
    val text: String
)

data class StyledTranslateResponse(
    val style: String,
    val text: String
)

data class ImageGenerationResponse(
    val prompt: String,
    val imageUrl: String? = null,
    val imageBase64: String? = null
)
