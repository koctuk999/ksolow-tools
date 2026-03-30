package ru.ksolowtools.service.style

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
class PromptService(
    private val styleService: StyleService
) {
    private val yamlMapper = ObjectMapper(YAMLFactory())

    private val loadedPrompts: Map<String, PromptDefinition> by lazy {
        val resource = ClassPathResource("prompts/prompts.yml")
        resource.inputStream.use { inputStream ->
            yamlMapper.readValue(inputStream, PromptFile::class.java)
                .prompts
                .mapValues { (_, prompt) -> prompt.copy(systemPrompt = prompt.systemPrompt.trim()) }
        }
    }

    fun getPrompt(promptName: String): PromptDefinition = requireNotNull(loadedPrompts[promptName]) {
        "Промпт '$promptName' не найден"
    }

    fun buildSystemPrompt(styleName: String, promptName: String): String = listOf(
        styleService.getStyle(styleName).systemPrompt,
        getPrompt(promptName).systemPrompt
    )
        .map(String::trim)
        .filter(String::isNotEmpty)
        .joinToString("\n\n")
}
