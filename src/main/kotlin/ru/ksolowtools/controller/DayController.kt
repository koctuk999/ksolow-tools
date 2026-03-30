package ru.ksolowtools.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.ksolowtools.service.DayService

@RestController
@RequestMapping("/day")
class DayController(
    private val dayService: DayService
) {

    @GetMapping("/holidays/today")
    fun holidaysToday() = dayService.holidaysTodayList()

    @GetMapping("/holidays/tomorrow")
    fun holidaysTomorrow() = dayService.holidaysTomorrowList()

    @GetMapping("/holidays/today/styled")
    fun holidaysTodayStyled(@RequestParam style: String) = dayService.holidaysTodayStyled(style)

}
