package ru.ksolowtools.service.style

data class StylePrompts(
    val style: String = "",
    val chat: String = "",
    val weather: String = "",
    val daySummary: String = "",
    val holidays: String = "",
    val morning: String = ""
)
