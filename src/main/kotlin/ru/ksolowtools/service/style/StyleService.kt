package ru.ksolowtools.service.style

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
class StyleService {
    private val yamlMapper = ObjectMapper(YAMLFactory())

    private val loadedStyles: Map<String, StyleDefinition> by lazy {
        val resource = ClassPathResource("styles/styles.yml")
        resource.inputStream.use { inputStream ->
            yamlMapper.readValue(inputStream, StyleFile::class.java)
                .styles
                .mapValues { (_, style) -> style.copy(systemPrompt = style.systemPrompt.trim()) }
        }
    }

    fun getStyleNames(): List<String> = loadedStyles.keys.sorted()

    fun getStyle(styleName: String): StyleDefinition = requireNotNull(loadedStyles[styleName]) {
        "Стиль '$styleName' не найден"
    }
}
