package ru.ksolowtools.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.ksolowtools.client.ai.AIClient
import ru.ksolowtools.client.day.DayClient
import ru.ksolowtools.service.TargetDay.TODAY
import ru.ksolowtools.service.TargetDay.TOMORROW
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities.Private

@Service
class DayService(
    private val aiClient: AIClient,
    private val dayClient: DayClient
) {

    private val log = LoggerFactory.getLogger(DayService::class.java)

    fun holidaysTodayList() = getHolidays(TODAY)
    fun holidaysTomorrowList() = getHolidays(TOMORROW)
}
