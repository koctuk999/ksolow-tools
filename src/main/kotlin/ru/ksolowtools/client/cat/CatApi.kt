package ru.ksolowtools.client.cat

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header

@JsonIgnoreProperties(ignoreUnknown = true)
class CatImageResponse {
    val url: String? = null
}

interface CatApi {

    @GET("v1/images/search")
    fun getRandomImage(@Header("x-api-key") apiKey: String): Call<List<CatImageResponse>>
}


