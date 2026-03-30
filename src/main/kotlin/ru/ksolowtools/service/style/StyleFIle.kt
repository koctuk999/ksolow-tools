package ru.ksolowtools.service.style

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class StyleFile(
    val prompts: SharedPrompts = SharedPrompts(),
    val styles: Map<String, StyleDefinition> = emptyMap()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SharedPrompts(
    val chat: String = "",
    val weather: String = "",
    val daySummary: String = "",
    val holidays: String = "",
    val morning: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StyleDefinition(
    val style: String = "",
    val chat: String = "",
    val weather: String = "",
    val daySummary: String = "",
    val holidays: String = "",
    val morning: String = ""
)
