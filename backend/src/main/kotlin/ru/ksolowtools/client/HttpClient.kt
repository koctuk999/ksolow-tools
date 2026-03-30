package ru.ksolowtools.client

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import okhttp3.Interceptor
import okhttp3.OkHttpClient.Builder
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit.MILLISECONDS

abstract class HttpClient(
    private val baseUrl: String,
    private val readTimeout: Long = 180000,
    private val connectTimeout: Long = 180000,
    private val interceptors: List<Interceptor> = emptyList(),
    private val converterFactories: List<Converter.Factory> = emptyList(),
) {
    private val client = Builder()
        .apply {
            readTimeout(readTimeout, MILLISECONDS)
            connectTimeout(connectTimeout, MILLISECONDS)
            addInterceptor(
                HttpLoggingInterceptor()
                    .setLevel(BODY)
                    .apply {
                        redactHeader("Authorization")
                        redactHeader("x-api-key")
                    }
            )
            interceptors.forEach { addInterceptor(it) }
        }
        .build()

    val retrofit = Retrofit.Builder()
        .apply {
            client(client)
            baseUrl(baseUrl)
            addConverterFactory(JacksonConverterFactory.create(
                jsonMapper()
                    .registerKotlinModule()
                    .apply {
                        disable(FAIL_ON_IGNORED_PROPERTIES)
                        disable(FAIL_ON_UNKNOWN_PROPERTIES)
                    }
            ))
            converterFactories.forEach { addConverterFactory(it) }
        }
        .build()
}
