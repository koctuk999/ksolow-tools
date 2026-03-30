package ru.ksolowtools.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.ksolowtools.client.cat.CatClient
import ru.ksolowtools.service.style.StyleService

@RestController
@RequestMapping("/tools")
class ToolsController(
    private val styleService: StyleService,
    private val catClient: CatClient
) {
    @GetMapping("/styles")
    fun getStyles() = styleService.getStyleNames()

    @GetMapping("/cat")
    fun randomCat() = CatResponse(
        url = catClient.randomCat()
    )
}

data class CatResponse(
    val url: String
)
