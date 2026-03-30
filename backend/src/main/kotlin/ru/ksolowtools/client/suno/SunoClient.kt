package ru.ksolowtools.client.suno

import ru.ksolowtools.client.HttpClient
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SunoClient(
    @Value("\${suno.api-key}")
    private val apiKey: String,
    @Value("\${suno.callback-url:https://example.com}")
    private val callbackUrl: String
) : HttpClient("https://api.sunoapi.org/") {

    private val log = getLogger(SunoClient::class.java)
    private val api = retrofit.create(SunoApi::class.java)

    fun generate(request: SunoGenerateRequest): SunoTaskCreateResult = kotlin.runCatching {
        val response = api.generate(
            authorization = "Bearer $apiKey",
            request = request.copy(callBackUrl = callbackUrl)
        ).execute()
        val body = response.body()
        log.info("Suno generate: http=${response.code()}, code=${body?.code}, msg=${body?.msg}, data=${body?.data}")
        if (!response.isSuccessful || body?.code != 200) {
            error("Suno generate failed: http=${response.code()}, msg=${body?.msg}")
        }
        body.data?.taskId ?: error(
            "Suno generate returned empty taskId; data=${body.data}"
        )
    }
        .fold(
            onSuccess = {
                log.info("Задача Suno создана: taskId={}", it)
                SunoTaskCreateResult.Success(it)
            },
            onFailure = {
                val reason = it.message ?: "Неизвестная ошибка при создании задачи Suno"
                log.warn("Ошибка при создании задачи Suno: {}", reason, it)
                SunoTaskCreateResult.Failure(reason)
            }
        )

    fun generationDetails(taskId: String): SunoGenerationDetails? = kotlin.runCatching {
        val response = api.generationDetails(
            authorization = "Bearer $apiKey",
            taskId = taskId
        ).execute()
        val body = response.body()
        log.info(
            "Suno generationDetails: taskId={}, http={}, code={}, msg={}, data={}",
            taskId,
            response.code(),
            body?.code,
            body?.msg,
            body?.data
        )
        if (!response.isSuccessful || body?.code != 200) {
            error("Suno generation details failed: http=${response.code()}, msg=${body?.msg}")
        }
        body.data ?: error("Suno generation details returned empty body")
    }
        .onSuccess { log.info("Статус задачи Suno {}: {}", taskId, it.status) }
        .onFailure { log.warn("Ошибка при получении статуса задачи Suno {}: {}", taskId, it.message, it) }
        .getOrNull()
}

sealed interface SunoTaskCreateResult {
    data class Success(val taskId: String) : SunoTaskCreateResult
    data class Failure(val reason: String) : SunoTaskCreateResult
}
