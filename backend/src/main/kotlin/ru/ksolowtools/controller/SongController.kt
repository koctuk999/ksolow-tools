package ru.ksolowtools.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.ksolowtools.service.SongService
import ru.ksolowtools.service.StyledSongTextRequest
import ru.ksolowtools.service.StyledSongTrackRequest

@RestController
@RequestMapping("/song")
class SongController(
    private val songService: SongService
) {

    @PostMapping("/text/styled")
    fun songTextStyled(
        @RequestBody request: StyledSongTextRequest
    ) = songService.songTextStyled(request)

    @PostMapping("/track/styled")
    fun songTrackStyled(
        @RequestBody request: StyledSongTrackRequest
    ) = songService.songTrackStyled(request)
}
