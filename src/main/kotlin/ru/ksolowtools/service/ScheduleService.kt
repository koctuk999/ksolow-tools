package ru.ksolowtools.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ScheduleService(
    private val dayService: DayService
) {

    @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Moscow")
    fun clearHolidayCache() {
        dayService.clearHolidayCache()
        dayService.clearStyledHolidayCache()
    }
}
