package ru.ksolowtools.telegram.client

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.network.Response
import com.github.kotlintelegrambot.types.TelegramBotResult
import org.slf4j.Logger
import retrofit2.Response as RetrofitResponse

fun <T> TelegramBotResult<T>.logTelegramResult(action: String, log: Logger) {
    fold(
        ifSuccess = { log.info("$action: ok") },
        ifError = { error ->
            val description = when (error) {
                is TelegramBotResult.Error.HttpError ->
                    "http=${error.httpCode}, description=${error.description}"

                is TelegramBotResult.Error.TelegramApi ->
                    "code=${error.errorCode}, description=${error.description}"

                is TelegramBotResult.Error.InvalidResponse ->
                    "http=${error.httpCode}, status=${error.httpStatusMessage}"

                is TelegramBotResult.Error.Unknown ->
                    "exception=${error.exception::class.java.simpleName}"
            }
            log.warn("$action: failed ($description)")
        }
    )
}

fun <T> Pair<RetrofitResponse<Response<T>?>?, Exception?>.logTelegramCall(action: String, log: Logger) {
    val exception = second
    val response = first
    when {
        exception != null -> log.warn("$action: failed (exception)", exception)
        response == null -> log.warn("$action: failed (empty response)")
        !response.isSuccessful -> log.warn("$action: failed (http=${response.code()} ${response.message()})")
        response.body()?.ok == true -> log.info("$action: ok")
        else -> log.warn(
            "$action: failed (code=${response.body()?.errorCode}, description=${response.body()?.errorDescription})"
        )
    }
}

fun Bot.sendMessageWithChunking(
    chatId: ChatId,
    text: String,
    action: String,
    log: Logger,
    parseMode: ParseMode? = null,
    maxLength: Int = 4096,
    escapeHtml: Boolean = false
) {
    val processedText = if (escapeHtml) text.escapeHtml() else text
    val chunks = splitIntoChunks(processedText, maxLength)
    if (chunks.size > 1) {
        log.warn("Текст длинный, отправляем ${chunks.size} сообщений для '$action'")
    }
    chunks.forEachIndexed { index, chunk ->
        val label = if (chunks.size > 1) "$action (часть ${index + 1}/${chunks.size})" else action
        sendMessage(chatId = chatId, text = chunk, parseMode = parseMode).also {
            it.logTelegramResult(label, log)
        }
    }
}

fun Bot.sendPhotoWithTruncatedCaption(
    chatId: ChatId,
    photo: TelegramFile,
    caption: String?,
    action: String,
    log: Logger,
    parseMode: ParseMode? = null,
    maxCaptionLength: Int = 1024,
    escapeHtml: Boolean = false
) {
    val processedCaption = caption?.let { if (escapeHtml) it.escapeHtml() else it }
    val trimmedCaption = if (processedCaption != null && processedCaption.length > maxCaptionLength) {
        log.warn("Подпись к фото превышает лимит, отправляем продолжением")
        processedCaption.take(maxCaptionLength)
    } else {
        processedCaption
    }

    sendPhoto(chatId = chatId, photo = photo, caption = trimmedCaption, parseMode = parseMode).also {
        it.logTelegramCall(action, log)
    }

    if (processedCaption != null && processedCaption.length > maxCaptionLength) {
        val remainder = processedCaption.drop(maxCaptionLength)
        if (remainder.isNotBlank()) {
            sendMessageWithChunking(
                chatId = chatId,
                text = remainder,
                action = "$action (продолжение)",
                log = log,
                parseMode = parseMode
            )
        }
    }
}

internal fun splitIntoChunks(text: String, maxLength: Int): List<String> =
    if (text.length <= maxLength) listOf(text) else text.chunked(maxLength)

private fun String.escapeHtml(): String = replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
