package ru.ksolowtools.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.ksolowtools.service.style.StyleService

@RestController
@RequestMapping("/tools")
class ToolsController(
    private val styleService: StyleService
) {
    @GetMapping("/styles")
    fun getStyles() = styleService.getStyleNames()
}
