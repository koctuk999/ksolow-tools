package ru.ksolowtools.telegram.client

data class KsolowToolsTelegramClientConfig(
    val serviceUrl: String,
    val mongoUrl: String,
    val messagesEncryptionKey: String,
    val mongoDatabase: String = "bot",
    val dayZoneId: String = "Europe/Moscow",
    val allowedIds: Set<Long> = emptySet(),
    val defaultStyle: String? = null,
    val weatherLocationAliases: Map<String, String> = DEFAULT_WEATHER_LOCATION_ALIASES
)

val DEFAULT_WEATHER_LOCATION_ALIASES = mapOf(
    "spb" to "spb",
    "спб" to "spb",
    "питер" to "spb",
    "санкт-петербург" to "spb",
    "санктпетербург" to "spb",
    "krasnoyarsk" to "krasnoyarsk",
    "красноярск" to "krasnoyarsk",
    "krsk" to "krasnoyarsk"
)
