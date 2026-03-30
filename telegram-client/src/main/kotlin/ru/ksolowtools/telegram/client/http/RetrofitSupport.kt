package ru.ksolowtools.telegram.client.http

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit.MILLISECONDS

internal object RetrofitSupport {
    fun create(baseUrl: String): Retrofit {
        val client = OkHttpClient.Builder()
            .readTimeout(180000, MILLISECONDS)
            .connectTimeout(180000, MILLISECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(client)
            .addConverterFactory(
                JacksonConverterFactory.create(
                    jsonMapper()
                        .registerKotlinModule()
                        .apply {
                            disable(FAIL_ON_IGNORED_PROPERTIES)
                            disable(FAIL_ON_UNKNOWN_PROPERTIES)
                        }
                )
            )
            .build()
    }

    private fun normalizeBaseUrl(baseUrl: String): String = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
}
