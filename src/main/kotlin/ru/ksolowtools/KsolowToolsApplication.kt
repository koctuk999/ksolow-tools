package ru.ksolowtools

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KsolowToolsApplication

fun main(args: Array<String>) {
    runApplication<KsolowToolsApplication>(*args)
}
