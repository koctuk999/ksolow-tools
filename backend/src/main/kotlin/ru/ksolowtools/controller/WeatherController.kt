package ru.ksolowtools.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.ksolowtools.client.wheather.WeatherLocation
import ru.ksolowtools.service.WeatherService

@RestController
@RequestMapping("/weather")
class WeatherController(
    private val weatherService: WeatherService
) {

    @GetMapping("/current")
    fun current(
        @RequestParam(defaultValue = "spb") location: String
    ) = weatherService.current(
        location = WeatherLocation.fromCode(location)
    )

    @GetMapping("/current/styled")
    fun currentStyled(
        @RequestParam style: String,
        @RequestParam(defaultValue = "spb") location: String
    ) = weatherService.currentStyled(
        style = style,
        location = WeatherLocation.fromCode(location)
    )
}
