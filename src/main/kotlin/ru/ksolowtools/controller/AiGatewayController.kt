package ru.ksolowtools.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.ksolowtools.service.AIGatewayService
import ru.ksolowtools.service.AIProxyRequest
import ru.ksolowtools.service.StyledAIProxyRequest

@RestController
@RequestMapping("/ai/proxy")
class AiGatewayController(private val aiGatewayService: AIGatewayService) {

    @PostMapping("/request")
    fun request(
        @RequestBody request: AIProxyRequest
    ) = aiGatewayService.sendRequest(request)

    @PostMapping("/request/styled")
    fun styledRequest(
        @RequestBody request: StyledAIProxyRequest
    ) = aiGatewayService.sendStyledRequest(request)
}
