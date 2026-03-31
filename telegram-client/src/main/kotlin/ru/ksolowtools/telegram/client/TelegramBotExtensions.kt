package ru.ksolowtools.telegram.client

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.TelegramFile.ByFile
import com.github.kotlintelegrambot.network.Response
import com.github.kotlintelegrambot.types.TelegramBotResult
import org.slf4j.Logger
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Response as RetrofitResponse
import java.io.File
import java.nio.file.Files

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

fun Bot.sendAudioFromUrl(
    chatId: ChatId,
    audioUrl: String,
    performer: String,
    title: String?,
    duration: Int?,
    action: String,
    log: Logger,
    replyToMessageId: Long? = null,
    allowSendingWithoutReply: Boolean = true
) {
    val audioFile = downloadAudioFile(audioUrl, log)
    if (audioFile == null) {
        sendMessage(
            chatId = chatId,
            text = "Suno сгенерировал трек, но mp3 скачать не удалось."
        ).also {
            it.logTelegramResult("$action (ошибка скачивания)", log)
        }
        return
    }

    try {
        sendAudio(
            chatId = chatId,
            audio = ByFile(audioFile),
            duration = duration,
            performer = performer,
            title = title,
            replyToMessageId = replyToMessageId,
            allowSendingWithoutReply = allowSendingWithoutReply
        ).also { call ->
            call.logTelegramCall("$action (отправка mp3)", log)
        }
    } finally {
        audioFile.delete()
    }
}

private fun downloadAudioFile(audioUrl: String, log: Logger): File? = kotlin.runCatching {
    val request = Request.Builder()
        .url(audioUrl)
        .get()
        .build()
    val tempFile = Files.createTempFile("suno-", ".mp3").toFile()

    OkHttpClient().newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            error("Не удалось скачать mp3 из Suno: http=${response.code}")
        }
        val body = response.body ?: error("Suno вернул пустое тело при скачивании mp3")
        tempFile.outputStream().use { output ->
            body.byteStream().use { input -> input.copyTo(output) }
        }
    }

    tempFile
}
    .onSuccess { log.info("MP3 из Suno скачан: {}", it.absolutePath) }
    .onFailure { log.warn("Ошибка при скачивании mp3 из Suno: {}", it.message, it) }
    .getOrNull()

private fun String.escapeHtml(): String = replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
