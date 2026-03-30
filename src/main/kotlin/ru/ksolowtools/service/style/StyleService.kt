package ru.ksolowtools.service.style

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
class StyleService {
    private val yamlMapper = ObjectMapper(YAMLFactory())

    private val loadedStyles: Map<String, StylePrompts> by lazy {
        val resource = ClassPathResource("styles/styles.yml")
        resource.inputStream.use { inputStream ->
            val file = yamlMapper.readValue(inputStream, StyleFile::class.java)
            file.styles.mapValues { (_, style) ->
                StylePrompts(
                    style = style.style.trim(),
                    chat = mergePrompts(style.style, style.chat, file.prompts.chat),
                    weather = mergePrompts(style.style, style.weather, file.prompts.weather),
                    daySummary = mergePrompts(style.style, style.daySummary, file.prompts.daySummary),
                    holidays = mergePrompts(style.style, style.holidays, file.prompts.holidays),
                    morning = mergePrompts(style.style, style.morning, file.prompts.morning)
                )
            }
        }
    }

    fun getStyleNames(): List<String> = loadedStyles.keys.sorted()

    fun getStylePrompts(styleName: String): StylePrompts = requireNotNull(loadedStyles[styleName]) {
        "Стиль '$styleName' не найден"
    }

    private fun mergePrompts(vararg parts: String): String = parts
        .map(String::trim)
        .filter(String::isNotEmpty)
        .joinToString("\n\n")
}
