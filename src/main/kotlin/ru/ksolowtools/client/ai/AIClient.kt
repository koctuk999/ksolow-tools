package ru.ksolowtools.client.ai

interface AIClient {
    fun complete(
        systemPrompt: String,
        userPrompt: String,
        fallback: String,
        options: AIRequestOptions = AIRequestOptions()
    ): String
}
