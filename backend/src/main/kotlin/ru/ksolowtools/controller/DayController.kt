package ru.ksolowtools.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.ksolowtools.service.DayService
import ru.ksolowtools.service.DaySummaryService
import ru.ksolowtools.service.StyledDaySummaryRequest

@RestController
@RequestMapping("/day")
class DayController(
    private val dayService: DayService,
    private val daySummaryService: DaySummaryService
) {

    @GetMapping("/holidays/today")
    fun holidaysToday() = dayService.holidaysTodayList()

    @GetMapping("/holidays/tomorrow")
    fun holidaysTomorrow() = dayService.holidaysTomorrowList()

    @GetMapping("/holidays/today/styled")
    fun holidaysTodayStyled(@RequestParam style: String) = dayService.holidaysTodayStyled(style)

    @PostMapping("/summary/styled")
    fun summaryStyled(@RequestBody request: StyledDaySummaryRequest) = daySummaryService.summarizeStyled(
        style = request.style,
        messages = request.messages
    )

}
