package ru.ksolowtools.service.style

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import ru.ksolowtools.service.SongStyleProfile

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

    fun songProfile(styleName: String?): SongStyleProfile {
        val resolvedStyleName = styleName
            ?.takeIf { it in loadedStyles }
            ?: DEFAULT_SONG_STYLE_NAME
        val style = loadedStyles[resolvedStyleName] ?: DEFAULT_STYLE_DEFINITION

        return SongStyleProfile(
            styleName = resolvedStyleName,
            songTextStyle = style.songTextStyle.ifBlank { DEFAULT_STYLE_DEFINITION.songTextStyle },
            songTrackStyle = style.songTrackStyle.ifBlank { DEFAULT_STYLE_DEFINITION.songTrackStyle }
        )
    }

    private companion object {
        private const val DEFAULT_SONG_STYLE_NAME = "modni"

        private val DEFAULT_STYLE_DEFINITION = StyleDefinition(
            songTextStyle = "модный - надоедливая типичная попса",
            songTrackStyle = "Русская поп-песня, цепкий припев, выразительный вокал"
        )
    }
}
