package ru.ksolowtools.telegram.client

import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment

interface Command {
    val value: String
}

fun Dispatcher.command(
    command: Command,
    body: CommandHandlerEnvironment.() -> Unit
) = command(command.value, body)
