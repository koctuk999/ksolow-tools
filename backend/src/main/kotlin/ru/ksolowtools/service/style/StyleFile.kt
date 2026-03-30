package ru.ksolowtools.service.style

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class StyleFile(
    val styles: Map<String, StyleDefinition> = emptyMap()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StyleDefinition(
    val systemPrompt: String = ""
)
