package ru.ksolowtools.service.style

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PromptFile(
    val prompts: Map<String, PromptDefinition> = emptyMap()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PromptDefinition(
    val systemPrompt: String = ""
)
