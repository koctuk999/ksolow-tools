package ru.ksolowtools.client.ai

data class AIRequestOptions(
    val temperature: Double = 0.7,
    val maxTokens: Int = 1000
)
