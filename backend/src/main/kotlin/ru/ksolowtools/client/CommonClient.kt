package ru.ksolowtools.client

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit.SECONDS

object CommonClient {
    private val client = OkHttpClient.Builder().apply {
        readTimeout(90, SECONDS)
        connectTimeout(90, SECONDS)
    }
        .build()

     fun sendRequest(requestBuilder: Request.Builder.() -> Unit): Response {
        val builder = Request.Builder().apply { requestBuilder() }
        return client.newCall(builder.build()).execute()
    }
}
